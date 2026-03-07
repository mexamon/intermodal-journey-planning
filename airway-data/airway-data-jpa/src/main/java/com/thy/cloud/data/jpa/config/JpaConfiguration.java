package com.thy.cloud.data.jpa.config;

import com.thy.cloud.data.jpa.entity.auditor.AppAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 *
 * <h2>JpaConfiguration</h2>
 *
 * @author Engin Mahmut
 * Audit Aware Bean Configuration Implementation {@link AuditorAware}
 *
 */
@Configuration
@EnableJpaAuditing
public class JpaConfiguration {

        @Bean
        public AuditorAware<String> auditorProvider() {
                return new AppAuditorAware();
        }
}
