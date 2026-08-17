package com.appambit.sdk.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.TokenResponse;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.endpoints.RegisterEndpoint;
import com.appambit.sdk.services.endpoints.TokenEndpoint;
import com.appambit.sdk.services.exceptionsCustom.HttpRequestException;
import com.appambit.sdk.services.exceptionsCustom.UnauthorizedException;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.HttpTransport;
import com.appambit.sdk.services.interfaces.HttpTransportCredentials;
import com.appambit.sdk.services.interfaces.HttpTransportResponse;
import com.appambit.sdk.services.interfaces.IEndpoint;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.JsonDeserializer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

public class HttpApiService implements ApiService, HttpTransport, HttpTransportCredentials {
    private static final String TAG = HttpApiService.class.getSimpleName();
    private static final int SDK_REQUEST_TIMEOUT_MILLIS = 20_000;

    private volatile String token;

    private final ExecutorService executor;
    private final HttpTransport transport;
    private final HttpTransport rawTransport;
    private final ReentrantLock tokenLock = new ReentrantLock();
    private long tokenGeneration;
    private volatile RenewalOperation currentRenewal;

    private static final class RenewalOperation {
        private final long generation;
        private final AppAmbitTaskFuture<ApiErrorType> future = new AppAmbitTaskFuture<>();
        private final CountDownLatch completed = new CountDownLatch(1);
        private volatile ApiErrorType result;

        private RenewalOperation(long generation) {
            this.generation = generation;
        }

        private void complete(ApiErrorType value) {
            result = value;
            future.complete(value);
            completed.countDown();
        }

        private void fail(Throwable error) {
            future.fail(error);
            completed.countDown();
        }

        private ApiErrorType await() throws InterruptedException {
            completed.await();
            return result == null ? ApiErrorType.Unknown : result;
        }
    }

    private static final class RenewalSelection {
        private final RenewalOperation operation;
        private final boolean owner;

        private RenewalSelection(RenewalOperation operation, boolean owner) {
            this.operation = operation;
            this.owner = owner;
        }
    }

    public HttpApiService(@NonNull Context context, ExecutorService executor) {
        this(context, executor, executor);
    }

    public HttpApiService(
            @NonNull Context context,
            ExecutorService executor,
            ExecutorService rawExecutor) {
        this.executor = executor;
        this.transport = new HttpUrlConnectionTransport(context, executor, this);
        this.rawTransport = rawExecutor == executor
                ? this.transport
                : new HttpUrlConnectionTransport(context, rawExecutor, this);
    }

    @Override
    public <T> ApiResult<T> executeRequest(IEndpoint endpoint, Class<T> clazz) {
        return executeRequest(endpoint, clazz, true);
    }

    private <T> ApiResult<T> executeRequest(
            IEndpoint endpoint,
            Class<T> clazz,
            boolean allowTokenRenewal) {
        if (allowTokenRenewal && requiresConsumerToken(endpoint) && isTokenMissing()) {
            ApiErrorType renewalResult = renewTokenForRetry();
            if (renewalResult != ApiErrorType.None) {
                return handleFailedRenewalResult(renewalResult);
            }
        }

        int timeout = SDK_REQUEST_TIMEOUT_MILLIS;
        HttpTransportResponse raw = transport.executeBlocking(endpoint, timeout);
        if (raw == null) {
            return ApiResult.fail(ApiErrorType.Unknown, "Empty HTTP response");
        }
        if (raw.getError() != null) {
            if (isNetworkError(raw.getError())) {
                return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet available");
            }
            Log.e(TAG, "HTTP request failed", raw.getError());
            return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error");
        }

        int statusCode = raw.getStatusCode() == null ? 0 : raw.getStatusCode();
        try {
            checkStatusCodeFrom(statusCode);
        } catch (UnauthorizedException unauthorized) {
            if (endpoint instanceof RegisterEndpoint
                    || endpoint instanceof TokenEndpoint
                    || endpoint instanceof CmsEndpoint
                    || !allowTokenRenewal
                    || !isRetryableAfterUnauthorized(endpoint)) {
                return ApiResult.fail(ApiErrorType.Unauthorized,
                        endpoint instanceof CmsEndpoint
                                ? "CMS request unauthorized"
                                : endpoint instanceof RegisterEndpoint || endpoint instanceof TokenEndpoint
                                ? "Authentication endpoint returned 401"
                                : "Request returned 401");
            }

            Log.w(TAG, "401 Unauthorized. Need to renew token.");
            ApiErrorType renewalResult = renewTokenForRetry();
            if (renewalResult != ApiErrorType.None) {
                return handleFailedRenewalResult(renewalResult);
            }
            return executeRequest(endpoint, clazz, false);
        } catch (HttpRequestException error) {
            return ApiResult.fail(ApiErrorType.Unknown, error.getMessage());
        }

        String body = raw.getBody() == null
                ? ""
                : new String(raw.getBody(), StandardCharsets.UTF_8);
        if (endpoint instanceof CmsEndpoint) {
            @SuppressWarnings("unchecked")
            T response = (T) body;
            return ApiResult.success(response);
        }

        try {
            return ApiResult.success(JsonDeserializer.deserializeFromJSONResponse(body, clazz));
        } catch (Exception error) {
            Log.e(TAG, "Unable to decode HTTP response", error);
            return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error");
        }
    }

