package com.appambit.sdk.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final String TAG = DateUtils.class.getSimpleName();
    public static Date getUtcNow() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return calendar.getTime();
    }

    @NonNull
    public static Date getDateDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return calendar.getTime();
    }

    @NonNull
    public static String toIsoUtc(Date date) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }

    @Nullable
    public static Date fromIsoUtc(String dateStr) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.parse(dateStr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse ISO date: " + dateStr, e);
            return null;
        }
    }

    @NonNull
    public static String toIsoUtcWithMillis(Date date) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Failed to format date to ISO with millis: " + date, e);
            return "";
        }
    }

    @NonNull
    public static String toIsoUtcNoMillis(Date date) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return isoFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Failed to format date to ISO without millis: " + date, e);
            return "";
        }
    }
}