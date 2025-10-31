package com.appambit.sdk;

import android.content.pm.PackageInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.enums.LogType;
import com.appambit.sdk.models.logs.ExceptionInfo;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.models.logs.LogResponse;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.LogEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.PackageInfoHelper;
import com.appambit.sdk.utils.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

class Logging {
    private static ApiService mApiService;
    private static ExecutorService mExecutor;
    private static final String TAG = Logging.class.getSimpleName();
    private static Storable mStorable;

    public static void Initialize() {
        mExecutor = ServiceLocator.getExecutorService();
        mApiService = ServiceLocator.getApiService();
        mStorable = ServiceLocator.getStorageService();
    }

    public static void LogEvent(String message, LogType logType, Exception exception, Map<String, String> properties, String classFqn, String fileName, int lineNumber) {
        ExceptionInfo exceptionInfo = (exception != null) ? ExceptionInfo.fromException(ServiceLocator.getContext(), exception) : null;
        logEvent(message, logType, exceptionInfo, properties, classFqn, fileName, lineNumber);
    }

    public static void logEvent(String message, LogType logType, ExceptionInfo exception,
                                Map<String, String> properties, String classFqn, String fileName, int lineNumber) {
        if (!SessionManager.isSessionActivate) {
            return;
        }

        String stackTrace = (exception != null && exception.getStackTrace() != null && !exception.getStackTrace().isEmpty())
                ? exception.getStackTrace()
                : AppConstants.NO_STACK_TRACE_AVAILABLE;

        String file = (exception != null) ? exception.getCrashLogFile() : null;
        PackageInfo pInfo = PackageInfoHelper.getPackageInfo(ServiceLocator.getContext());
        LogEntity log = new LogEntity();
        assert pInfo != null;
        log.setId(UUID.randomUUID());
        if (exception != null && exception.getSessionId() != null && !exception.getSessionId().isEmpty()) {
            log.setSessionId(exception.getSessionId());
        } else {
            log.setSessionId(SessionManager.getSessionId());
        }

        log.setAppVersion(pInfo.versionName + " (" + pInfo.versionCode + ")");
        log.setClassFQN((exception != null && exception.getClassFullName() != null) ? exception.getClassFullName()
                : (classFqn != null ? classFqn : AppConstants.UNKNOWN_CLASS));
        log.setFileName((exception != null && exception.getFileNameFromStackTrace() != null) ? exception.getFileNameFromStackTrace()
                : (fileName != null ? fileName : AppConstants.UNKNOWN_FILENAME));
        log.setLineNumber((exception != null && exception.getLineNumberFromStackTrace() != 0) ? exception.getLineNumberFromStackTrace()
                : lineNumber);
        log.setMessage(!StringUtils.isNullOrBlank(message) ? message :
                (exception != null && exception.getMessage() != null) ? exception.getMessage() : AppConstants.UNKNOWN_CLASS);
        log.setStackTrace(stackTrace);

        Map<String, String> cleanedProps = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, String> e : properties.entrySet()) {
                String v = e.getValue();
                if (v != null && !v.trim().isEmpty()) {
                    cleanedProps.put(e.getKey(), v);
                }
            }
        }
        log.setContext(cleanedProps);

        log.setType(logType);
        log.setFile((logType == LogType.CRASH && exception != null) ? file : null);
        log.setCreatedAt(DateUtils.getUtcNow());

        sendOrSaveLogEventAsync(log);
    }

    private static void sendOrSaveLogEventAsync(LogEntity log) {
        try {
            AppAmbitTaskFuture<Void> appAmbitTaskFuture = new AppAmbitTaskFuture<>();
            mExecutor.execute(() -> {
                var logEndpoint = new LogEndpoint(log);

                try {
                    ApiResult<LogResponse> logResponse = mApiService.executeRequest(logEndpoint, LogResponse.class);

                    if (logResponse == null || logResponse.errorType != ApiErrorType.None) {
                        storeLogInDb(log);
                        appAmbitTaskFuture.then(result -> android.util.Log.d(TAG, "Log event stored in database: " + log.getMessage()));
                    }
                } catch (Exception ex) {
                    appAmbitTaskFuture.onError(error -> android.util.Log.d(TAG, "Error sending log event - Api: " + ex.getMessage()));
                }
            });
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private static void storeLogInDb(@NonNull LogEntity log) {
        mExecutor.execute(() -> {
            mStorable.putLogEvent(log);
            android.util.Log.d(TAG, "Log event stored in database");
        });
    }

}