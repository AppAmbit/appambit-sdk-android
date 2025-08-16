package com.appambit.sdk.services.storage.contract;

public final class AppSecretContract {
    public static final String TABLE_NAME = "secrets";

    public static final class Columns {
        private Columns() {}

        public static final String ID         = "id";
        public static final String CONSUMER_ID     = "consumerId";
        public static final String APP_ID     = "appId";
        public static final String DEVICE_ID  = "deviceId";
        public static final String TOKEN      = "token";
        public static final String SESSION_ID = "sessionId";
        public static final String USER_ID    = "userId";
        public static final String USER_EMAIL = "userEmail";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID            + " TEXT PRIMARY KEY, "
                    + Columns.CONSUMER_ID   + " TEXT, "
                    + Columns.APP_ID        + " TEXT, "
                    + Columns.DEVICE_ID     + " TEXT, "
                    + Columns.TOKEN         + " TEXT, "
                    + Columns.SESSION_ID    + " TEXT, "
                    + Columns.USER_ID       + " TEXT, "
                    + Columns.USER_EMAIL    + " TEXT "
                    + ");";
}

