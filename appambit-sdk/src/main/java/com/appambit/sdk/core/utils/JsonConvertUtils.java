package com.appambit.sdk.core.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonConvertUtils {
    private static final String TAG = JsonConvertUtils.class.getSimpleName();

    public static String toJson(Object object) throws JSONException {
        if (object == null) return "null";

        if (object instanceof String || object instanceof Number || object instanceof Boolean)
            return JSONObject.quote(object.toString());

        if (object instanceof Enum)
            return JSONObject.quote(((Enum<?>) object).name());

        if (object instanceof Date)
            return JSONObject.quote(toIsoUtc((Date) object));

        if (object instanceof Collection)
            return collectionToJson((Collection<?>) object).toString();

        if (object.getClass().isArray())
            return arrayToJson(object).toString();

        if (object instanceof Map)
            return mapToJson((Map<?, ?>) object).toString();

        return objectToJson(object).toString();
    }

    private static JSONObject objectToJson(Object object) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        Class<?> clazz = object.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (Modifier.isStatic(field.getModifiers())) continue;

                String key = getJsonKey(field);
                Object value = field.get(object);
                if (value == null) continue;

                if (value instanceof Enum)
                    jsonObject.put(key, ((Enum<?>) value).name());
                else if (value instanceof Date)
                    jsonObject.put(key, toIsoUtc((Date) value));
                else if (value instanceof UUID)
                    jsonObject.put(key, value.toString());
                else if (value instanceof Collection)
                    jsonObject.put(key, collectionToJson((Collection<?>) value));
                else if (isSimpleType(value.getClass()))
                    jsonObject.put(key, value);
                else
                    jsonObject.put(key, objectToJson(value));

            } catch (IllegalAccessException e) {
                Log.e(TAG, "Serialization error: " + e.getMessage(), e);
            }
        }
        return jsonObject;
    }

    private static JSONArray collectionToJson(Collection<?> collection) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        for (Object item : collection) {
            if (item instanceof Enum)
                jsonArray.put(((Enum<?>) item).name());
            else if (item instanceof Date)
                jsonArray.put(toIsoUtc((Date) item));
            else if (item instanceof UUID)
                jsonArray.put(item.toString());
            else if (isSimpleType(item.getClass()))
                jsonArray.put(item);
            else
                jsonArray.put(objectToJson(item));
        }
        return jsonArray;
    }

    private static JSONArray arrayToJson(Object array) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            if (item instanceof Enum)
                jsonArray.put(((Enum<?>) item).name());
            else if (item instanceof Date)
                jsonArray.put(toIsoUtc((Date) item));
            else if (item instanceof UUID)
                jsonArray.put(item.toString());
            else if (item != null && isSimpleType(item.getClass()))
                jsonArray.put(item);
            else
                jsonArray.put(objectToJson(item));
        }
        return jsonArray;
    }

    private static JSONObject mapToJson(Map<?, ?> map) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Enum)
                jsonObject.put(entry.getKey().toString(), ((Enum<?>) value).name());
            else if (value instanceof Date)
                jsonObject.put(entry.getKey().toString(), toIsoUtc((Date) value));
            else if (value instanceof UUID)
                jsonObject.put(entry.getKey().toString(), value.toString());
            else
                jsonObject.put(entry.getKey().toString(),
                        isSimpleType(value.getClass()) ? value : objectToJson(value));
        }
        return jsonObject;
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                Number.class.isAssignableFrom(clazz) ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Date.class) ||
                clazz.equals(UUID.class) ||
                clazz.isEnum();
    }

    private static String toIsoUtc(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }

    private static Date fromIsoUtc(String dateStr) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.parse(dateStr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse ISO date: " + dateStr, e);
            return null;
        }
    }

    private static String getJsonKey(Field field) {
        JsonKey annotation = field.getAnnotation(JsonKey.class);
        return (annotation != null) ? annotation.value() : field.getName();
    }

    public static <T> T fromJson(Class<T> clazz, JSONObject json) throws Exception {
        T instance = clazz.newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) continue;

            String key = getJsonKey(field);
            if (!json.has(key) || json.isNull(key)) continue;

            Class<?> type = field.getType();
            Object value = null;

            if (type == String.class)
                value = json.getString(key);
            else if (type == int.class || type == Integer.class)
                value = json.getInt(key);
            else if (type == long.class || type == Long.class)
                value = json.getLong(key);
            else if (type == boolean.class || type == Boolean.class)
                value = json.getBoolean(key);
            else if (type == double.class || type == Double.class)
                value = json.getDouble(key);
            else if (type == float.class || type == Float.class)
                value = (float) json.getDouble(key);
            else if (type == Date.class)
                value = fromIsoUtc(json.getString(key));
            else if (type == UUID.class)
                value = UUID.fromString(json.getString(key));
            else if (type.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum> enumType = (Class<? extends Enum>) type;
                value = Enum.valueOf(enumType, json.getString(key));
            }
            else if (Collection.class.isAssignableFrom(type)) {
                // Soportar solo List<T> por simplicidad
                Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                Class<?> genericClass = (Class<?>) genericType;
                JSONArray array = json.getJSONArray(key);
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject elementObj = array.getJSONObject(i);
                    list.add(fromJson(genericClass, elementObj));
                }
                value = list;
            } else if (json.get(key) instanceof JSONObject) {
                value = fromJson(type, json.getJSONObject(key));
            }

            if (value != null) {
                field.set(instance, value);
            }
        }
        return instance;
    }
}
