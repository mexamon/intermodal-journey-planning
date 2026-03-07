package com.thy.cloud.base.core.serialize;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom Jackson module for Java 8 time types with ISO-8601 UTC patterns.
 * <p>
 * Patterns:
 * <ul>
 * <li>{@code LocalDateTime} → {@code yyyy-MM-dd'T'HH:mm:ss}</li>
 * <li>{@code LocalDate} → {@code yyyy-MM-dd}</li>
 * <li>{@code LocalTime} → {@code HH:mm:ss}</li>
 * </ul>
 *
 * @author Engin Mahmut
 */
public class AirwayJacksonModule extends SimpleModule {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    public AirwayJacksonModule() {
        super("AirwayJacksonModule");

        // Serializers
        this.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        // Deserializers
        this.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));
    }
}
