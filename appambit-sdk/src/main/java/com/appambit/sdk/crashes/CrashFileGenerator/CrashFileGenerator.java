package com.appambit.sdk.crashes.CrashFileGenerator;

import android.content.pm.PackageInfo;
import androidx.annotation.NonNull;
import com.appambit.sdk.core.utils.PackageInfoHelper;
import java.lang.Exception;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import android.content.Context;
import android.os.Build;

public class CrashFileGenerator {

    @NonNull
    public static String generateCrashLog(Context context, @NonNull Exception ex, String deviceId) {
        StringBuilder log = new StringBuilder();

        // Header
        addHeader(context, log, deviceId);

        log.append("\n");

        // Exception Stack Trace
        log.append("Android Exception Stack:\n");
        log.append(Arrays.toString(ex.getStackTrace()));

        log.append("\n");

        // Threads info
        addThreads(log);

        return log.toString();
    }

    public static void addHeader(Context context, @NonNull StringBuilder log, String deviceId) {
        PackageInfo pInfo = PackageInfoHelper.getPackageInfo(context);
        assert pInfo != null;
        String versionName = pInfo.versionName;
        long versionCode = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                ? pInfo.getLongVersionCode()
                : pInfo.versionCode;

        log.append("Package: ").append(context.getPackageName()).append("\n");
        log.append("Version Code: ").append(versionCode).append("\n");
        log.append("Version Name: ").append(versionName).append("\n");
        log.append("Android: ").append(Build.VERSION.SDK_INT).append("\n");
        log.append("Android Build: ").append(Build.DISPLAY).append("\n");
        log.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        log.append("Model: ").append(Build.MODEL).append("\n");
        log.append("Device Id: ").append(deviceId).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date());
        log.append("Date: ").append(date).append("\n");
    }

    public static void addThreads(StringBuilder log) {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            allStackTraces.keySet().stream()
                    .sorted(Comparator.comparingLong(Thread::getId))
                    .forEach(thread -> {
                        StackTraceElement[] stack = allStackTraces.get(thread);
                        log.append("Thread ").append(thread.getId())
                                .append(" - ").append(thread.getName())
                                .append(" (").append(thread.getState()).append("):\n");

                        int count = 0;
                        assert stack != null;
                        for (StackTraceElement element : stack) {
                            String countPadded = String.format("%-4s", count++);
                            log.append(countPadded).append("    at ")
                                    .append(element.getClassName()).append(".")
                                    .append(element.getMethodName()).append(" (")
                                    .append(element.getFileName()).append(":")
                                    .append(element.getLineNumber()).append(")\n");
                        }
                        log.append("\n");
                    });
        }
    }
}