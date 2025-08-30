package com.appambit.sdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.enums.LogType;
import com.appambit.sdk.models.logs.ExceptionInfo;
import com.appambit.sdk.models.logs.LogBatch;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.models.logs.LogResponse;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.services.endpoints.LogBatchEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.Storable;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.DateUtils;
import com.appambit.sdk.utils.PackageInfoHelper;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public class Crashes {

    static Storable mStorable = ServiceLocator.getStorageService();
    static ExecutorService mExecutorService = ServiceLocator.getExecutorService();
    static ApiService apiService = ServiceLocator.getApiService();
    private static final Semaphore ensureFileLocked = new Semaphore(1);

    private static final String TAG = Crashes.class.getSimpleName();

    public static void sendBatchesLogs() {
        mExecutorService.execute(() -> {
            try {
                List<LogEntity> logs = mStorable.getOldest100Logs();
                if (logs.isEmpty()) {
                    Log.d(TAG, "No logs to send");
                    return;
                }
                LogBatch logBatch = new LogBatch();
                logBatch.setLogs(logs);
                LogBatchEndpoint logBatchEndpoint = new LogBatchEndpoint(logBatch);
                ApiResult<LogResponse> logResponse = apiService.executeRequest(logBatchEndpoint, LogResponse.class);

                if(logResponse.errorType != ApiErrorType.None) {
                    Log.d(TAG, "Error sending logs: " + logResponse.errorType);
                }else {
                    Log.d(TAG, "Logs sent successfully: " + logResponse.data.getMessage());
                    if (!logs.isEmpty()) {
                        AppAmbitTaskFuture<Void> deleteFuture = deleteLogs(logs);
                        deleteFuture.complete(null);
                        deleteFuture.then(result -> Log.d(TAG, "Logs deleted successfully"));
                        deleteFuture.onError(error -> Log.e(TAG, "Error deleting logs", error));
                    }
                }

            } catch (Exception ex) {
                Log.e(TAG, "Error to process Logs", ex);
            }
        });
    }

    @NonNull
    private static AppAmbitTaskFuture<Void> deleteLogs(List<LogEntity> logs) {
        AppAmbitTaskFuture<Void> future = new AppAmbitTaskFuture<>();
        try {
            ServiceLocator.getStorageService().deleteLogList(logs);
            future.complete(null);
        } catch (Throwable t) {
            future.fail(t);
        }
        return future;
    }

    public static void loadCrashFileIfExists(Context context) {
        ServiceLocator.getExecutorService().execute(() -> {
            try {
                ensureFileLocked.acquire();
                if(!SessionManager.isSessionActivate()) {
                    return;
                }
                File crashDir = context.getFilesDir();
                File[] crashFiles = crashDir.listFiles((dir, name) -> name.startsWith("crash_") && name.endsWith(".json"));

                if (crashFiles == null || crashFiles.length == 0) {
                    CrashHandler.clearCrashFile(context);
                    return;
                }

                Log.d(TAG, "Debug Count of Crashes: " + crashFiles.length);
                CrashHandler.setCrashFlag(context, true);

                List<ExceptionInfo> exceptionInfos = new ArrayList<>();

                for (File file : crashFiles) {
                    String filePath = file.getAbsolutePath();
                    ExceptionInfo info = readCrashFile(filePath);
                    if (info != null) {
                        exceptionInfos.add(info);
                    }
                }

                if (exceptionInfos.size() == 1) {
                    if(!TextUtils.isDigitsOnly(exceptionInfos.get(0).getSessionId())) {
                        storeBatchCrashesLog(context, exceptionInfos);
                        Log.d(TAG, "Storing crash log as batch due to invalid session ID");
                        deleteCrashes(context);
                        return;
                    }
                    logCrash(context, exceptionInfos.get(0));
                    Log.d(TAG, "Sending one crash");
                    deleteCrashes(context);
                } else if (exceptionInfos.size() > 1) {
                    storeBatchCrashesLog(context, exceptionInfos);
                    Log.d(TAG, "Sending crash batch: " + exceptionInfos.size() + " items");
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Semaphore interrupted " + e);
            } finally {
                ensureFileLocked.release();
            }
        });
    }

    public static void storeBatchCrashesLog(Context context, @NonNull List<ExceptionInfo> crashList) {
        Log.d(TAG, "Debug Storing in DB Crashes Batches");
        AppAmbitTaskFuture<Void> appAmbitTaskFuture = new AppAmbitTaskFuture<>();
        for(ExceptionInfo crash : crashList) {
            try {
                LogEntity logEntity = mapExceptionInfoToLogEntity(context, crash);
                if (logEntity.getId() == null) {
                    logEntity.setId(UUID.randomUUID());
                }
                if (crash.getCreatedAt() == null) {
                    logEntity.setCreatedAt(DateUtils.getUtcNow());
                } else {
                    logEntity.setCreatedAt(crash.getCreatedAt());
                }
                if(logEntity.getSessionId() == null) {
                    logEntity.setSessionId(crash.getSessionId());
                }
                if (mStorable == null) {
                    return;
                }
                mStorable.putLogEvent(logEntity);
                Log.d(TAG, "Stored crash log");
            } catch (Exception e) {
                Log.d(TAG, "Debug exception: " + e);
                appAmbitTaskFuture.fail(e);
            }
        }
        appAmbitTaskFuture.complete(null);
        appAmbitTaskFuture.then(result -> {
            Log.d(TAG, "All crash logs stored successfully");
            deleteCrashes(context);
        });

        appAmbitTaskFuture.onError(error -> Log.e(TAG, "Error while storing crash logs", error));
    }

    public static void LogError(Context context, Exception exception, Map<String, String> properties, String classFqn, String fileName,int lineNumber, Date createdAt) {
        Logging.LogEvent(context, "", LogType.ERROR, exception, properties, classFqn, fileName, lineNumber, createdAt);
    }

    public static void LogError(Context context, String message, Map<String, String> properties, String classFqn, Exception exception, String fileName, int lineNumber, Date createdAt) {
        Logging.LogEvent(context, message, LogType.ERROR, exception, properties, classFqn, fileName, lineNumber, createdAt);
    }

    @NonNull
    private static LogEntity mapExceptionInfoToLogEntity(Context context, ExceptionInfo exception) {
        LogEntity entity = new LogEntity();
        PackageInfo pInfo = PackageInfoHelper.getPackageInfo(context);
        assert pInfo != null;
        entity.setAppVersion(pInfo.versionName + " (" + pInfo.versionCode + ")");
        entity.setClassFQN(exception != null && exception.getClassFullName() != null ? exception.getClassFullName() : AppConstants.UNKNOWN_CLASS);
        entity.setFileName(exception != null && exception.getFileNameFromStackTrace() != null ? exception.getFileNameFromStackTrace() : AppConstants.UNKNOWN_FILENAME);
        entity.setLineNumber(exception != null ? exception.getLineNumberFromStackTrace() : 0);
        entity.setMessage(exception != null && exception.getMessage() != null ? exception.getMessage() : "");
        entity.setStackTrace(exception != null ? exception.getStackTrace() : null);

        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("Source", exception != null && exception.getSource() != null ? exception.getSource() : "");
        contextMap.put("InnerException", exception != null && exception.getInnerException() != null ? exception.getInnerException() : "");
        entity.setContext(contextMap);

        entity.setType(LogType.CRASH);
        entity.setFile(exception != null ? exception.getCrashLogFile() : null);
        entity.setCreatedAt(exception != null ? exception.getCreatedAt() : null);

        return entity;
    }

    private static void logCrash(Context context, @NonNull ExceptionInfo exception)
    {
        var message = exception.getMessage();
        Logging.logEvent(context, message, LogType.CRASH, exception, null, exception.getClassFullName(),
                exception.getFileNameFromStackTrace(), (int) exception.getLineNumberFromStackTrace(), exception.getCreatedAt());
    }

    public static void deleteCrashes(@NonNull Context context) {
        File dir = context.getFilesDir();

        File[] crashFiles = dir.listFiles((dir1, name) ->
                name.startsWith("crash_") && name.endsWith(".json"));

        if (crashFiles == null) return;

        for (File crashFile : crashFiles) {
            if (crashFile.delete()) {
                Log.d(TAG, "Deleted crash file: " + crashFile.getName());
            } else {
                Log.d(TAG, "Failed to delete crash file: " + crashFile.getName());
            }
        }
        Log.d(TAG, "All crash files deleted");
    }

    @Nullable
    public static ExceptionInfo readCrashFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return null;

            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            return ExceptionInfo.fromJson(jsonObject);

        } catch (Exception e) {
            Log.d(TAG, "Error reading file " + e);
            return null;
        }
    }

    public static boolean didCrashInLastSession(Context context) {
        return CrashHandler.didCrashInLastSession(context);
    }

    public static void generateTestCrash() {
        throw new NullPointerException();
    }

}