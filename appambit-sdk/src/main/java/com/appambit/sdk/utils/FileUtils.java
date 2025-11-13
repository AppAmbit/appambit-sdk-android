package com.appambit.sdk.utils;

import android.content.Context;
import android.util.Log;

import com.appambit.sdk.models.analytics.SessionData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    private static Context context;

    public static void initialize(Context appContext) {
        context = appContext.getApplicationContext();
    }

    public static String getFileName(Class<?> clazz) {
        return clazz.getSimpleName() + ".json";
    }

    public static String getFilePath(String fileName) {
        return context.getFilesDir().getAbsolutePath()
                + File.separator
                + fileName;
    }

    public static <T extends Identifiable> T getSavedSingleObject(Class<T> clazz) {
        try {
            String fileName = getFileName(clazz);
            String filePath = getFilePath(FileUtils.getFileName(SessionData.class));
            Log.d(TAG, "File: " + filePath);

            String path = getFilePath(fileName);
            File file = new File(path);
            if (!file.exists()) return null;

            String text = readFile(path);
            JSONObject json = new JSONObject(text);
            return JsonConvertUtils.fromJson(clazz, json);
        } catch (Exception e) {
            Log.d(TAG, "getSavedSingleObject Exception: " + e.getMessage());
            return null;
        }
    }

    public static <T extends Identifiable> void deleteSingleObject(Class<T> clazz) {
        String fileName = getFileName(clazz);
        String path = getFilePath(fileName);
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        boolean isDelete = file.delete();
        Log.d(TAG, "Delete file: " + isDelete);
    }

    public static <T extends Identifiable> void saveToFile(T entry) {
        if (entry == null) return;
        try {
            Class<?> clazz = entry.getClass();
            String fileName = getFileName(clazz);
            String path = getFilePath(fileName);
            String json = JsonConvertUtils.toJson(entry);
            writeFile(path, json);
            Log.d(TAG, "Saved " + clazz.getSimpleName() + " to " + path);
        } catch (Exception e) {
            Log.d(TAG, "saveToFile Exception: " + e.getMessage());
        }
    }

    private static String readFile(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void writeFile(String path, String content) throws IOException {
        File out = new File(path);
        if (out.getParentFile() != null) out.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(content);
        }
    }

    private static String prepareFileSettings(String fileName) {
        if (!fileName.toLowerCase().endsWith(".json")) {
            fileName = fileName + ".json";
        }
        return getFilePath(fileName);
    }

    public static <T extends Identifiable> List<T> getSaveJsonArray(String fileName, Class<T> clazz) {
        return getSaveJsonArray(fileName, null, clazz);
    }

    public static <T extends Identifiable> List<T> getSaveJsonArray(String fileName, T entry, Class<T> clazz) {
        try {
            String path = prepareFileSettings(fileName);
            List<T> list = new ArrayList<>();
            File f = new File(path);
            if (f.exists()) {
                String text = readFile(path);
                if (!text.isEmpty()) {
                    JSONArray arr = new JSONArray(text);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        T item = JsonConvertUtils.fromJson(clazz, obj);
                        list.add(item);
                    }
                }
            }

            if (entry != null) {
                boolean exists = false;
                for (T x : list) {
                    if (x.getId() != null && x.getId().equals(entry.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    list.add(entry);
                    Collections.sort(list, (a, b) -> {
                        long ta = extractTimestamp(a);
                        long tb = extractTimestamp(b);
                        return Long.compare(ta, tb);
                    });
                    JSONArray out = new JSONArray();
                    for (T it : list) {
                        out.put(new JSONObject(JsonConvertUtils.toJson(it)));
                    }
                    writeFile(path, out.toString(2));
                }
            }

            return list;
        } catch (Exception e) {
            Log.d(TAG, "File Exception: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static <T> void updateJsonArray(String fileName, List<T> updatedList) {
        try {
            String path = prepareFileSettings(fileName);

            if (updatedList == null || updatedList.isEmpty()) {
                File f = new File(path);
                f.delete();
                return;
            }

            JSONArray out = new JSONArray();
            for (T it : updatedList) {
                out.put(new JSONObject(JsonConvertUtils.toJson(it)));
            }
            writeFile(path, out.toString(2));
        } catch (Exception e) {
            Log.d(TAG, "Error to save file json");
        }
    }

    private static long extractTimestamp(Object obj) {
        if (obj == null) return 0L;
        try {
            Method m = obj.getClass().getMethod("getTimestamp");
            Object v = m.invoke(obj);
            long r = toMillis(v);
            if (r != Long.MIN_VALUE) return r;
        } catch (Throwable ignored) { }
        try {
            Method m = obj.getClass().getMethod("getCreatedAt");
            Object v = m.invoke(obj);
            long r = toMillis(v);
            if (r != Long.MIN_VALUE) return r;
        } catch (Throwable ignored) { }
        try {
            Field f = obj.getClass().getDeclaredField("timestamp");
            f.setAccessible(true);
            Object v = f.get(obj);
            long r = toMillis(v);
            if (r != Long.MIN_VALUE) return r;
        } catch (Throwable ignored) { }
        try {
            Field f = obj.getClass().getDeclaredField("createdAt");
            f.setAccessible(true);
            Object v = f.get(obj);
            long r = toMillis(v);
            if (r != Long.MIN_VALUE) return r;
        } catch (Throwable ignored) { }
        return 0L;
    }

    private static long toMillis(Object v) {
        if (v == null) return Long.MIN_VALUE;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof Date) return ((Date) v).getTime();
        if (v instanceof CharSequence) {
            try {
                return Long.parseLong(v.toString());
            } catch (Throwable ignored) { }
        }
        return Long.MIN_VALUE;
    }
}
