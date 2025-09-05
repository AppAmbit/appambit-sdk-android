package com.appambit.sdk;

import static com.appambit.sdk.AppConstants.DID_APP_CRASH;
import static com.appambit.sdk.utils.JsonConvertUtils.toJson;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.appambit.sdk.models.logs.ExceptionInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private static final String TAG = CrashHandler.class.getSimpleName();

    public CrashHandler(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static void initialize(Context context) {
        CrashHandler crashHandler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        Log.d(TAG, "Crash handler installed successfully");
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        try {
            Exception exception = (throwable instanceof Exception)
                    ? (Exception) throwable
                    : new Exception(throwable);

            ExceptionInfo exceptionInfo = ExceptionInfo.fromException(context, exception);

            if(!Analytics.isManualSessionEnabled()) {
                SessionManager.saveEndSession();
            }

            if(!SessionManager.isSessionActivate()) {
                return;
            }

            saveCrashToFileJson(context, exceptionInfo);

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

    private static void saveCrashToFileJson(Context context, @NonNull ExceptionInfo info) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File crashFile = new File(context.getFilesDir(), "crash_" + timeStamp + ".json");

            String json = toJson(info);
            Log.d(TAG, "JSON: " + json);
            try (FileWriter writer = new FileWriter(crashFile)) {
                writer.write(json);
            }

            Log.d(TAG, "Crash JSON saved in: " + crashFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Error saving crash information to file", e);
        }
    }

    public static void setCrashFlag(@NonNull Context context, boolean didCrash) {
        File flagFile = new File(context.getFilesDir(), DID_APP_CRASH);
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
            File flagFile = new File(context.getFilesDir(), DID_APP_CRASH);
            FileOutputStream fos = new FileOutputStream(flagFile);
            fos.write("".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error creating crash flag file", e);
        }
    }

    public static boolean didCrashInLastSession(@NonNull Context context) {
        File flagFile = new File(context.getFilesDir(), DID_APP_CRASH);
        return flagFile.exists();
    }

    @NonNull
    public static File getCrashFile(@NonNull Context context) {
        return new File(context.getFilesDir(), DID_APP_CRASH);
    }

    public static void clearCrashFile(Context context) {
        File crashFile = getCrashFile(context);
        if (crashFile.exists()) {
            crashFile.delete();
            Log.d(TAG, "Crash file cleared");
        }
    }

}