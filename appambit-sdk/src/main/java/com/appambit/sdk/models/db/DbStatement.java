package com.appambit.sdk.models.db;

import com.appambit.sdk.utils.JsonKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DbStatement {
    @JsonKey("sql")
    private String sql;
    @JsonKey("params")
    private List<Object> params;

    private DbStatement() {}

    private DbStatement(String sql, List<Object> params) {
        this.sql = sql;
        this.params = (params != null && !params.isEmpty()) ? params : null;
    }

    public static DbStatement of(String sql) {
        return new DbStatement(sql, null);
    }

    public static DbStatement of(String sql, Object... params) {
        // Java passes of(sql, null) as a null array, not as one null parameter.
        // of(sql) already covers "no parameters", so treat it as a single NULL.
        if (params == null) params = new Object[]{ null };
        List<Object> list = new ArrayList<>(Arrays.asList(params));
        return new DbStatement(sql, list.isEmpty() ? null : list);
    }

    public String getSql() { return sql; }
    public List<Object> getParams() { return params; }
}
