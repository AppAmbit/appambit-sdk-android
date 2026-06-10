package com.appambit.sdk.models.db;

import com.appambit.sdk.utils.JsonKey;
import java.util.List;

public class DbQueryRequest {
    @JsonKey("sql")
    private String sql;
    @JsonKey("params")
    private List<Object> params;

    public DbQueryRequest(String sql, List<Object> params) {
        this.sql = sql;
        this.params = (params != null && !params.isEmpty()) ? params : null;
    }
}
