package com.fabbitinc.server.infrastructure.scheduling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledJobLockSupport {

    private static final String TRY_LOCK_SQL = "select pg_try_advisory_lock(hashtext(?), 0)";
    private static final String UNLOCK_SQL = "select pg_advisory_unlock(hashtext(?), 0)";

    private final DataSource dataSource;

    public boolean executeWithLock(String jobName, Runnable task) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryAcquire(connection, jobName)) {
                log.warn("event=scheduled_job_skipped job={} reason=lock_not_acquired", jobName);
                return false;
            }

            try {
                task.run();
                return true;
            } finally {
                release(connection, jobName);
            }
        } catch (SQLException ex) {
            log.warn("event=scheduled_job_lock_fallback job={} reason={}", jobName, ex.getMessage());
            task.run();
            return true;
        }
    }

    private boolean tryAcquire(Connection connection, String jobName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK_SQL)) {
            statement.setString(1, jobName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        }
    }

    private void release(Connection connection, String jobName) {
        try (PreparedStatement statement = connection.prepareStatement(UNLOCK_SQL)) {
            statement.setString(1, jobName);
            statement.execute();
        } catch (SQLException ex) {
            log.warn("event=scheduled_job_unlock_failed job={} reason={}", jobName, ex.getMessage());
        }
    }
}
