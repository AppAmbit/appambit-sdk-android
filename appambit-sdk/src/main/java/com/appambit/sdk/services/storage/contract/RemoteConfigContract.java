package com.appambit.sdk.services.storage.contract;

public class RemoteConfigContract {

    public static final String TABLE_NAME = "configs";

    public static final class Columns {
        private Columns() {
        }

        public static final String ID = "id";
        public static final String KEY = "key";
        public static final String VALUE = "value";
    }

    public static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + RemoteConfigContract.Columns.ID + " " + BreadcrumbContract.Columns.TYPE_TEXT + " PRIMARY KEY, "
                    + RemoteConfigContract.Columns.KEY + " " + BreadcrumbContract.Columns.TYPE_TEXT + ", "
                    + RemoteConfigContract.Columns.VALUE + " " + BreadcrumbContract.Columns.TYPE_TEXT
                    + ");";

}