    private static boolean isNetworkError(Throwable error) {
        return error instanceof IOException
                || error.getMessage() != null && error.getMessage().contains("No internet connection");
    }

    private boolean isTokenMissing() {
        String value = getToken();
        return value == null || value.trim().isEmpty();
    }

    private static boolean requiresConsumerToken(IEndpoint endpoint) {
        return !(endpoint instanceof RegisterEndpoint)
                && !(endpoint instanceof TokenEndpoint)
                && !(endpoint instanceof CmsEndpoint)
                && !endpoint.isSkipAuthorization();
    }

    private static boolean isRetryableAfterUnauthorized(IEndpoint endpoint) {
        return endpoint.getMethod() == com.appambit.sdk.enums.HttpMethodEnum.GET;
    }

    private static void checkStatusCodeFrom(int statusCode)
            throws UnauthorizedException, HttpRequestException {
        if (statusCode > 199 && statusCode < 300) return;
        if (statusCode == 401) throw new UnauthorizedException();
        throw new HttpRequestException("HTTP error " + statusCode);
    }

    private <T> ApiResult<T> handleFailedRenewalResult(ApiErrorType result) {
        if (result == ApiErrorType.NetworkUnavailable) {
            Log.w(TAG, "Cannot retry request: no internet after token renewal");
            return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet after token renewal");
        }
        Log.w(TAG, "Could not renew token. Cleaning up");
        clearToken();
        return ApiResult.fail(result, "Token renewal failed");
    }

    private ApiErrorType renewTokenForRetry() {
        RenewalSelection selection = selectRenewal();
        RenewalOperation operation = selection.operation;
        if (!selection.owner) {
            try {
                // executeRequest has a synchronous return type; this narrow wait coordinates
                // concurrent refresh callers without using the public blocking helper.
                return operation.await();
            } catch (Exception error) {
                return ApiErrorType.Unknown;
            }
        }

        try {
            ApiErrorType result = renewToken(operation.generation);
            operation.complete(result);
            return result;
        } catch (Throwable error) {
            operation.fail(error);
            return ApiErrorType.Unknown;
        } finally {
            clearRenewal(operation);
        }
    }

    private RenewalSelection selectRenewal() {
        tokenLock.lock();
        try {
            RenewalOperation existing = currentRenewal;
            if (existing != null && existing.generation == tokenGeneration) {
                return new RenewalSelection(existing, false);
            }

            RenewalOperation created = new RenewalOperation(tokenGeneration);
            currentRenewal = created;
            return new RenewalSelection(created, true);
        } finally {
            tokenLock.unlock();
        }
    }

