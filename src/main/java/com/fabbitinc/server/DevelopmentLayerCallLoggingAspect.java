package com.fabbitinc.server;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
 @Aspect
 @Component
 @ConditionalOnProperty(
         prefix = "app.dev-layer-call-log",
         name = "enabled",
         havingValue = "true"
 )
public class DevelopmentLayerCallLoggingAspect {

    @Pointcut("execution(public * com.fabbitinc.server.presentation..*Controller.*(..))")
    public void controllerLayer() {
    }

    @Pointcut("execution(public * com.fabbitinc.server.application..*UseCase.*(..))")
    public void useCaseLayer() {
    }

    @Pointcut("execution(public * com.fabbitinc.server.application..*Query.*(..))")
    public void queryLayer() {
    }

    @Pointcut("execution(public * com.fabbitinc.server.application..*Service.*(..))")
    public void serviceLayer() {
    }

    @Around("controllerLayer() || useCaseLayer() || queryLayer() || serviceLayer()")
    public Object logCallAndReturn(ProceedingJoinPoint joinPoint) throws Throwable {
        String targetName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String argSummary = Arrays.stream(joinPoint.getArgs())
                .map(this::toTypeName)
                .collect(Collectors.joining(", "));

        long startNs = System.nanoTime();
        log.info("call {}.{}(args=[{}])", targetName, methodName, argSummary);

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info(
                    "return {}.{} => {} ({}ms)",
                    targetName,
                    methodName,
                    toResultName(result),
                    elapsedMs
            );
            return result;
        } catch (Throwable throwable) {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.warn(
                    "throw {}.{} => {}({}) ({}ms)",
                    targetName,
                    methodName,
                    throwable.getClass().getSimpleName(),
                    safeMessage(throwable.getMessage()),
                    elapsedMs
            );
            throw throwable;
        }
    }

    private String toTypeName(Object arg) {
        if (arg == null) {
            return "null";
        }
        return arg.getClass().getSimpleName();
    }

    private String toResultName(Object result) {
        if (result == null) {
            return "void";
        }
        return result.getClass().getSimpleName();
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.length() <= 120 ? message : message.substring(0, 120) + "...";
    }
}
