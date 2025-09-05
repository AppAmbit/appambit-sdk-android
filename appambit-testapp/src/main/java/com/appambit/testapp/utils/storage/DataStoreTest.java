package com.appambit.testapp.utils.storage;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.appambit.sdk.AppConstants;

public class DataStoreTest extends SQLiteOpenHelper {
    public DataStoreTest(Context context) {
        super(context, AppConstants.DATABASE_NAME, null, AppConstants.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {}

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}
}