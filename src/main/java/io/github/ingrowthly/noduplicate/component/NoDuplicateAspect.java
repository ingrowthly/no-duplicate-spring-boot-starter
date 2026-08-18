package io.github.ingrowthly.noduplicate.component;

import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import io.github.ingrowthly.noduplicate.exception.DuplicateSubmitException;
import io.github.ingrowthly.noduplicate.store.DuplicateRequestStore;
import io.github.ingrowthly.noduplicate.util.RequestFingerprintGenerator;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Prevents duplicate method invocations backed by a {@link DuplicateRequestStore}.
 */
@Aspect
public final class NoDuplicateAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoDuplicateAspect.class);

    private final DuplicateRequestStore store;
    private final RequestFingerprintGenerator fingerprintGenerator;
    private final String keyPrefix;
    private final String namespace;

    public NoDuplicateAspect(DuplicateRequestStore store, RequestFingerprintGenerator fingerprintGenerator,
            String keyPrefix, String namespace) {
        this.store = store;
        this.fingerprintGenerator = fingerprintGenerator;
        this.keyPrefix = keyPrefix;
        this.namespace = namespace;
    }

    @Around("@annotation(noDuplicate)")
    public Object execute(ProceedingJoinPoint joinPoint, NoDuplicate noDuplicate) throws Throwable {
        Duration ttl = toDuration(noDuplicate);
        Method method = resolveMethod(joinPoint);
        String fingerprint = fingerprintGenerator.generate(method, joinPoint.getArgs(), noDuplicate.key());
        String redisKey = buildKey(currentRequestUri(noDuplicate), fingerprint);
        String token = UUID.randomUUID().toString();

        if (!store.acquire(redisKey, token, ttl)) {
            throw new DuplicateSubmitException(noDuplicate.message());
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (noDuplicate.termination()) {
                releaseQuietly(redisKey, token);
            }
        }
    }

    private Duration toDuration(NoDuplicate noDuplicate) {
        if (noDuplicate.ttl() <= 0) {
            throw new IllegalArgumentException("@NoDuplicate ttl must be greater than zero");
        }
        return Duration.of(noDuplicate.ttl(), noDuplicate.timeUnit().toChronoUnit());
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return AopUtils.getMostSpecificMethod(method, joinPoint.getTarget().getClass());
    }

    private String currentRequestUri(NoDuplicate noDuplicate) {
        if (!noDuplicate.uri()) {
            return null;
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRequestURI();
        }
        return null;
    }

    private String buildKey(String uri, String fingerprint) {
        StringJoiner key = new StringJoiner(":");
        addIfPresent(key, keyPrefix);
        addIfPresent(key, namespace);
        addIfPresent(key, uri);
        key.add(fingerprint);
        return key.toString();
    }

    private void addIfPresent(StringJoiner key, String part) {
        if (StringUtils.hasText(part)) {
            key.add(part);
        }
    }

    private void releaseQuietly(String key, String token) {
        try {
            store.release(key, token);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to release no-duplicate key: {}", key, ex);
        }
    }
}
