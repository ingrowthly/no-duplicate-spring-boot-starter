package io.github.ingrowthly.noduplicate.config;

import io.github.ingrowthly.noduplicate.component.NoDuplicateAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 防重复提交自动配置类
 *
 * @since 2023/4/3
 */
@Configuration
public class NoDuplicateAutoConfiguration {

    @Bean
    public NoDuplicateAspect noDuplicateSubmitAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoDuplicateAspect(stringRedisTemplate);
    }
}
