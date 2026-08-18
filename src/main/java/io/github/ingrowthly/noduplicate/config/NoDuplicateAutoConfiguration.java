package io.github.ingrowthly.noduplicate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ingrowthly.noduplicate.component.NoDuplicateAspect;
import io.github.ingrowthly.noduplicate.store.DuplicateRequestStore;
import io.github.ingrowthly.noduplicate.store.RedisDuplicateRequestStore;
import io.github.ingrowthly.noduplicate.util.RequestFingerprintGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for duplicate request protection.
 */
@AutoConfiguration(after = DataRedisAutoConfiguration.class)
@EnableConfigurationProperties(NoDuplicateProperties.class)
@ConditionalOnProperty(prefix = "no-duplicate", name = "enabled", matchIfMissing = true)
public class NoDuplicateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequestFingerprintGenerator noDuplicateFingerprintGenerator(
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new RequestFingerprintGenerator(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(DuplicateRequestStore.class)
    public DuplicateRequestStore redisDuplicateRequestStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisDuplicateRequestStore(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnBean(DuplicateRequestStore.class)
    @ConditionalOnMissingBean
    public NoDuplicateAspect noDuplicateAspect(DuplicateRequestStore store,
            RequestFingerprintGenerator fingerprintGenerator, NoDuplicateProperties properties,
            Environment environment) {
        String namespace = properties.getNamespace();
        if (!StringUtils.hasText(namespace)) {
            namespace = environment.getProperty("spring.application.name", "application");
        }
        return new NoDuplicateAspect(store, fingerprintGenerator, properties.getKeyPrefix(), namespace);
    }
}
