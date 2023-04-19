package io.github.ingrowthly.noduplicate.component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import io.github.ingrowthly.noduplicate.exception.DuplicateSubmitException;
import io.github.ingrowthly.noduplicate.util.NoDuplicateUtils;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 防止重复提交切面
 *
 * @author ingrowthly
 * @since 2023/4/3
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

    @Around("doPointcut()&&@annotation(noDuplicate)")
    public Object doAround(ProceedingJoinPoint pjp, NoDuplicate noDuplicate) throws Throwable {

        ServletRequestAttributes attributes =
            (ServletRequestAttributes)Objects.requireNonNull(RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = attributes.getRequest();

        // 生成请求签名
        String sign;
        if (Strings.isNullOrEmpty(noDuplicate.key())) {
            sign = NoDuplicateUtils.getMethodSign(((MethodSignature)pjp.getSignature()).getMethod(), pjp.getArgs());
        } else {
            sign = NoDuplicateUtils.getSpelKey(pjp, noDuplicate);
        }

        // 生成 Redis key
        String redisKey =
            NoDuplicateUtils.buildRedisKey(namespace, noDuplicate.uri() ? request.getServletPath() : null, sign);
        log.info("NoDuplicateSubmit redisKey: {}", redisKey);

        String result = noRepeatSubmitRedisTemplate.execute(noRepeatSubmitRedisScript, Lists.newArrayList(redisKey),
            String.valueOf(noDuplicate.ttl()));

        if ("ok".equalsIgnoreCase(result)) {
            return pjp.proceed();
        }
        throw new DuplicateSubmitException(noDuplicate.message());
    }
}
