package com.appambit.kotlinapp.utils.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.appambit.sdk.AppConstants

class DataStoreTest(context: Context?) :
    SQLiteOpenHelper(context, AppConstants.DATABASE_NAME, null, AppConstants.DB_VERSION) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {}

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase?, i: Int, i1: Int) {}
}