package com.thy.cloud.data.cache.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP aspect that intercepts methods annotated with {@link DistributedLock}
 * and wraps them with Redis-backed distributed lock acquisition/release.
 * <p>
 * The lock key is built from the annotation's {@code prefix} and {@code key}
 * attributes. The {@code key} attribute supports SpEL expressions referencing
 * method parameters (e.g. {@code #orderId}, {@code #request.id}).
 * <p>
 * Example:
 * <pre>
 * &#064;DistributedLock(prefix = "payment", key = "#paymentId", leaseTime = 60)
 * public void processPayment(String paymentId) { ... }
 * </pre>
 *
 * @author Engin Mahmut
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedisDistributedLockService lockService;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockKey = buildLockKey(joinPoint, distributedLock);

        log.debug("@DistributedLock: acquiring [{}] waitTime={}{}, leaseTime={}{}",
                lockKey, distributedLock.waitTime(), distributedLock.timeUnit(),
                distributedLock.leaseTime(), distributedLock.timeUnit());

        RedisDistributedLockService.LockToken token = lockService.tryLock(
                lockKey,
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
        );

        if (token == null) {
            log.warn("@DistributedLock: acquisition failed for [{}]", lockKey);
            throw new LockAcquisitionException(lockKey, distributedLock.failMessage());
        }

        try {
            log.debug("@DistributedLock: acquired [{}], proceeding", lockKey);
            return joinPoint.proceed();
        } finally {
            lockService.unlock(token);
            log.debug("@DistributedLock: released [{}]", lockKey);
        }
    }

    /**
     * Build the full lock key from annotation attributes.
     * Supports SpEL expressions in the {@code key} attribute.
     *
     * @return formatted key like "prefix:evaluated-key" or method FQDN as fallback
     */
    private String buildLockKey(ProceedingJoinPoint joinPoint, DistributedLock annotation) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Evaluate SpEL key expression
        String keyPart;
        if (annotation.key().isEmpty()) {
            // Default: fully-qualified method name
            keyPart = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        } else if (annotation.key().startsWith("#")) {
            // SpEL expression — evaluate against method parameters
            EvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), NAME_DISCOVERER);
            Object value = PARSER.parseExpression(annotation.key()).getValue(context);
            keyPart = value != null ? value.toString() : "null";
        } else {
            // Plain string key
            keyPart = annotation.key();
        }

        // Build final key
        if (annotation.prefix().isEmpty()) {
            return keyPart;
        }
        return annotation.prefix() + ":" + keyPart;
    }
}
