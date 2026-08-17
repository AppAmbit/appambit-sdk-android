package com.appambit.javaapp.models;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class CloudCodeDashboardSummary {
    @JsonKey("task_count")
    public Integer task_count;
    @JsonKey("database_available")
    public boolean database_available;
    @JsonKey("database_tables_ready")
    public boolean database_tables_ready;
    @JsonKey("posts")
    public List<String> posts;
}
