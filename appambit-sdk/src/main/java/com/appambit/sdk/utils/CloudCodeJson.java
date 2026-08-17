package com.appambit.sdk.utils;

import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CloudCodeJson {
    private CloudCodeJson() {}

    public static String encodeObject(Map<String, ?> value) throws JSONException {
        return encodeValue(value).toString();
    }

    public static Map<String, Object> snapshotObject(Map<String, ?> value) throws JSONException {
        Object snapshot = snapshotValue(value);
        if (!(snapshot instanceof Map)) throw new JSONException("JSON body must be an object");
        return (Map<String, Object>) snapshot;
    }

    public static Object decodeDynamic(@Nullable String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) return null;
        Object parsed = new JSONTokener(json).nextValue();
        return fromJsonValue(parsed);
    }

    @SuppressWarnings("unchecked")
    public static <T> T decode(@Nullable String json, Class<T> type) throws Exception {
        Object value = decodeDynamic(json);
        if (value == null) return null;
        return (T) convertValue(value, type);
    }

    private static Object convertValue(@Nullable Object value, Type targetType) throws Exception {
        if (value == null) return null;
        if (targetType == Object.class) return value;
        if (targetType instanceof Class) {
            Class<?> type = (Class<?>) targetType;
            if (type.isAssignableFrom(value.getClass())) return value;
            if (type == String.class) return value instanceof String ? value : String.valueOf(value);
            if (type == Boolean.class || type == boolean.class) return value;
            if (type == Integer.class || type == int.class) return ((Number) value).intValue();
            if (type == Long.class || type == long.class) return ((Number) value).longValue();
            if (type == Double.class || type == double.class) return ((Number) value).doubleValue();
            if (type == Float.class || type == float.class) return ((Number) value).floatValue();
            if (type.isEnum()) return Enum.valueOf((Class<? extends Enum>) type, String.valueOf(value));
            if (type.isArray() && value instanceof List) {
                List<?> source = (List<?>) value;
                Object result = Array.newInstance(type.getComponentType(), source.size());
                for (int i = 0; i < source.size(); i++) {
                    Array.set(result, i, convertValue(source.get(i), type.getComponentType()));
                }
                return result;
            }
            if (Map.class.isAssignableFrom(type) || List.class.isAssignableFrom(type)) return value;
            if (!(value instanceof Map)) {
                throw new JSONException("Expected a JSON object for " + type.getName());
            }
            return convertObject((Map<?, ?>) value, type);
        }
        if (targetType instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) targetType;
            Type rawType = parameterized.getRawType();
            Type[] arguments = parameterized.getActualTypeArguments();
            if (rawType == List.class || (rawType instanceof Class && List.class.isAssignableFrom((Class<?>) rawType))) {
                List<Object> result = new ArrayList<>();
                if (value instanceof Iterable) {
                    for (Object item : (Iterable<?>) value) {
                        result.add(convertValue(item, arguments[0]));
                    }
                }
                return result;
            }
            if (rawType == Map.class || (rawType instanceof Class && Map.class.isAssignableFrom((Class<?>) rawType))) {
                Map<Object, Object> result = new LinkedHashMap<>();
                if (value instanceof Map) {
                    Type valueType = arguments.length > 1 ? arguments[1] : Object.class;
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                        result.put(entry.getKey(), convertValue(entry.getValue(), valueType));
                    }
                }
                return result;
            }
        }
        return value;
    }

    private static Object convertObject(Map<?, ?> source, Class<?> type) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        for (Class<?> current = type; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String key = field.isAnnotationPresent(JsonKey.class)
                        ? field.getAnnotation(JsonKey.class).value()
                        : field.getName();
                if (!source.containsKey(key) || source.get(key) == null) continue;
                field.setAccessible(true);
                field.set(instance, convertValue(source.get(key), field.getGenericType()));
            }
        }
        return instance;
    }

    private static Object fromJsonValue(Object value) throws JSONException {
        if (value == JSONObject.NULL) return null;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Map<String, Object> result = new LinkedHashMap<>();
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, fromJsonValue(object.get(key)));
            }
            return result;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) result.add(fromJsonValue(array.get(i)));
            return result;
        }
        return value;
    }

    private static Object encodeValue(@Nullable Object value) throws JSONException {
        if (value == null) return JSONObject.NULL;
        if (value instanceof String || value instanceof Boolean || value instanceof Number) return value;
        if (value instanceof Map) {
            JSONObject object = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new JSONException("JSON object keys must be strings");
                object.put((String) entry.getKey(), encodeValue(entry.getValue()));
            }
            return object;
        }
        if (value instanceof Iterable) {
            JSONArray array = new JSONArray();
            for (Object item : (Iterable<?>) value) array.put(encodeValue(item));
            return array;
        }
        if (value.getClass().isArray()) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < Array.getLength(value); i++) array.put(encodeValue(Array.get(value, i)));
            return array;
        }
        throw new JSONException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static Object snapshotValue(@Nullable Object value) throws JSONException {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new JSONException("JSON object keys must be strings");
                }
                copy.put((String) entry.getKey(), snapshotValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof Iterable) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (Iterable<?>) value) copy.add(snapshotValue(item));
            return Collections.unmodifiableList(copy);
        }
        if (value.getClass().isArray()) {
            List<Object> copy = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                copy.add(snapshotValue(Array.get(value, i)));
            }
            return Collections.unmodifiableList(copy);
        }
        throw new JSONException("Unsupported JSON value: " + value.getClass().getName());
    }
}
