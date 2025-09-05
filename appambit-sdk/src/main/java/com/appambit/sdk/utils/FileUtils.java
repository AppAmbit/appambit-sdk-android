package com.appambit.sdk.utils;

import android.content.Context;
import android.util.Log;

import com.appambit.sdk.models.analytics.SessionData;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
}
