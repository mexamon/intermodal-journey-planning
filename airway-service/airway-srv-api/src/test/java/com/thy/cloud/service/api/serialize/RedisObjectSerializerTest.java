package com.thy.cloud.service.api.serialize;

import com.thy.cloud.data.cache.serializer.RedisObjectSerializer;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.enums.EnumCoverageType;
import com.thy.cloud.service.dao.enums.EnumEdgeResolution;
import com.thy.cloud.service.dao.enums.EnumModeCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link RedisObjectSerializer} correctly serializes/deserializes
 * entities with enum fields — enums should be stored as plain strings in Redis,
 * not as objects.
 */
class RedisObjectSerializerTest {

    private static RedisObjectSerializer serializer;

    @BeforeAll
    static void setup() {
        serializer = new RedisObjectSerializer(null);
    }

    // ── Helpers ───────────────────────────────────────────────

    private static TransportMode buildFlightMode() {
        TransportMode mode = new TransportMode();
        mode.setId(UUID.fromString("9f914e76-74d9-49f1-86ed-b259355d3d27"));
        mode.setCode("FLIGHT");
        mode.setName("Flight");
        mode.setCategory(EnumModeCategory.AIR);
        mode.setCoverageType(EnumCoverageType.FIXED_STOP);
        mode.setEdgeResolution(EnumEdgeResolution.STATIC);
        mode.setRequiresStop(true);
        mode.setMaxWalkingAccessM(0);
        mode.setDefaultSpeedKmh(800);
        mode.setIcon("plane");
        mode.setColorHex("#1E88E5");
        mode.setIsActive(true);
        mode.setSortOrder(1);
        return mode;
    }

    // ── Enum as String ────────────────────────────────────────

    @Test
    void enumSerializedAsString_notObject() {
        TransportMode mode = buildFlightMode();
        byte[] bytes = serializer.serialize(mode);
        assertNotNull(bytes);

        String json = new String(bytes, StandardCharsets.UTF_8);
        // Should contain "AIR" as a string, not {"code":"AIR",...}
        assertTrue(json.contains("\"AIR\""), "Enum should be serialized as string: " + json);
        assertFalse(json.contains("\"Air transport\""), "Enum desc should NOT be in Redis: " + json);
    }

    @Test
    void enumDeserializedCorrectly() {
        TransportMode original = buildFlightMode();
        byte[] bytes = serializer.serialize(original);
        Object result = serializer.deserialize(bytes);

        assertInstanceOf(TransportMode.class, result);
        TransportMode restored = (TransportMode) result;
        assertEquals(EnumModeCategory.AIR, restored.getCategory());
        assertEquals(EnumCoverageType.FIXED_STOP, restored.getCoverageType());
        assertEquals(EnumEdgeResolution.STATIC, restored.getEdgeResolution());
    }

    // ── Full Roundtrip ────────────────────────────────────────

    @Test
    void roundtripTransportMode() {
        TransportMode original = buildFlightMode();
        byte[] bytes = serializer.serialize(original);
        TransportMode restored = (TransportMode) serializer.deserialize(bytes);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getCode(), restored.getCode());
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getCategory(), restored.getCategory());
        assertEquals(original.getCoverageType(), restored.getCoverageType());
        assertEquals(original.getDefaultSpeedKmh(), restored.getDefaultSpeedKmh());
        assertEquals(original.getIsActive(), restored.getIsActive());
    }

    // ── List roundtrip ────────────────────────────────────────

    @Test
    void roundtripArrayOfModes() {
        TransportMode flight = buildFlightMode();
        TransportMode train = new TransportMode();
        train.setId(UUID.randomUUID());
        train.setCode("TRAIN");
        train.setName("Train");
        train.setCategory(EnumModeCategory.GROUND_FIXED);
        train.setCoverageType(EnumCoverageType.FIXED_STOP);
        train.setEdgeResolution(EnumEdgeResolution.STATIC);
        train.setIsActive(true);

        TransportMode[] array = new TransportMode[]{flight, train};
        byte[] bytes = serializer.serialize(array);
        Object result = serializer.deserialize(bytes);

        assertNotNull(result);
        // GenericJacksonJsonRedisSerializer may return as array or list
        if (result instanceof TransportMode[] restored) {
            assertEquals(2, restored.length);
            assertEquals(EnumModeCategory.AIR, restored[0].getCategory());
            assertEquals(EnumModeCategory.GROUND_FIXED, restored[1].getCategory());
        } else if (result instanceof Object[] restored) {
            assertEquals(2, restored.length);
        }
    }

    // ── Null safety ───────────────────────────────────────────

    @Test
    void serializeNull_returnsEmptyBytes() {
        byte[] bytes = serializer.serialize(null);
        assertTrue(bytes == null || bytes.length == 0);
    }

    @Test
    void deserializeNull_returnsNull() {
        Object result = serializer.deserialize(null);
        assertNull(result);
    }

    @Test
    void deserializeEmptyBytes_returnsNull() {
        Object result = serializer.deserialize(new byte[0]);
        assertNull(result);
    }
}
