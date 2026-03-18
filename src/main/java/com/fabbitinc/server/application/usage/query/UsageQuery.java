package com.fabbitinc.server.application.usage.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.usage.model.StorageCategory;
import com.fabbitinc.server.application.usage.model.StorageTrendPeriod;
import com.fabbitinc.server.application.usage.query.condition.StorageTrendCondition;
import com.fabbitinc.server.application.usage.query.result.CreditUsageResult;
import com.fabbitinc.server.application.usage.query.result.StorageTrendResult;
import com.fabbitinc.server.application.usage.query.result.StorageUsageResult;
import com.fabbitinc.server.domain.aiusage.repository.AiUsageEventRepository;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.file.model.FileStatus;
import com.fabbitinc.server.domain.file.repository.FileRepository;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionUsagePolicy;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatAssignmentRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionUsagePolicyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsageQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationRepository organizationRepository;
    private final FileRepository fileRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSeatAssignmentRepository subscriptionSeatAssignmentRepository;
    private final SubscriptionUsagePolicyRepository subscriptionUsagePolicyRepository;
    private final AiUsageEventRepository aiUsageEventRepository;

    public StorageUsageResult getStorageUsage() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Organization organization = organizationRepository.findById(auth.orgId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));
        Subscription subscription = getActiveSubscription(auth.orgId());
        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(subscription.getId());
        long fullSeatCount = subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(auth.orgId(), SeatType.FULL);

        List<File> files = fileRepository.findByStatusAndOwnerTypeIsNotNull(FileStatus.UPLOADED);
        Map<StorageCategory, CategorySummary> summaries = new EnumMap<>(StorageCategory.class);

        for (File file : files) {
            if (file.getDeletedAt() != null) {
                continue;
            }
            StorageCategory category = StorageCategory.fromOwnerType(file.getOwnerType());
            CategorySummary summary = summaries.computeIfAbsent(category, ignored -> new CategorySummary());
            summary.bytesUsed += file.getFileSize();
            summary.fileCount += 1;
        }

        List<StorageUsageResult.StorageCategoryItemResult> categories = new ArrayList<>();
        for (StorageCategory category : List.of(StorageCategory.DRAWING, StorageCategory.ATTACHMENT, StorageCategory.OTHER)) {
            CategorySummary summary = summaries.get(category);
            if (summary == null || summary.fileCount == 0) {
                continue;
            }
            categories.add(new StorageUsageResult.StorageCategoryItemResult(
                    category,
                    summary.bytesUsed,
                    summary.fileCount
            ));
        }

        long bytesLimit = usagePolicy.calculateIncludedStorageBytes(fullSeatCount);
        long overage = Math.max(organization.getStorageBytesUsed() - bytesLimit, 0L);
        return new StorageUsageResult(
                organization.getStorageBytesUsed(),
                bytesLimit,
                overage,
                subscription.getPlanType().allowsStorageOverage(),
                categories
        );
    }

    public StorageTrendResult getStorageTrend(StorageTrendCondition condition) {
        currentAuthProvider.getCurrentAuth();
        StorageTrendPeriod period = StorageTrendPeriod.from(condition.period());

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<TrendPoint> points = buildPoints(period, today);
        if (points.isEmpty()) {
            return new StorageTrendResult(List.of());
        }

        LocalDate startDate = points.getFirst().snapshotDate();
        LocalDate endDate = points.getLast().snapshotDate();
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);

        List<File> files = fileRepository.findByStatusAndOwnerTypeIsNotNull(FileStatus.UPLOADED);
        CategoryTotals baseline = buildBaseline(files, startInstant);
        Map<LocalDate, CategoryTotals> dailyDeltas = buildDailyDeltas(files, startDate, endDate);

        Map<LocalDate, CategoryTotals> snapshots = new HashMap<>();
        CategoryTotals running = baseline.copy();
        LocalDate day = startDate;
        while (!day.isAfter(endDate)) {
            CategoryTotals delta = dailyDeltas.get(day);
            if (delta != null) {
                running.add(delta);
            }
            snapshots.put(day, running.copy());
            day = day.plusDays(1);
        }

        List<StorageTrendResult.StorageTrendItemResult> items = new ArrayList<>(points.size());
        for (TrendPoint point : points) {
            CategoryTotals totals = snapshots.getOrDefault(point.snapshotDate(), CategoryTotals.empty());
            items.add(new StorageTrendResult.StorageTrendItemResult(
                    point.label(),
                    Math.max(totals.drawing, 0L),
                    Math.max(totals.attachment, 0L),
                    Math.max(totals.other, 0L)
            ));
        }
        return new StorageTrendResult(items);
    }

    public CreditUsageResult getCreditUsage() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Subscription subscription = getActiveSubscription(auth.orgId());
        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(subscription.getId());

        BigDecimal totalUsedRaw = aiUsageEventRepository.sumCreditsUsed(auth.orgId(), subscription.getCurrentPeriodStart());
        int totalUsed = ceil(totalUsedRaw);

        List<CreditUsageResult.CreditCategoryItemResult> categories = aiUsageEventRepository.aggregateCreditsByCategory(
                        auth.orgId(),
                        subscription.getCurrentPeriodStart()
                ).stream()
                .map(row -> new CreditUsageResult.CreditCategoryItemResult(
                        (String) row[0],
                        ceil((BigDecimal) row[1]),
                        ((Number) row[2]).longValue()
                ))
                .toList();

        int includedLimit = ceil(usagePolicy.getStarterMonthlyAiCredits());
        int includedUsed = Math.min(totalUsed, includedLimit);
        int includedRemaining = Math.max(includedLimit - includedUsed, 0);
        Integer meteredLimit = usagePolicy.getAiMonthlyCreditLimit() == null ? null : ceil(usagePolicy.getAiMonthlyCreditLimit());

        return new CreditUsageResult(
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                totalUsed,
                includedLimit,
                includedUsed,
                includedRemaining,
                meteredLimit,
                usagePolicy.isAiHardLimitEnabled(),
                categories
        );
    }

    private Subscription getActiveSubscription(java.util.UUID orgId) {
        return subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "활성 구독 정보를 찾을 수 없습니다"));
    }

    private SubscriptionUsagePolicy getUsagePolicy(java.util.UUID subscriptionId) {
        return subscriptionUsagePolicyRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));
    }

    private int ceil(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.CEILING).intValue();
    }

    private List<TrendPoint> buildPoints(StorageTrendPeriod period, LocalDate today) {
        if (period == StorageTrendPeriod.DAYS_7 || period == StorageTrendPeriod.DAYS_30) {
            int days = period == StorageTrendPeriod.DAYS_7 ? 7 : 30;
            LocalDate startDate = today.minusDays(days - 1L);
            List<TrendPoint> points = new ArrayList<>(days);
            for (int offset = 0; offset < days; offset++) {
                LocalDate target = startDate.plusDays(offset);
                points.add(new TrendPoint(target.toString(), target));
            }
            return points;
        }

        LocalDate thisMonth = today.withDayOfMonth(1);
        LocalDate firstMonth = thisMonth.minusMonths(11);
        List<TrendPoint> points = new ArrayList<>(12);
        for (int offset = 0; offset < 12; offset++) {
            LocalDate monthStart = firstMonth.plusMonths(offset);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            LocalDate snapshotDate = monthEnd.isAfter(today) ? today : monthEnd;
            String label = String.format("%04d-%02d", monthStart.getYear(), monthStart.getMonthValue());
            points.add(new TrendPoint(label, snapshotDate));
        }
        return points;
    }

    private CategoryTotals buildBaseline(List<File> files, Instant startInstant) {
        CategoryTotals baseline = CategoryTotals.empty();
        for (File file : files) {
            if (file.getCreatedAt().isAfter(startInstant) || file.getCreatedAt().equals(startInstant)) {
                continue;
            }
            if (file.getDeletedAt() != null && file.getDeletedAt().isBefore(startInstant)) {
                continue;
            }
            baseline.add(StorageCategory.fromOwnerType(file.getOwnerType()), file.getFileSize());
        }
        return baseline;
    }

    private Map<LocalDate, CategoryTotals> buildDailyDeltas(
            List<File> files,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, CategoryTotals> dailyDeltas = new HashMap<>();

        for (File file : files) {
            StorageCategory category = StorageCategory.fromOwnerType(file.getOwnerType());

            LocalDate createdDate = file.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (!createdDate.isBefore(startDate) && !createdDate.isAfter(endDate)) {
                CategoryTotals delta = dailyDeltas.computeIfAbsent(createdDate, ignored -> CategoryTotals.empty());
                delta.add(category, file.getFileSize());
            }

            if (file.getDeletedAt() == null) {
                continue;
            }
            LocalDate deletedDate = file.getDeletedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (deletedDate.isBefore(startDate) || deletedDate.isAfter(endDate)) {
                continue;
            }
            CategoryTotals delta = dailyDeltas.computeIfAbsent(deletedDate, ignored -> CategoryTotals.empty());
            delta.add(category, -file.getFileSize());
        }

        return dailyDeltas;
    }

    private record TrendPoint(String label, LocalDate snapshotDate) {
    }

    private static final class CategorySummary {
        long bytesUsed;
        int fileCount;
    }

    private static final class CategoryTotals {
        private long drawing;
        private long attachment;
        private long other;

        static CategoryTotals empty() {
            return new CategoryTotals();
        }

        void add(StorageCategory category, long delta) {
            switch (category) {
                case DRAWING -> drawing += delta;
                case ATTACHMENT -> attachment += delta;
                case OTHER -> other += delta;
            }
        }

        void add(CategoryTotals delta) {
            drawing += delta.drawing;
            attachment += delta.attachment;
            other += delta.other;
        }

        CategoryTotals copy() {
            CategoryTotals copy = new CategoryTotals();
            copy.drawing = drawing;
            copy.attachment = attachment;
            copy.other = other;
            return copy;
        }
    }
}
