package com.thy.cloud.base.util.map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Map Utility
 *
 * @author Engin Mahmut
 */
public class MapHelper extends HashMap<String, Object> {

    public static <K, V, M> ImmutableMap<K, M> uniqueIndex(Iterable<V> values, Function<? super V, K> keyFunction,
            Function<? super V, M> valueFunction) {
        Iterator<V> iterator = values.iterator();
        checkNotNull(keyFunction);
        checkNotNull(valueFunction);
        ImmutableMap.Builder<K, M> builder = ImmutableMap.builder();
        while (iterator.hasNext()) {
            V value = iterator.next();
            builder.put(keyFunction.apply(value), valueFunction.apply(value));
        }
        try {
            return builder.build();
        } catch (IllegalArgumentException duplicateKeys) {
            throw new IllegalArgumentException(
                    duplicateKeys.getMessage()
                            + ". To index multiple values under the key, use Multimaps.index.",
                    duplicateKeys);
        }
    }

    @Override
    public MapHelper put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Entry.<K, V>comparingByValue().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    public static <K extends Comparable<? super K>, V> Map<K, V> sortByKey(Map<K, V> map) {
        Map<K, V> result = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted(Entry.<K, V>comparingByKey().reversed())
                .forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    public static Map<String, String> exchangeKeyVal(Map<String, String> oldMap) {
        Map<String, String> newMap = new HashMap<>();
        if (oldMap != null && !oldMap.isEmpty()) {
            for (String key : oldMap.keySet()) {
                newMap.put(oldMap.get(key), key);
            }
        }
        return newMap;
    }

    public static Map<String, Object> getDifferenceSetByGuava(Map<String, Object> bigMap,
            Map<String, Object> smallMap) {
        Set<String> differenceSet = Sets.difference(bigMap.keySet(), smallMap.keySet());
        Map<String, Object> result = Maps.newHashMap();
        for (String key : differenceSet) {
            result.put(key, bigMap.get(key));
        }
        return result;
    }

    public static int initialCapacity(int size, float loadFactor) {
        return (int) (size / loadFactor + 1);
    }

    public static int initialCapacity(int size) {
        return initialCapacity(size, 0.75F);
    }
}
