package com.appambit.sdk.services.storage.contract;

public class BreadcrumbContract {

    public static final String TABLE_NAME = "breadcrumbs";

    public static final class Columns {
        private Columns() {
        }

        public static final String ID = "id";
        public static final String SESSION_ID = "sessionId";
        public static final String NAME = "name";
        public static final String CREATED_AT = "createdAt";
        static final String TYPE_TEXT = "TEXT";
        static final String TYPE_INTEGER = "INTEGER";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID + " " + Columns.TYPE_TEXT + " PRIMARY KEY, "
                    + Columns.SESSION_ID + " " + Columns.TYPE_TEXT + ", "
                    + Columns.NAME + " " + Columns.TYPE_TEXT + ", "
                    + Columns.CREATED_AT + " " + Columns.TYPE_INTEGER
                    + ");";

}
