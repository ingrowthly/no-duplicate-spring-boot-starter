package io.github.ingrowthly.noduplicate.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisDuplicateRequestStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisDuplicateRequestStore store;

    @BeforeEach
    void setUp() {
        store = new RedisDuplicateRequestStore(redisTemplate);
    }

    @Test
    void acquiresWithAtomicSetIfAbsentAndTtl() {
        Duration ttl = Duration.ofSeconds(3);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("key", "token", ttl)).thenReturn(true);

        assertTrue(store.acquire("key", "token", ttl));
        verify(valueOperations).setIfAbsent("key", "token", ttl);
    }

    @Test
    void treatsNullRedisResponseAsFailedAcquisition() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(null);

        assertFalse(store.acquire("key", "token", Duration.ofSeconds(3)));
    }

    @Test
    void releasesWithTokenCheckedScript() {
        store.release("key", "token");

        verify(redisTemplate).execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of("key")), eq("token"));
    }
}
