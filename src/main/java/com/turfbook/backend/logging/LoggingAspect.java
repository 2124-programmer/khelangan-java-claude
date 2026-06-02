package com.turfbook.backend.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting trace logger for controllers and services.
 * Logs method entry (DEBUG), exit (DEBUG + durationMs), and exceptions (ERROR).
 *
 * Arg values are NEVER logged — only the count — to avoid leaking PII.
 * Change the pointcuts' log level in application.yml:
 *   com.turfbook.backend.logging: DEBUG   → enables per-method traces
 *   com.turfbook.backend.logging: INFO    → silences them in prod
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.turfbook.backend.controller..*)")
    private void controllerLayer() {}

    @Pointcut("within(com.turfbook.backend.service.impl..*)")
    private void serviceLayer() {}

    @Around("controllerLayer() || serviceLayer()")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
        String className  = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        int    argCount   = pjp.getArgs().length;
        long   start      = System.currentTimeMillis();

        log.debug("event=METHOD_ENTER class={} method={} argCount={}", className, methodName, argCount);

        try {
            Object result = pjp.proceed();
            long durationMs = System.currentTimeMillis() - start;
            log.debug("event=METHOD_EXIT class={} method={} status=SUCCESS durationMs={}",
                    className, methodName, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("event=METHOD_EXIT class={} method={} status=ERROR durationMs={} error={}",
                    className, methodName, durationMs, ex.getMessage());
            throw ex;
        }
    }
}
