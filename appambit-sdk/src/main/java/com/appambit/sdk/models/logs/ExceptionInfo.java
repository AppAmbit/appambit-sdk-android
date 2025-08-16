package com.appambit.sdk.models.logs;

import static com.appambit.sdk.utils.DateUtils.fromIsoUtc;
import com.appambit.sdk.AppConstants;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.JsonKey;
import com.appambit.sdk.crashFileGenerator.CrashFileGenerator;
import android.content.Context;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.Date;

public class ExceptionInfo {
    @JsonKey("Type")
    private String type;
    @JsonKey("Message")
    private String message;
    @JsonKey("StackTrace")
    private String stackTrace;
    @JsonKey("Source")
    private String source;
    @JsonKey("InnerException")
    private String innerException;
    @JsonKey("FileNameFromStackTrace")
    private String fileNameFromStackTrace;
    @JsonKey("ClassFullName")
    private String classFullName;
    @JsonKey("LineNumberFromStackTrace")
    private long lineNumberFromStackTrace;
    @JsonKey("CrashLogFile")
    private String crashLogFile;
    @JsonKey("CreatedAt")
    private Date createdAt;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInnerException() {
        return innerException;
    }

    public void setInnerException(String innerException) {
        this.innerException = innerException;
    }

    public String getFileNameFromStackTrace() {
        return fileNameFromStackTrace;
    }

    public void setFileNameFromStackTrace(String fileNameFromStackTrace) {
        this.fileNameFromStackTrace = fileNameFromStackTrace;
    }

    public String getClassFullName() {
        return classFullName;
    }

    public void setClassFullName(String classFullName) {
        this.classFullName = classFullName;
    }

    public long getLineNumberFromStackTrace() {
        return lineNumberFromStackTrace;
    }

    public void setLineNumberFromStackTrace(long lineNumberFromStackTrace) {
        this.lineNumberFromStackTrace = lineNumberFromStackTrace;
    }

    public String getCrashLogFile() {
        return crashLogFile;
    }

    public void setCrashLogFile(String crashLogFile) {
        this.crashLogFile = crashLogFile;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public static ExceptionInfo fromException(Context context, Exception exception) {
        ExceptionInfo exceptionInfo = new ExceptionInfo();
        exceptionInfo.setType(exception != null ? exception.getClass().getName() : null);
        exceptionInfo.setMessage(exception != null ? exception.getStackTrace()[0].getClassName() : null);
        exceptionInfo.setStackTrace(exception != null ? Arrays.toString(exception.getStackTrace()) : null);
        exceptionInfo.setSource(exception != null ? exception.getStackTrace()[0].getClassName() : null);
        exceptionInfo.setInnerException(exception != null && exception.getCause() != null ? exception.getCause().toString() : null);
        exceptionInfo.setClassFullName(exception != null ? exception.getStackTrace()[0].getClassName() : AppConstants.UNKNOWN_CLASS);
        exceptionInfo.setFileNameFromStackTrace(exception != null ? exception.getStackTrace().getClass().getName() : AppConstants.UNKNOWN_FILENAME);
        exceptionInfo.setLineNumberFromStackTrace(exception != null ? exception.getStackTrace()[0].getLineNumber() : 0);
        assert exception != null;
        exceptionInfo.setCrashLogFile(CrashFileGenerator.generateCrashLog(context, exception));
        exceptionInfo.setCreatedAt(new Date());
        return exceptionInfo;
    }

    @NonNull
    public static ExceptionInfo fromJson(@NonNull JSONObject json) {
        ExceptionInfo info = new ExceptionInfo();
        info.type = json.optString("Type");
        info.message = json.optString("Message");
        info.stackTrace = json.optString("StackTrace");
        info.source = json.optString("Source");
        info.innerException = json.optString("InnerException");
        info.fileNameFromStackTrace = json.optString("FileNameFromStackTrace");
        info.classFullName = json.optString("ClassFullName");
        try {
            info.lineNumberFromStackTrace = Long.parseLong(json.optString("LineNumberFromStackTrace"));
        } catch (NumberFormatException e) {
            info.lineNumberFromStackTrace = 0;
        }
        info.crashLogFile = json.optString("CrashLogFile");
        String dateString = json.optString("CreatedAt");
        try {
            info.createdAt = fromIsoUtc(dateString);
        } catch (Exception e) {
            info.createdAt = DateUtils.getUtcNow();
        }
        return info;
    }
}