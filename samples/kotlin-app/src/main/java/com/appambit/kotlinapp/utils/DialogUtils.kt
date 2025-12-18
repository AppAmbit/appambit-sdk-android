package com.appambit.kotlinapp.utils

import android.app.AlertDialog
import android.content.Context

fun dialogUtils(context: Context, title: String, message: String) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder
        .setMessage(message)
        .setTitle(title)
        .setPositiveButton("Ok") { dialog, which ->
           dialog.dismiss()
        }

    val dialog: AlertDialog = builder.create()
    dialog.show()
}