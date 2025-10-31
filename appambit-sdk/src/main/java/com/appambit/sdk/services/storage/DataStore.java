package com.appambit.sdk.services.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.appambit.sdk.AppConstants;
import com.appambit.sdk.services.storage.contract.AppSecretContract;
import com.appambit.sdk.services.storage.contract.BreadcrumbContract;
import com.appambit.sdk.services.storage.contract.EventEntityContract;
import com.appambit.sdk.services.storage.contract.LogEntityContract;
import com.appambit.sdk.services.storage.contract.SessionContract;

public class DataStore extends SQLiteOpenHelper {
    public DataStore(Context context) {
        super(context, AppConstants.DATABASE_NAME, null, AppConstants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(AppSecretContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(LogEntityContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(EventEntityContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(SessionContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(BreadcrumbContract.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}