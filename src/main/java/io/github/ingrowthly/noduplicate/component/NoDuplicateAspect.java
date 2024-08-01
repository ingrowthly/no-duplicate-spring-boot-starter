package io.github.ingrowthly.noduplicate.component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import io.github.ingrowthly.noduplicate.exception.DuplicateSubmitException;
import io.github.ingrowthly.noduplicate.util.NoDuplicateUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 防止重复提交切面
 *
 * @author chuncheng
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class NoDuplicateAspect {

    private final StringRedisTemplate noRepeatSubmitRedisTemplate;

    /**
     * 防止重复提交 Redis 脚本
     */
    private final RedisScript<String> noRepeatSubmitRedisScript =
        new DefaultRedisScript<>("return redis.call('set', KEYS[1], 1, 'ex', ARGV[1], 'nx');", String.class);

    @Value("${spring.application.name}") private String namespace;

    @Pointcut("@annotation(io.github.ingrowthly.noduplicate.annotation.NoDuplicate)")
    public void doPointcut() {
    }

    @Before("doPointcut()&&@annotation(noDuplicate)")
    public void doBefore(JoinPoint pjp, NoDuplicate noDuplicate) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = attributes.getRequest();

        // 生成 Redis key
        String redisKey = getKey(pjp, noDuplicate, request);
        log.info("NoDuplicateSubmit redisKey: {}", redisKey);
        String result = noRepeatSubmitRedisTemplate.execute(noRepeatSubmitRedisScript, Lists.newArrayList(redisKey),
                String.valueOf(noDuplicate.ttl()));
        if (!"ok".equalsIgnoreCase(result)) {
            throw new DuplicateSubmitException(noDuplicate.message());
        }
    }

    @After("doPointcut()&&@annotation(noDuplicate)")
    public void doAfter(JoinPoint pjp, NoDuplicate noDuplicate) {
        if (noDuplicate.termination()) {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
            HttpServletRequest request = attributes.getRequest();
            String redisKey = getKey(pjp, noDuplicate, request);
            noRepeatSubmitRedisTemplate.delete(redisKey);
        }
    }


    private String getKey(JoinPoint pjp, NoDuplicate noDuplicate, HttpServletRequest request) {
        // 生成请求签名
        String sign;
        if (Strings.isNullOrEmpty(noDuplicate.key())) {
            sign = NoDuplicateUtils.getMethodSign(((MethodSignature) pjp.getSignature()).getMethod(), pjp.getArgs());
        } else {
            sign = NoDuplicateUtils.getSpelKey(pjp, noDuplicate);
        }

        // 生成 Redis key
        return NoDuplicateUtils.buildRedisKey(namespace, noDuplicate.uri() ? request.getServletPath() : null, sign);
    }
}
