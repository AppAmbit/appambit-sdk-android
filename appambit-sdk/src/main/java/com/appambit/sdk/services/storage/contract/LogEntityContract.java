package com.appambit.sdk.services.storage.contract;

// LogEntityContract.java
public final class LogEntityContract {
    public static final String TABLE_NAME = "logs";

    public static final class Columns {
        public static final String ID              = "id";
        public static final String CREATED_AT      = "createdAt";
        public static final String APP_VERSION     = "appVersion";
        public static final String CLASS_FQN       = "classFQN";
        public static final String FILE_NAME       = "fileName";
        public static final String LINE_NUMBER     = "lineNumber";
        public static final String MESSAGE         = "message";
        public static final String STACK_TRACE     = "stackTrace";
        public static final String CONTEXT_JSON    = "contextJson";
        public static final String TYPE            = "type";
        public static final String FILE            = "file";
        static final String TYPE_INTEGER           = "INTEGER";
        static final String TYPE_TEXT              = "TEXT";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID            + " TEXT PRIMARY KEY, "
                    + Columns.APP_VERSION   + " " + Columns.TYPE_TEXT + ", "
                    + Columns.CLASS_FQN     + " " + Columns.TYPE_TEXT + ", "
                    + Columns.FILE_NAME     + " " + Columns.TYPE_TEXT + ", "
                    + Columns.LINE_NUMBER   + " " + Columns.TYPE_INTEGER + ", "
                    + Columns.MESSAGE       + " " + Columns.TYPE_TEXT + ", "
                    + Columns.STACK_TRACE   + " " + Columns.TYPE_TEXT + ", "
                    + Columns.CONTEXT_JSON       + " " + Columns.TYPE_TEXT + ", "
                    + Columns.TYPE          + " " + Columns.TYPE_TEXT + ", "
                    + Columns.FILE          + " " + Columns.TYPE_TEXT  + ", "
                    + Columns.CREATED_AT     + " " + Columns.TYPE_INTEGER
                    + ");";
}
