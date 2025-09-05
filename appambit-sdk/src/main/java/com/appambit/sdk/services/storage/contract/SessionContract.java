package com.appambit.sdk.services.storage.contract;

public final class SessionContract {

    public static final String TABLE_NAME = "sessions";

    public static final class Columns {
        private Columns() {
        }

        public static final String ID = "id";
        public static final String SESSION_ID = "sessionId";
        public static final String START_SESSION_DATE = "startedAt";
        public static final String END_SESSION_DATE = "endedAt";

        static final String TYPE_TEXT = "TEXT";

    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID + " " + Columns.TYPE_TEXT + " PRIMARY KEY, "
                    + Columns.SESSION_ID + " " + Columns.TYPE_TEXT + ", "
                    + Columns.START_SESSION_DATE + " " + Columns.TYPE_TEXT + ", "
                    + Columns.END_SESSION_DATE + " " + Columns.TYPE_TEXT
                    + ");";
}