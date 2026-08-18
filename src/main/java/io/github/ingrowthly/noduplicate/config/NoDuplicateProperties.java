package io.github.ingrowthly.noduplicate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for duplicate request protection.
 */
@ConfigurationProperties("no-duplicate")
public class NoDuplicateProperties {

    /** Whether duplicate request protection is enabled. */
    private boolean enabled = true;

    /** Prefix applied to every duplicate request store key. */
    private String keyPrefix = "no-duplicate";

    /** Key namespace; defaults to the spring.application.name property. */
    private String namespace;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
