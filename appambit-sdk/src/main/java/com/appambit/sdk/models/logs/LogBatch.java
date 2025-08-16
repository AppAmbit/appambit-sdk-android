package com.appambit.sdk.models.logs;

import com.appambit.sdk.utils.JsonKey;

import java.util.List;

public class LogBatch {
    @JsonKey("logs")
    public List<LogEntity> Logs;

    public List<LogEntity> getLogs() {
        return Logs;
    }

    public void setLogs(List<LogEntity> logs) {
        Logs = logs;
    }
}