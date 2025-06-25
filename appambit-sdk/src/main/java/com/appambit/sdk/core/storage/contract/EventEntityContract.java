package com.appambit.sdk.core.storage.contract;

public final class EventEntityContract {
    public static final String TABLE_NAME = "events";

    public static final class Columns {
        private Columns() {}
        public static final String ID                  = "id";
        public static final String DATA_JSON           = "data_json";
        public static final String NAME                = "name";
        public static final String CREATED_AT          = "createdAt";
        static final String TYPE_INTEGER               = "INTEGER";
        static final String TYPE_TEXT                  = "TEXT";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID          + " " + Columns.TYPE_TEXT + " PRIMARY KEY, "
                    + Columns.DATA_JSON   + " " + Columns.TYPE_TEXT    + ", "
                    + Columns.NAME        + " " + Columns.TYPE_TEXT    + ", "
                    + Columns.CREATED_AT   + " " + Columns.TYPE_INTEGER
                    + ");";
}
