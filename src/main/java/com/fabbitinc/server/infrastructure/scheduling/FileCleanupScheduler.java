package com.fabbitinc.server.infrastructure.scheduling;

import com.fabbitinc.server.application.file.service.FileCleanupService;
import com.fabbitinc.server.application.file.service.input.CleanupOrphanObjectsInput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private static final Duration STALE_MAX_AGE = Duration.ofHours(24);
    private static final Duration DELETED_RETENTION = Duration.ofDays(7);
    private static final int BATCH_SIZE = 100;
    private static final int ORPHAN_MAX_LIST_PAGES = 20;
    private static final int ORPHAN_MAX_DELETE_COUNT = 500;
    private static final Duration ORPHAN_PAGE_PAUSE = Duration.ofMillis(200);

    private final OrganizationRepository organizationRepository;
    private final FileCleanupService fileCleanupService;
    private final ScheduledJobLockSupport scheduledJobLockSupport;

    /**
     * 매일 새벽 3시에 실행된다.
     * <p>
     * 모든 조직 테넌트를 순회하면서 생성 후 24시간이 지난 {@code PENDING} 파일을 찾아
     * 스토리지 객체를 삭제하고 DB 레코드를 물리 삭제한다.
     * 같은 시각에 여러 인스턴스가 떠 있어도 advisory lock을 획득한 한 인스턴스만 수행한다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupStalePendingFiles() {
        runAcrossTenants(
                "file_cleanup_stale_pending",
                "stale_pending",
                org -> fileCleanupService.cleanupStalePendingFiles(
                        STALE_MAX_AGE,
                        BATCH_SIZE
                )
        );
    }

    /**
     * 매일 새벽 3시 30분에 실행된다.
     * <p>
     * 모든 조직 테넌트를 순회하면서 soft delete 후 7일이 지난 파일을 찾아
     * 스토리지 객체를 삭제하고 DB 레코드를 물리 삭제한다.
     * 같은 시각에 여러 인스턴스가 떠 있어도 advisory lock을 획득한 한 인스턴스만 수행한다.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void cleanupExpiredDeletedFiles() {
        runAcrossTenants(
                "file_cleanup_expired_deleted",
                "expired_deleted",
                org -> fileCleanupService.cleanupExpiredDeletedFiles(
                        DELETED_RETENTION,
                        BATCH_SIZE
                )
        );
    }

    /**
     * 매일 새벽 4시에 실행된다.
     * <p>
     * 모든 조직 테넌트를 순회하면서 현재 조직 prefix 아래의 스토리지 객체를 페이지 단위로 조회하고,
     * 현재 테넌트 DB의 {@code file_key}와 비교해 어떤 파일 레코드에도 연결되지 않은 orphan 객체만 삭제한다.
     * 한 번의 실행에서는 페이지 조회 수와 삭제 수를 상한으로 제한하고, 페이지 사이에 짧은 대기 시간을 둬
     * 스토리지 list/delete 호출이 짧은 시간에 과도하게 몰리지 않도록 한다.
     * 같은 시각에 여러 인스턴스가 떠 있어도 advisory lock을 획득한 한 인스턴스만 수행한다.
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupOrphanStorageObjects() {
        runAcrossTenants(
                "file_cleanup_orphan_objects",
                "orphan_objects",
                org -> fileCleanupService.cleanupOrphanObjects(new CleanupOrphanObjectsInput(
                        org.getId(),
                        BATCH_SIZE,
                        ORPHAN_MAX_LIST_PAGES,
                        ORPHAN_MAX_DELETE_COUNT,
                        ORPHAN_PAGE_PAUSE
                )).deletedCount()
        );
    }

    private void runAcrossTenants(String lockName, String jobType, TenantCleanupJob cleanupJob) {
        scheduledJobLockSupport.executeWithLock(lockName, () -> {
            TenantContextHolder.clear();

            int tenantCount = 0;
            int deletedCount = 0;
            int failedTenantCount = 0;

            for (Organization organization : organizationRepository.findAll()) {
                tenantCount++;
                String schemaName = TenantSchemaPolicy.schemaNameForOrgId(organization.getId());
                TenantContextHolder.setCurrentSchema(schemaName);

                try {
                    int tenantDeletedCount = cleanupJob.cleanup(organization);
                    deletedCount += tenantDeletedCount;

                    if (tenantDeletedCount > 0) {
                        log.info(
                                "event=file_cleanup_tenant_completed job={} org_id={} schema={} deleted_count={} outcome=success",
                                jobType,
                                organization.getId(),
                                schemaName,
                                tenantDeletedCount
                        );
                    }
                } catch (Exception ex) {
                    failedTenantCount++;
                    log.error(
                            "event=file_cleanup_tenant_failed job={} org_id={} schema={} reason={}",
                            jobType,
                            organization.getId(),
                            schemaName,
                            ex.getMessage(),
                            ex
                    );
                } finally {
                    TenantContextHolder.clear();
                }
            }

            log.info(
                    "event=file_cleanup_job_completed job={} tenant_count={} deleted_count={} failed_tenant_count={} outcome=success",
                    jobType,
                    tenantCount,
                    deletedCount,
                    failedTenantCount
            );
        });
    }

    @FunctionalInterface
    private interface TenantCleanupJob {
        int cleanup(Organization organization);
    }
}
