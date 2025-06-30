package com.appambit.sdk.crashes;

import android.content.Context;
import android.content.pm.PackageInfo;
import androidx.annotation.NonNull;
import com.appambit.sdk.core.AppConstants;
import com.appambit.sdk.core.ServiceLocator;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.enums.LogType;
import com.appambit.sdk.core.models.logs.ExceptionInfo;
import com.appambit.sdk.core.models.logs.Log;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.models.logs.LogResponse;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.services.endpoints.LogEndpoint;
import com.appambit.sdk.core.services.interfaces.ApiService;
import com.appambit.sdk.core.storage.Storable;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.DateUtils;
import com.appambit.sdk.core.utils.PackageInfoHelper;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

class Logging {

    private static final ApiService mApiService = ServiceLocator.getApiService();
    private static final ExecutorService mExecutor = ServiceLocator.getExecutorService();
    private static final String TAG = Logging.class.getSimpleName();
    static Storable mStorable = ServiceLocator.getStorageService();

    public static void LogEvent(Context context, String message, LogType logType, Exception exception, Map<String, String> properties, String classFqn, String fileName, int lineNumber, Date createdAt) {
        ExceptionInfo exceptionInfo = (exception != null) ? ExceptionInfo.fromException(context, exception) : null;
        logEvent(context, message, logType, exceptionInfo, properties, classFqn, fileName, lineNumber, createdAt);
    }

    public static void logEvent(Context context, String message, LogType logType, ExceptionInfo exception,
                                Map<String, String> properties, String classFqn, String fileName,
                                int lineNumber, Date createdAt) {

        String stackTrace = (exception != null && exception.getStackTrace() != null && !exception.getStackTrace().isEmpty())
                ? exception.getStackTrace()
                : AppConstants.NO_STACK_TRACE_AVAILABLE;

        String file = (exception != null) ? exception.getCrashLogFile() : null;
        PackageInfo pInfo = PackageInfoHelper.getPackageInfo(context);
        LogEntity log = new LogEntity();
        assert pInfo != null;
        log.setId(UUID.randomUUID());
        log.setAppVersion(pInfo.versionName + " (" + pInfo.versionCode + ")");
        log.setClassFQN((exception != null && exception.getClassFullName() != null) ? exception.getClassFullName()
                : (classFqn != null ? classFqn : AppConstants.UNKNOWN_CLASS));
        log.setFileName((exception != null && exception.getFileNameFromStackTrace() != null) ? exception.getFileNameFromStackTrace()
                : (fileName != null ? fileName : AppConstants.UNKNOWN_FILENAME));
        log.setLineNumber((exception != null && exception.getLineNumberFromStackTrace() != 0) ? exception.getLineNumberFromStackTrace()
                : lineNumber);
        log.setMessage((exception != null && exception.getMessage() != null) ? exception.getMessage()
                : (message != null ? message : ""));
        log.setStackTrace(stackTrace);
        log.setContext(properties != null ? properties : new HashMap<>());
        log.setType(logType);
        log.setFile((logType == LogType.CRASH && exception != null) ? file : null);
        log.setCreatedAt(createdAt != null ? createdAt : DateUtils.getUtcNow());

        sendOrSaveLogEventAsync(log);
    }

    private static AppAmbitTaskFuture<Void> sendOrSaveLogEventAsync(Log log) {
        mExecutor.execute(() -> {
            AppAmbitTaskFuture<Void> appAmbitTaskFuture = new AppAmbitTaskFuture<>();
            var logEndpoint = new LogEndpoint(log);

            try {
                ApiResult<LogResponse> logResponse = mApiService.executeRequest(logEndpoint, LogResponse.class);

                if (logResponse == null || logResponse.errorType != ApiErrorType.None) {
                    storeLogInDb(log);
                    appAmbitTaskFuture.then(result -> android.util.Log.d(TAG, "Log event stored in database: " + log.getMessage()));
                }
            }
            catch (Exception ex) {
                appAmbitTaskFuture.then(result -> android.util.Log.d(TAG, "Error sending log event: " + ex.getMessage()));
            }
        });
        return null;
    }

    private static void storeLogInDb(@NonNull Log log) {
        mExecutor.execute(() -> {
            LogEntity logEntity = new LogEntity();
            logEntity.setId(UUID.randomUUID());
            logEntity.setCreatedAt(DateUtils.getUtcNow());
            logEntity.setAppVersion(log.getAppVersion());
            logEntity.setClassFQN(log.getClassFQN());
            logEntity.setFileName(log.getFileName());
            logEntity.setLineNumber(log.getLineNumber());
            logEntity.setMessage(log.getMessage());
            logEntity.setStackTrace(log.getStackTrace());
            logEntity.setContext(log.getContext());
            logEntity.setType(log.getType());
            logEntity.setFile(log.getFile());
            mStorable.putLogEvent(logEntity);
            android.util.Log.d(TAG, "Log event stored in database");
        });
    }

}