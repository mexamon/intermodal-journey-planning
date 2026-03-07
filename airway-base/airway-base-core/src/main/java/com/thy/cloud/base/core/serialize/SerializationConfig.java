package com.thy.cloud.base.core.serialize;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.util.TimeZone;

/**
 * Serialization auto-configuration — customizes ObjectMapper with UTC timezone,
 * ISO-8601 dates, and safe defaults using Spring Boot 4.x builder pattern.
 * <p>
 * Jackson 3.x defaults to ISO-8601 date format (no WRITE_DATES_AS_TIMESTAMPS
 * needed).
 *
 * @author Engin Mahmut
 */
@AutoConfiguration(before = JacksonAutoConfiguration.class)
@ConditionalOnClass(ObjectMapper.class)
public class SerializationConfig {

    /**
     * Customizer that applies project-wide ObjectMapper defaults:
     * <ul>
     * <li>UTC timezone</li>
     * <li>Tolerant deserialization (unknown props ignored)</li>
     * <li>Lazy-loaded JPA entities safe (FAIL_ON_EMPTY_BEANS disabled)</li>
     * <li>Unescaped control chars / single quotes allowed</li>
     * <li>Custom Java 8 time serializers via {@link AirwayJacksonModule}</li>
     * </ul>
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public JsonMapperBuilderCustomizer airwayJsonMapperCustomizer() {
        return (JsonMapper.Builder builder) -> {
            // ── Timezone: UTC worldwide standard ──────────────────
            builder.defaultTimeZone(TimeZone.getTimeZone("UTC"));

            // ── Deserialization tolerance ─────────────────────────
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            // ── JPA lazy-load safety ─────────────────────────────
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

            // ── Escape / encoding tolerance ──────────────────────
            builder.enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);
            builder.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS);
            builder.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES);

            // ── Custom time module ───────────────────────────────
            builder.addModule(new AirwayJacksonModule());
        };
    }

    /**
     * Initializes the static {@link JsonUtils} with Jackson adapter.
     */
    @Bean
    @ConditionalOnMissingBean(JsonTool.class)
    public JsonTool jsonTool(ObjectMapper objectMapper) {
        JacksonJsonToolAdapter adapter = new JacksonJsonToolAdapter(objectMapper);
        JsonUtils.init(adapter);
        return adapter;
    }
}
