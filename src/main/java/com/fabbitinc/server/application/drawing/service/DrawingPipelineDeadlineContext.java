package com.fabbitinc.server.application.drawing.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DrawingPipelineDeadlineContext {

    private static final ThreadLocal<Instant> DEADLINE = new ThreadLocal<>();

    private DrawingPipelineDeadlineContext() {
    }

    public static void bind(Duration timeout) {
        DEADLINE.set(Instant.now().plus(timeout));
    }

    public static void clear() {
        DEADLINE.remove();
    }

    public static void check(String stage) {
        remainingMillis(stage);
    }

    public static <T> T call(String stage, CheckedSupplier<T> supplier) throws Exception {
        long remainingMillis = remainingMillis(stage);
        FutureTask<T> task = new FutureTask<>(supplier::get);
        Thread worker = new Thread(task, "drawing-pipeline-step");
        worker.setDaemon(true);
        worker.start();

        try {
            return task.get(remainingMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            worker.interrupt();
            throw new IllegalStateException("도면 변환 시간이 초과되었습니다: stage=" + stage, ex);
        } catch (InterruptedException ex) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("도면 변환 중 알 수 없는 오류가 발생했습니다: stage=" + stage, cause);
        }
    }

    private static long remainingMillis(String stage) {
        Instant deadline = DEADLINE.get();
        if (deadline == null) {
            return TimeUnit.MINUTES.toMillis(10);
        }

        long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
        if (remainingMillis <= 0) {
            throw new IllegalStateException("도면 변환 시간이 초과되었습니다: stage=" + stage);
        }
        return Math.max(1L, remainingMillis);
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
