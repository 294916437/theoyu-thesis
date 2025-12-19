package com.theoyu.framework.common.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Map 转换工具类
 * 用于对象与 Hash Map 之间的通用转换
 *
 * @author theoyu
 */
public class MapUtils {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.registerModules(new JavaTimeModule()); // 解决 LocalDateTime 的序列化问题
    }

    /**
     * 初始化:统一使用 Spring Boot 个性化配置的 ObjectMapper
     *
     * @param objectMapper ObjectMapper 实例
     */
    public static void init(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * 将对象转换为 Hash Map (String 键值对)
     * 适用于 Redis Hash 存储
     *
     * @param obj 待转换的对象
     * @return Map<String, String> 键值对,值为 null 的字段会被过滤
     */
    @SneakyThrows
    public static Map<String, String> objectToStringMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        // 先转换为通用 Map
        @SuppressWarnings("unchecked")
        Map<String, Object> map = OBJECT_MAPPER.convertValue(obj, Map.class);

        // 将所有值转换为字符串,过滤 null 值
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));
    }

    /**
     * 将对象转换为 Hash Map (Object 键值对)
     *
     * @param obj 待转换的对象
     * @return Map<String, Object> 键值对
     */
    @SneakyThrows
    public static Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = OBJECT_MAPPER.convertValue(obj, Map.class);
        return map;
    }

    /**
     * 将 Hash Map 转换为指定类型的对象
     *
     * @param map   Hash Map
     * @param clazz 目标对象类型
     * @param <T>   泛型类型
     * @return 转换后的对象
     */
    @SneakyThrows
    public static <T> T mapToObject(Map<?, ?> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * 将 String Map 转换为指定类型的对象
     * 适用于从 Redis Hash 读取数据后的转换
     *
     * @param stringMap String 类型的 Map
     * @param clazz     目标对象类型
     * @param <T>       泛型类型
     * @return 转换后的对象
     */
    @SneakyThrows
    public static <T> T stringMapToObject(Map<String, String> stringMap, Class<T> clazz) {
        if (stringMap == null || stringMap.isEmpty()) {
            return null;
        }

        // 先转换为 JSON 字符串,再转换为对象,利用 Jackson 的类型推断
        String jsonStr = OBJECT_MAPPER.writeValueAsString(stringMap);
        Map<String, Object> objectMap = OBJECT_MAPPER.readValue(jsonStr, Map.class);
        
        return OBJECT_MAPPER.convertValue(objectMap, clazz);
    }

    /**
     * 合并两个 Map
     *
     * @param target 目标 Map
     * @param source 源 Map
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 合并后的 Map
     */
    public static <K, V> Map<K, V> merge(Map<K, V> target, Map<K, V> source) {
        if (target == null) {
            target = new HashMap<>();
        }
        if (source != null) {
            target.putAll(source);
        }
        return target;
    }

    /**
     * 过滤 Map 中值为 null 的条目
     *
     * @param map 原始 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 过滤后的 Map
     */
    public static <K, V> Map<K, V> filterNullValues(Map<K, V> map) {
        if (map == null) {
            return new HashMap<>();
        }

        return map.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}