package com.appambit.sdk.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.appambit.sdk.core.utils.PackageInfoHelper;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private static final String TAG = CrashHandler.class.getSimpleName();

    public CrashHandler(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void install(Context context) {
        CrashHandler crashHandler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        Log.d(TAG, "Crash handler installed successfully");
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            JSONObject crashInfo = createCrashInfo(thread, throwable);

            saveCrashToFileJson(context, crashInfo);

            createCrashFlag(context);

            Log.e(TAG, "Crash detected and saved", throwable);

        } catch (Exception e) {
            Log.e(TAG, "Error saving crash information", e);
        } finally {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        }
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.S)
    private JSONObject createCrashInfo(@NonNull Thread thread, @NonNull Throwable throwable) {
        JSONObject crashInfo = new JSONObject();

        try {
            crashInfo.put("app_version", Objects.requireNonNull(PackageInfoHelper.getPackageInfo(context)).versionName);
            crashInfo.put("device_model", Build.MODEL);
            crashInfo.put("device_manufacturer", Build.MANUFACTURER);
            crashInfo.put("android_version", Build.VERSION.RELEASE);
            crashInfo.put("android_device", Build.DEVICE);
            crashInfo.put("api_level", Build.VERSION.SDK_INT);
            crashInfo.put("thread_name", thread.getName());
            crashInfo.put("thread_id", thread.getId());
            crashInfo.put("exception_class", throwable.getClass().getName());
            crashInfo.put("exception_message", throwable.getMessage() != null ? throwable.getMessage() :
                                                throwable.getStackTrace()[0].getClassName());
            crashInfo.put("stack_trace", getStackTrace(throwable));
            crashInfo.put("app_package", "AppAmbitAndroid");
            crashInfo.put("inner_exception", throwable.getClass().getSimpleName());
            crashInfo.put("exception_full_class", throwable.getStackTrace()[0].getClassName());
            crashInfo.put("line_number_from_stack_trace", throwable.getStackTrace()[0].getLineNumber());
            crashInfo.put("date", toIsoUtc(new Date()));

        } catch (JSONException e) {
            Log.e(TAG, "Error creating crash JSON", e);
        }

        return crashInfo;
    }

    @NonNull
    private String getStackTrace(@NonNull Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    private static void saveCrashToFileJson(Context context, JSONObject crashInfo) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File crashFile = new File(context.getFilesDir(), "crash_" + timeStamp + ".json");

            JSONObject json = new JSONObject();
            json.put("Type", crashInfo.optString("exception_class"));
            json.put("Message", crashInfo.optString("exception_message"));
            json.put("StackTrace", crashInfo.optString("stack_trace"));
            json.put("Source", crashInfo.optString("app_package"));
            json.put("InnerException", crashInfo.optString("inner_exception"));
            json.put("FileNameFromStackTrace", "UnknownFileName");
            json.put("ClassFullName", crashInfo.optString("exception_full_class"));
            json.put("LineNumberFromStackTrace", crashInfo.optString("line_number_from_stack_trace", "0"));
            json.put("CrashLogFile", crashInfoToText(crashInfo));
            json.put("CreatedAt", toIsoUtc(new Date()));

            try (FileWriter writer = new FileWriter(crashFile)) {
                writer.write(json.toString(2));
            }

            Log.d(TAG, "Crash JSON saved in: " + crashFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error saving crash information to file", e);
        }
    }

    public static void setCrashFlag(@NonNull Context context, boolean didCrash) {
        File flagFile = new File(context.getFilesDir(), "did_app_crash.json");
        if (didCrash) {
            if (!flagFile.exists()) {
                createCrashFlag(context);
            }
        } else {
            if (flagFile.exists()) {
                clearCrashFile(context);
            }
        }
    }

    private static void createCrashFlag(@NonNull Context context) {
        try {
            File flagFile = new File(context.getFilesDir(), "did_app_crash.json");
            FileOutputStream fos = new FileOutputStream(flagFile);
            fos.write("".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error creating crash flag file", e);
        }
    }

    public static boolean didCrashInLastSession(@NonNull Context context) {
        File flagFile = new File(context.getFilesDir(), "did_app_crash.json");
        return flagFile.exists();
    }

    @NonNull
    public static File getCrashFile(@NonNull Context context) {
        return new File(context.getFilesDir(), "did_app_crash.json");
    }

    public static void clearCrashFile(Context context) {
        File crashFile = getCrashFile(context);
        if (crashFile.exists()) {
            crashFile.delete();
            Log.d(TAG, "Crash file cleared");
        }
    }

    @NonNull
    private static String toIsoUtc(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }
    @NonNull
    private static String crashInfoToText(@NonNull JSONObject crashInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Crash Report\n");
        sb.append("============\n");

        Iterator<String> keys = crashInfo.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = crashInfo.opt(key);

            sb.append((key)).append(": ").append(value).append("\n");
        }

        return sb.toString();
    }

}