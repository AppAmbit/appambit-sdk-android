package com.appambit.sdk.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
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
            String path = getFilePath(fileName);
            File file = new File(path);
            if (!file.exists()) return null;

            String text = readFile(path);
            boolean isDelete = file.delete();
            Log.d(TAG, "Delete file: " + isDelete);
            JSONObject json = new JSONObject(text);
            return JsonConvertUtils.fromJson(clazz, json);
        } catch (Exception e) {
            Log.d(TAG, "getSavedSingleObject Exception: " + e.getMessage());
            return null;
        }
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

    public static <T extends Identifiable> List<T> getSaveJsonArray(
            String fileNameBase,
            Class<T> clazz,
            T entry
    ) {
        List<T> list = new ArrayList<>();
        try {
            String fileName = fileNameBase.toLowerCase().endsWith(".json")
                    ? fileNameBase
                    : fileNameBase + ".json";
            String path = getFilePath(fileName);
            File file = new File(path);

            if (file.exists()) {
                String text = readFile(path);
                JSONArray array = new JSONArray(text);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    list.add(JsonConvertUtils.fromJson(clazz, obj));
                }
            }

            if (entry != null) {
                boolean exists = false;
                for (T item : list) {
                    if (item.getId().equals(entry.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    list.add(entry);
                    JSONArray newArr = new JSONArray();
                    for (T item : list) {
                        String jsonStr = JsonConvertUtils.toJson(item);
                        JSONObject jsonObject = new JSONObject(jsonStr);
                        newArr.put(jsonObject);
                    }
                    writeFile(path, newArr.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getSaveJsonArray Exception: " + e.getMessage());
        }
        return list;
    }

    public static <T extends Identifiable> List<T> getSaveJsonArray(
            String fileNameBase,
            Class<T> clazz
    ) {
        return getSaveJsonArray(fileNameBase, clazz, null);
    }

    public static <T extends Identifiable> void updateJsonArray(
            String fileNameBase,
            List<T> updatedList
    ) {
        try {
            String fileName = fileNameBase.toLowerCase().endsWith(".json")
                    ? fileNameBase
                    : fileNameBase + ".json";
            String path = getFilePath(fileName);
            File file = new File(path);

            if (updatedList == null || updatedList.isEmpty()) {
                if (file.exists()) {
                    boolean isDelete = file.delete();
                    Log.d(TAG, "Delete file: " + isDelete);
                }
                return;
            }

            JSONArray array = new JSONArray();
            for (T item : updatedList) {
                String jsonStr = JsonConvertUtils.toJson(item);
                JSONObject jsonObject = new JSONObject(jsonStr);
                array.put(jsonObject);
            }
            writeFile(path, array.toString());
        } catch (Exception e) {
            Log.e(TAG, "updateJsonArray Exception: " + e.getMessage());
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
}
