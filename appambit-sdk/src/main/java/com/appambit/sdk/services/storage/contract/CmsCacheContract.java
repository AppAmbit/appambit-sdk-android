package com.appambit.sdk.services.storage.contract;

public final class CmsCacheContract {
    public static final String TABLE_NAME = "cms_cache";

    public static final class Columns {
        private Columns() {}

        public static final String CONTENT_TYPE  = "ContentType";
        public static final String JSON_DATA     = "JsonData";
        public static final String LAST_UPDATED  = "LastUpdated";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.CONTENT_TYPE  + " TEXT, "
                    + Columns.JSON_DATA     + " TEXT, "
                    + Columns.LAST_UPDATED  + " DATETIME, "
                    + "PRIMARY KEY (" + Columns.CONTENT_TYPE + ")"
                    + ");";
}
