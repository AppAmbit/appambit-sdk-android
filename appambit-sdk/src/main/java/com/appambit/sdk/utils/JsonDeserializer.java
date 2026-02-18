package com.appambit.sdk.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

@SuppressLint("NewApi")
public class JsonDeserializer {
    private static final String TAG = "JsonDeserializer";
    private static final SimpleDateFormat iso8601Format;
    static {
        iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    public static <T> T deserializeFromJSONStringContent(JSONObject json, Class<T> cls) {
        try {
            T instance = cls.getDeclaredConstructor().newInstance();
            for (Field field : cls.getDeclaredFields()) {
                field.setAccessible(true);
                String key = field.isAnnotationPresent(JsonKey.class)
                        ? Objects.requireNonNull(field.getAnnotation(JsonKey.class)).value()
                        : field.getName();
                if (json.has(key) && !json.isNull(key)) {
                    Object value = json.get(key);
                    Class<?> fieldType = field.getType();
                    if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(instance, ((Number) value).intValue());
                    } else if (fieldType == long.class || fieldType == Long.class) {
                        field.set(instance, ((Number) value).longValue());
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        field.set(instance, ((Number) value).doubleValue());
                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                        field.set(instance, value instanceof Boolean ? value : Boolean.parseBoolean(value.toString()));
                    } else if (fieldType == String.class) {
                        field.set(instance, value.toString());
                    } else if (fieldType == Date.class) {
                        try {
                            Date parsedDate = iso8601Format.parse(value.toString());
                            field.set(instance, parsedDate);
                        } catch (ParseException ex) {
                            throw new RuntimeException("Invalid date format for field: " + key, ex);
                        }
                    } else if (Map.class.isAssignableFrom(fieldType) && value instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) value;
                        Map<String, Object> map = new HashMap<>();

                        Iterator<String> keys = jsonObject.keys();
                        while (keys.hasNext()) {
                            String mapKey = keys.next();
                            Object mapValue = jsonObject.get(mapKey);

                            if (mapValue instanceof JSONObject) {
                                map.put(mapKey, mapValue.toString());
                            } else if (mapValue instanceof JSONArray) {
                                map.put(mapKey, mapValue.toString());
                            } else {
                                map.put(mapKey, mapValue);
                            }
                        }
                        field.set(instance, map);
                    }else {
                        Log.d(TAG, "Could not parse JSON. Data not serialized");
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JSON. Something went wrong.", e);
        }
    }

    public static <T> List<T> deserializeFromJSONArrayContent(JSONArray jsonArray, Class<T> cls) {
        try {
            List<T> resultList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                T item = deserializeFromJSONStringContent(jsonObject, cls);
                resultList.add(item);
            }
            return resultList;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JSON Array. Something went wrong.", e);
        }
    }

    public static <T> T deserializeFromJSONResponse(String jsonString, Class<T> cls) {
        try {
            String trimmedJson = jsonString.trim();

            if (trimmedJson.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonString);
                return (T) deserializeFromJSONArrayContent(jsonArray, cls);
            } else if (trimmedJson.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(jsonString);
                return deserializeFromJSONStringContent(jsonObject, cls);
            } else {
                throw new RuntimeException("Invalid JSON format. Must start with '[' or '{'");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JSON response. Something went wrong.", e);
        }
    }

}