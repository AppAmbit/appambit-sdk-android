package com.appambit.sdk.core.models.logs;

import com.appambit.sdk.core.AppConstants;
import com.appambit.sdk.core.enums.LogType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Log {

    private String appVersion;

    private String classFQN;

    private String fileName;

    private long lineNumber;

    private String message = "";

    private String stackTrace = AppConstants.NO_STACK_TRACE_AVAILABLE;

    private String contextJson = "{}";

    private LogType type;

    private String file;

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getClassFQN() {
        return classFQN;
    }

    public void setClassFQN(String classFQN) {
        this.classFQN = classFQN;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(long lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public Map<String, String> getContext() {
        Map<String, String> map = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(contextJson);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, jsonObject.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void setContext(Map<String, String> context) {
        JSONObject jsonObject = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.contextJson = jsonObject.toString();
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public LogType getType() {
        return type;
    }

    public void setType(LogType type) {
        this.type = type;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
