package com.fabbitinc.server.application.migration.service;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.migration.model.InventorManifestFile;
import com.fabbitinc.server.application.migration.model.InventorMigrationSession;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InventorMigrationSessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final Map<UUID, InventorMigrationSession> sessions = new ConcurrentHashMap<>();

    public InventorMigrationSession createSession(
            AuthContext auth,
            String projectName,
            String ipjPath,
            String inventorVersion,
            List<InventorManifestFile> files,
            Map<String, UUID> manifestPathToFileId
    ) {
        Instant now = Instant.now();
        InventorMigrationSession session = new InventorMigrationSession(
                UUID.randomUUID(),
                auth.orgId(),
                auth.userId(),
                projectName,
                ipjPath,
                inventorVersion,
                now,
                now.plus(SESSION_TTL),
                files,
                new LinkedHashMap<>(manifestPathToFileId)
        );
        sessions.put(session.sessionId(), session);
        return session;
    }

    public InventorMigrationSession getAccessibleSession(UUID sessionId, AuthContext auth) {
        InventorMigrationSession session = sessions.get(sessionId);
        if (session == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "마이그레이션 세션을 찾을 수 없습니다");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionId);
            throw new AppException(ErrorCode.PRECONDITION_FAILED, "만료된 마이그레이션 세션입니다");
        }
        if (!session.orgId().equals(auth.orgId()) || !session.userId().equals(auth.userId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 마이그레이션 세션에 접근할 수 없습니다");
        }
        return session;
    }

    public void removeSession(UUID sessionId) {
        sessions.remove(sessionId);
    }
}
