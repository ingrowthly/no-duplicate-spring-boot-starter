package io.github.ingrowthly.noduplicate.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ingrowthly.noduplicate.annotation.NoDuplicate;
import io.github.ingrowthly.noduplicate.exception.DuplicateSubmitException;
import io.github.ingrowthly.noduplicate.store.DuplicateRequestStore;
import io.github.ingrowthly.noduplicate.util.RequestFingerprintGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoDuplicateAspectTest {

    private InMemoryStore store;
    private TestService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        NoDuplicateAspect aspect = new NoDuplicateAspect(store,
                new RequestFingerprintGenerator(new ObjectMapper()), "no-duplicate", "test-service");
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestService());
        factory.addAspect(aspect);
        service = factory.getProxy();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/orders");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rejectsAnIdenticalRequestAndIncludesRequestContextInKey() {
        assertEquals("order-1", service.submit("order-1"));

        DuplicateSubmitException exception =
                assertThrows(DuplicateSubmitException.class, () -> service.submit("order-1"));

        assertEquals("duplicate order", exception.getMessage());
        assertTrue(store.lastKey.startsWith("no-duplicate:test-service:/orders:"));
    }

    @Test
    void allowsDifferentArguments() {
        assertEquals("order-1", service.submit("order-1"));
        assertEquals("order-2", service.submit("order-2"));
    }

    @Test
    void expressionUsesOnlyTheSelectedArgument() {
        assertEquals("first", service.keyed("order-1", "first"));
        assertThrows(DuplicateSubmitException.class, () -> service.keyed("order-1", "second"));
    }

    @Test
    void terminationReleasesClaimAfterSuccessAndFailure() {
        assertEquals("order-1", service.terminate("order-1"));
        assertEquals("order-1", service.terminate("order-1"));

        assertThrows(IllegalStateException.class, () -> service.fail("order-2"));
        assertThrows(IllegalStateException.class, () -> service.fail("order-2"));
    }

    @Test
    void convertsConfiguredTimeUnitWithoutWaiting() {
        service.withMilliseconds("order-1");

        assertEquals(Duration.ofMillis(250), store.lastTtl);
    }

    @Test
    void rejectsInvalidTtlBeforeAccessingStore() {
        assertThrows(IllegalArgumentException.class, () -> service.invalidTtl("order-1"));
        assertEquals(0, store.acquireCount);
    }

    @Test
    void worksWithoutAWebRequestContext() {
        RequestContextHolder.resetRequestAttributes();

        assertEquals("order-1", service.submit("order-1"));
        assertTrue(store.lastKey.startsWith("no-duplicate:test-service:"));
    }

    @Test
    void canExcludeTheRequestUriFromTheKey() {
        assertEquals("order-1", service.withoutUri("order-1"));

        assertFalse(store.lastKey.contains("/orders"));
    }

    @Test
    void releaseFailureDoesNotReplaceTheBusinessOutcome() {
        store.failRelease = true;

        assertEquals("order-1", service.terminate("order-1"));
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> service.fail("order-2"));
        assertEquals("order-2", exception.getMessage());
    }

    static class TestService {

        @NoDuplicate(message = "duplicate order")
        public String submit(String id) {
            return id;
        }

        @NoDuplicate(key = "#id")
        public String keyed(String id, String value) {
            return value;
        }

        @NoDuplicate(termination = true)
        public String terminate(String id) {
            return id;
        }

        @NoDuplicate(termination = true)
        public String fail(String id) {
            throw new IllegalStateException(id);
        }

        @NoDuplicate(ttl = 250, timeUnit = TimeUnit.MILLISECONDS)
        public String withMilliseconds(String id) {
            return id;
        }

        @NoDuplicate(ttl = 0)
        public String invalidTtl(String id) {
            return id;
        }

        @NoDuplicate(uri = false)
        public String withoutUri(String id) {
            return id;
        }
    }

    static final class InMemoryStore implements DuplicateRequestStore {

        private final Map<String, String> claims = new ConcurrentHashMap<>();
        private String lastKey;
        private Duration lastTtl;
        private int acquireCount;
        private boolean failRelease;

        @Override
        public boolean acquire(String key, String token, Duration ttl) {
            lastKey = key;
            lastTtl = ttl;
            acquireCount++;
            return claims.putIfAbsent(key, token) == null;
        }

        @Override
        public void release(String key, String token) {
            if (failRelease) {
                throw new IllegalStateException("store unavailable");
            }
            claims.remove(key, token);
        }
    }
}
