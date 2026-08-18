package io.github.ingrowthly.noduplicate.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis implementation using an expiring SET NX claim and token-safe release.
 */
public final class RedisDuplicateRequestStore implements DuplicateRequestStore {

    private static final RedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisDuplicateRequestStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquire(String key, String token, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, ttl));
    }

    @Override
    public void release(String key, String token) {
        redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
    }
}
