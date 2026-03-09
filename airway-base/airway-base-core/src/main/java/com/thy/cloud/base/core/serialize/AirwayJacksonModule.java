package com.thy.cloud.base.core.serialize;

import com.thy.cloud.base.core.enums.IEnum;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
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
 * Custom Jackson module for Java 8 time types with ISO-8601 UTC patterns
 * and global {@link IEnum} serialization as {@code {code, value, desc}} objects.
 *
 * @author Engin Mahmut
 */
public class AirwayJacksonModule extends SimpleModule {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public AirwayJacksonModule() {
        super("AirwayJacksonModule");

        // ── Date/Time Serializers ──────────────────────────────
        this.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addSerializer(LocalTime.class,
                new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        this.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
        this.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        this.addDeserializer(LocalTime.class,
                new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME_PATTERN)));

        // ── IEnum Serializer ──────────────────────────────────
        // Serializes all IEnum implementations as {code, value, desc} objects,
        // replacing per-enum @JsonFormat(shape=OBJECT) annotations.
        this.addSerializer((Class) IEnum.class, IEnumSerializer.INSTANCE);
    }

    /**
     * Serializes any {@link IEnum} as {@code {"code":"NAME","value":"...", "desc":"..."}}.
     */
    static class IEnumSerializer extends StdSerializer<IEnum<?>> {
        static final IEnumSerializer INSTANCE = new IEnumSerializer();

        IEnumSerializer() {
            super(IEnum.class, true);
        }

        @Override
        public void serialize(IEnum<?> value, JsonGenerator gen, SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeStringProperty("code", ((Enum<?>) value).name());
            gen.writeStringProperty("value", String.valueOf(value.getValue()));
            gen.writeStringProperty("desc", value.getDesc());
            gen.writeEndObject();
        }
    }
}

