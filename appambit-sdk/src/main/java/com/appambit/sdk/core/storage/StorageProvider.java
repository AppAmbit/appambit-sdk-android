package com.appambit.sdk.core.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.appambit.sdk.core.AppConstants;
import com.appambit.sdk.core.storage.contract.AppSecretContract;
import com.appambit.sdk.core.storage.contract.EventEntityContract;
import com.appambit.sdk.core.storage.contract.LogEntityContract;

class StorageProvider extends SQLiteOpenHelper {
    StorageProvider(Context context) {
        super(context, AppConstants.DATABASE_NAME, null, AppConstants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(AppSecretContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogEntityContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(EventEntityContract.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}