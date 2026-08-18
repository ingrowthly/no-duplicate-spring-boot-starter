package io.github.ingrowthly.noduplicate.config;

import io.github.ingrowthly.noduplicate.component.NoDuplicateAspect;
import io.github.ingrowthly.noduplicate.store.DuplicateRequestStore;
import io.github.ingrowthly.noduplicate.store.RedisDuplicateRequestStore;
import io.github.ingrowthly.noduplicate.util.RequestFingerprintGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NoDuplicateAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NoDuplicateAutoConfiguration.class));

    @Test
    void configuresAspectWithAUserProvidedStore() {
        contextRunner.withBean(DuplicateRequestStore.class, StubStore::new)
                .withPropertyValues("spring.application.name=orders", "no-duplicate.key-prefix=guard")
                .run(context -> {
                    assertThat(context).hasSingleBean(RequestFingerprintGenerator.class);
                    assertThat(context).hasSingleBean(NoDuplicateAspect.class);
                    assertThat(context.getBean(NoDuplicateProperties.class).getKeyPrefix()).isEqualTo("guard");
                });
    }

    @Test
    void configuresRedisStoreWhenRedisTemplateIsAvailable() {
        contextRunner.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DuplicateRequestStore.class);
                    assertThat(context.getBean(DuplicateRequestStore.class))
                            .isInstanceOf(RedisDuplicateRequestStore.class);
                    assertThat(context).hasSingleBean(NoDuplicateAspect.class);
                });
    }

    @Test
    void canDisableAutoConfiguration() {
        contextRunner.withBean(DuplicateRequestStore.class, StubStore::new)
                .withPropertyValues("no-duplicate.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RequestFingerprintGenerator.class);
                    assertThat(context).doesNotHaveBean(NoDuplicateAspect.class);
                });
    }

    static final class StubStore implements DuplicateRequestStore {

        @Override
        public boolean acquire(String key, String token, Duration ttl) {
            return true;
        }

        @Override
        public void release(String key, String token) {
        }
    }
}
