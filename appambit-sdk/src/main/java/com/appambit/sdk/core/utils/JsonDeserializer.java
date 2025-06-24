package com.appambit.sdk.core.utils;

import android.annotation.SuppressLint;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

@SuppressLint("NewApi")
public class JsonDeserializer {
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
                    } else {

                    }
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse JSON. Something went wrong.", e);
        }
    }
}