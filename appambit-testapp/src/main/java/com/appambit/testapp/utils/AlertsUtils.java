package com.appambit.testapp.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class AlertsUtils {

    public static void showAlert(Context context, String title, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Ok", null)
                    .show();
        });
    }
}