    private void clearRenewal(RenewalOperation operation) {
        tokenLock.lock();
        try {
            if (currentRenewal == operation) {
                currentRenewal = null;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private void clearToken() {
        setToken(null);
    }

    @Override
    public AppAmbitTaskFuture<ApiErrorType> GetNewToken() {
        RenewalSelection selection = selectRenewal();
        RenewalOperation operation = selection.operation;
        if (!selection.owner) return operation.future;

        try {
            executor.execute(() -> {
                try {
                    operation.complete(renewToken(operation.generation));
                } catch (Throwable error) {
                    operation.fail(error);
                } finally {
                    clearRenewal(operation);
                }
            });
        } catch (Throwable error) {
            clearRenewal(operation);
            operation.future.fail(error);
        }
        return operation.future;
    }

    private ApiErrorType renewToken(long renewalGeneration) {
        try {
            TokenEndpoint endpoint = TokenService.createTokenendpoint();
            ApiResult<TokenResponse> response = executeRequest(endpoint, TokenResponse.class, false);
            tokenLock.lock();
            try {
                if (tokenGeneration != renewalGeneration) {
                    Log.w(TAG, "Discarding stale token refresh result");
                    return ApiErrorType.Unknown;
                }

                if (response != null
                        && response.errorType == ApiErrorType.None
                        && response.data != null
                        && response.data.getToken() != null
                        && !response.data.getToken().trim().isEmpty()) {
                    token = response.data.getToken();
                    Log.d(TAG, "Token renewed successfully");
                    return ApiErrorType.None;
                }
                token = null;
                tokenGeneration++;
                return response == null ? ApiErrorType.Unknown : response.errorType;
            } finally {
                tokenLock.unlock();
            }
        } catch (Exception error) {
            tokenLock.lock();
            try {
                if (tokenGeneration == renewalGeneration) {
                    token = null;
                    tokenGeneration++;
                }
            } finally {
                tokenLock.unlock();
            }
            return ApiErrorType.Unknown;
        }
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void setToken(String value) {
        tokenLock.lock();
        try {
            if (value == null || value.trim().isEmpty()) {
                token = null;
                tokenGeneration++;
                Log.d(TAG, "Token invalidated locally");
            } else {
                token = value;
                tokenGeneration++;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    @Override
    public HttpTransportResponse executeBlocking(
            @NonNull IEndpoint endpoint,
            int timeoutMillis) {
        return transport.executeBlocking(endpoint, timeoutMillis);
    }

    @Override
    public void executeRaw(
            @NonNull IEndpoint endpoint,
            int timeoutMillis,
            @NonNull HttpTransport.Callback callback) {
        long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        if (requiresConsumerToken(endpoint) && isTokenMissing()) {
            Log.d(TAG, "Raw request requires token refresh before sending: " + endpoint.getUrl());
            requestTokenThenExecute(endpoint, deadlineNanos, callback);
            return;
        }

        executeRawWithExistingToken(endpoint, deadlineNanos, callback, true);
    }

    private void executeRawWithExistingToken(
            @NonNull IEndpoint endpoint,
            long deadlineNanos,
            @NonNull HttpTransport.Callback callback,
            boolean allowTokenRenewal) {
        int timeoutMillis;
        try {
            timeoutMillis = remainingTimeoutMillis(deadlineNanos);
        } catch (SocketTimeoutException timeout) {
            callback.onComplete(new HttpTransportResponse(null, null, null, timeout));
            return;
        }

        rawTransport.executeRaw(endpoint, timeoutMillis, response -> {
            if (!allowTokenRenewal
                    || response.getStatusCode() == null
                    || response.getStatusCode() != 401
                    || !isRetryableAfterUnauthorized(endpoint)) {
                callback.onComplete(response);
                return;
            }

            Log.w(TAG, "Raw request returned 401; refreshing token for one retry: " + endpoint.getUrl());
            AppAmbitTaskFuture<ApiErrorType> renewal = GetNewToken();
            renewal.then(result -> {
                if (result == ApiErrorType.None) {
                    Log.d(TAG, "Retrying raw request after token refresh: " + endpoint.getUrl());
                    executeRawWithExistingToken(endpoint, deadlineNanos, callback, false);
                } else {
                    callback.onComplete(response);
                }
            });
            renewal.onError(error -> callback.onComplete(response));
        });
    }

    private void requestTokenThenExecute(
            @NonNull IEndpoint endpoint,
            long deadlineNanos,
            @NonNull HttpTransport.Callback callback) {
        Log.d(TAG, "Waiting for token refresh before raw request: " + endpoint.getUrl());
        AppAmbitTaskFuture<ApiErrorType> renewal = GetNewToken();
        renewal.then(result -> {
            Log.d(TAG, "Raw request token refresh completed: " + result);
            if (result == ApiErrorType.None) {
                executeRawWithExistingToken(endpoint, deadlineNanos, callback, true);
            } else {
                callback.onComplete(new HttpTransportResponse(
                        null, null, null,
                        new IOException("Consumer token unavailable")));
            }
        });
        renewal.onError(error -> callback.onComplete(new HttpTransportResponse(
                null, null, null, error)));
    }

    private static int remainingTimeoutMillis(long deadlineNanos) throws SocketTimeoutException {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) throw new SocketTimeoutException("HTTP request timed out");
        long remainingMillis = (remainingNanos + 999_999L) / 1_000_000L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, remainingMillis));
    }
}
