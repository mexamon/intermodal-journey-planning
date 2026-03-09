package com.thy.cloud.service.api.serialize;

import com.thy.cloud.base.core.serialize.AirwayJacksonModule;
import com.thy.cloud.service.dao.enums.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link AirwayJacksonModule}'s IEnumSerializer produces
 * {@code {code, value, desc}} objects for API responses, and that
 * Jackson can deserialize enums from plain strings.
 */
class IEnumSerializerTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = JsonMapper.builder()
                .addModule(new AirwayJacksonModule())
                .build();
    }

    // ── Serialization ─────────────────────────────────────────

    @Test
    void serializeEnumModeCategory_producesObjectWithCodeValueDesc() throws Exception {
        String json = mapper.writeValueAsString(EnumModeCategory.AIR);

        assertTrue(json.contains("\"code\""));
        assertTrue(json.contains("\"value\""));
        assertTrue(json.contains("\"desc\""));
        assertTrue(json.contains("\"AIR\""));
        assertTrue(json.contains("\"Air transport\""));
    }

    @Test
    void serializeEnumEdgeStatus_producesObjectFormat() throws Exception {
        String json = mapper.writeValueAsString(EnumEdgeStatus.ACTIVE);

        assertTrue(json.contains("\"code\":\"ACTIVE\""));
        assertTrue(json.contains("\"value\":\"ACTIVE\""));
    }

    @Test
    void serializeEnumPolicyStatus_producesObjectFormat() throws Exception {
        String json = mapper.writeValueAsString(EnumPolicyStatus.DRAFT);

        assertTrue(json.contains("\"code\":\"DRAFT\""));
        assertTrue(json.contains("\"desc\":\"Draft policy\""));
    }

    // ── Deserialization ───────────────────────────────────────

    @Test
    void deserializeEnumFromString() throws Exception {
        EnumModeCategory cat = mapper.readValue("\"AIR\"", EnumModeCategory.class);
        assertEquals(EnumModeCategory.AIR, cat);
    }

    @Test
    void deserializeEnumFromString_groundFixed() throws Exception {
        EnumModeCategory cat = mapper.readValue("\"GROUND_FIXED\"", EnumModeCategory.class);
        assertEquals(EnumModeCategory.GROUND_FIXED, cat);
    }

    // ── Parametrized round-trip ───────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("allEnums")
    void allEnumsSerializeWithCodeValueDesc(String label, Enum<?> enumValue) throws Exception {
        String json = mapper.writeValueAsString(enumValue);
        assertTrue(json.startsWith("{"), "Should serialize as object: " + json);
        assertTrue(json.contains("\"code\""), "Missing 'code' field: " + json);
        assertTrue(json.contains("\"value\""), "Missing 'value' field: " + json);
        assertTrue(json.contains("\"desc\""), "Missing 'desc' field: " + json);
    }

    static Stream<Arguments> allEnums() {
        return Stream.of(
                Arguments.of("EnumModeCategory.AIR", EnumModeCategory.AIR),
                Arguments.of("EnumModeCategory.GROUND_FIXED", EnumModeCategory.GROUND_FIXED),
                Arguments.of("EnumCoverageType.FIXED_STOP", EnumCoverageType.FIXED_STOP),
                Arguments.of("EnumEdgeResolution.STATIC", EnumEdgeResolution.STATIC),
                Arguments.of("EnumEdgeSource.MANUAL", EnumEdgeSource.MANUAL),
                Arguments.of("EnumEdgeStatus.ACTIVE", EnumEdgeStatus.ACTIVE),
                Arguments.of("EnumScheduleType.FIXED", EnumScheduleType.FIXED),
                Arguments.of("EnumLocationType.AIRPORT", EnumLocationType.AIRPORT),
                Arguments.of("EnumPolicyStatus.ACTIVE", EnumPolicyStatus.ACTIVE),
                Arguments.of("EnumPolicyScopeType.GLOBAL", EnumPolicyScopeType.GLOBAL),
                Arguments.of("EnumPolicySegment.DEFAULT", EnumPolicySegment.DEFAULT)
        );
    }
}
