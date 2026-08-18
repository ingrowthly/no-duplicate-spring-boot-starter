package io.github.ingrowthly.noduplicate.store;

import java.time.Duration;

/**
 * Atomic storage contract used to claim and release request fingerprints.
 *
 * @since 3.0.0
 */
public interface DuplicateRequestStore {

    /**
     * Atomically claims a key with an expiring ownership token.
     *
     * @return {@code true} when the claim was acquired
     */
    boolean acquire(String key, String token, Duration ttl);

    /**
     * Releases the key only when it is still owned by the supplied token.
     */
    void release(String key, String token);
}
