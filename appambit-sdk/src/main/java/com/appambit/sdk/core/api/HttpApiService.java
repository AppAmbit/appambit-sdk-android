package com.appambit.sdk.core.api;

import static com.appambit.sdk.core.utils.InternetConnection.hasInternetConnection;
import static com.appambit.sdk.core.utils.JsonDeserializer.deserializeFromJSONStringContent;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.models.logs.LogBatch;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.models.responses.TokenResponse;
import com.appambit.sdk.core.api.exceptionsCustom.HttpRequestException;
import com.appambit.sdk.core.api.exceptionsCustom.UnauthorizedException;
import com.appambit.sdk.core.api.endpoints.RegisterEndpoint;
import com.appambit.sdk.core.api.interfaces.ApiService;
import com.appambit.sdk.core.api.interfaces.IEndpoint;
import com.appambit.sdk.core.services.ConsumerService;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.JsonConvertUtils;
import com.appambit.sdk.core.utils.JsonKey;
import com.appambit.sdk.core.utils.MultipartFormData;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class HttpApiService implements ApiService {

    private final Context context;
    private static String _token;
    private static String TAG = HttpApiService.class.getSimpleName();
    private final ExecutorService mExecutor;
    private final ReentrantLock tokenLock = new ReentrantLock();
    private volatile AppAmbitTaskFuture<ApiErrorType> currentRenewalFuture = null;
    public HttpApiService(@NonNull Context context, ExecutorService executor) {
        this.context = context.getApplicationContext();
        mExecutor = executor;
    }

    public <T> ApiResult<T> executeRequest(IEndpoint endpoint, Class<T> clazz) {

        if (!hasInternetConnection(context)) {
            Log.d(TAG, "No internet connection available.");
            return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet available");
        }

        try {
            HttpURLConnection httpResponse = requestHttp(endpoint);
            Log.d(TAG, "HTTP-Response-Header: " + httpResponse.getResponseCode()  + ": " + httpResponse.getResponseMessage());

            Map<String, List<String>> responseHeaders = httpResponse.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                Log.d(TAG, entry.getKey() + ": " + entry.getValue());
            }

            InputStream is = (httpResponse.getResponseCode() >= 400)
                    ? httpResponse.getErrorStream()
                    : httpResponse.getInputStream();

            StringBuilder responseBuilder = new StringBuilder();

            if (is == null) {
                Log.e("[APIService]", "InputStream is null. Possibly no response body.");
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                } catch (IOException e) {
                    Log.e("[APIService]", "Error reading response: " + e.getMessage(), e);
                    return ApiResult.fail(ApiErrorType.Unknown, "Failed to read response");
                }
            }

            String json = responseBuilder.toString();
            checkStatusCodeFrom(httpResponse.getResponseCode());
            T response = deserializeFromJSONStringContent(new JSONObject(json), clazz);
            Log.d("[HTTP-Response-Body]", json);
            checkStatusCodeFrom(httpResponse.getResponseCode());

            return ApiResult.success(response);

        } catch (UnauthorizedException ex) {
            if (endpoint instanceof RegisterEndpoint) {
                clearToken();
                return ApiResult.fail(ApiErrorType.Unauthorized, "Register failed");
            }

            Log.w(TAG, "401 Unauthorized. Need to renew token.");

            if (!isRenewingToken()) {
                try {
                    currentRenewalFuture = new AppAmbitTaskFuture<>();
                    Log.d(TAG, "Token invalid - triggering renewal");
                    ApiErrorType renewalResult = renewToken(null, currentRenewalFuture);

                    if (!isRenewSuccess(renewalResult)) {
                        return handleFailedRenewalResult(clazz, renewalResult);
                    }
                } catch (Exception e) {
                    return handleTokenRenewalException(clazz, e);
                } finally {
                    currentRenewalFuture = null;
                }

            }

            Log.d(TAG, "Retrying request after token renewal");
            return executeRequest(endpoint, clazz);
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
            return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error");
        }

    }

    private boolean isRenewingToken() {
        return currentRenewalFuture != null;
    }

    private boolean isRenewSuccess(ApiErrorType result) {
        return result == ApiErrorType.None;
    }

    private <T> ApiResult<T> handleTokenRenewalException(Class<T> clazz, Exception ex) {
        Log.e(TAG, "Error while renewing token: " + ex);
        clearToken();
        return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error during token renewal");
    }

    private <T> ApiResult<T> handleFailedRenewalResult(Class<T> clazz, ApiErrorType result) {
        if (result == ApiErrorType.NetworkUnavailable) {
            Log.w(TAG, "Cannot retry request: no internet after token renewal");
            return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet after token renewal");
        }

        Log.w(TAG, "Could not renew token. Cleaning up");
        clearToken();
        return ApiResult.fail(result, "Token renewal failed");
    }

    private void clearToken() {
        _token = null;
    }

    public AppAmbitTaskFuture<ApiErrorType> GetNewToken(String appKey) {
        AppAmbitTaskFuture<ApiErrorType> newTokenFuture = new AppAmbitTaskFuture<>();
        mExecutor.execute(() -> {
            ApiErrorType result = renewToken(appKey, null);
            newTokenFuture.complete(result);
        });
        return newTokenFuture;
    }

    private ApiErrorType renewToken(String appKey, AppAmbitTaskFuture<ApiErrorType> asyncFuture) {
        try {
            RegisterEndpoint registerEndpoint = new ConsumerService().RegisterConsumer(appKey);
            ApiResult<TokenResponse> tokenResponse = executeRequest(registerEndpoint, TokenResponse.class);

            tokenLock.lock();
            try {
                if (tokenResponse != null && tokenResponse.errorType == ApiErrorType.None) {
                    _token = tokenResponse.data.getToken();
                    Log.d(TAG, "Token renewed successfully");

                    if (asyncFuture != null) {
                        asyncFuture.complete(null);
                    }
                    return ApiErrorType.None;
                } else {
                    clearToken();
                    Log.e(TAG, "Token renewal failed");

                    if (asyncFuture != null) {
                        asyncFuture.fail(new Exception("Token renewal failed"));
                    }
                    return tokenResponse != null ? tokenResponse.errorType : ApiErrorType.Unknown;
                }
            } finally {
                if (asyncFuture != null) {
                    currentRenewalFuture = null;
                }
                tokenLock.unlock();
            }
        } catch (Exception e) {
            tokenLock.lock();
            try {
                clearToken();
                Log.e(TAG, "Token renewal error", e);

                if (asyncFuture != null) {
                    asyncFuture.fail(e);
                }
                return ApiErrorType.Unknown;
            } finally {
                if (asyncFuture != null) {
                    currentRenewalFuture = null;
                }
                tokenLock.unlock();
            }
        }
    }

    public String getToken() {
        return _token;
    }

    public void setToken(String token) {
        _token = token;
    }

    private static void checkStatusCodeFrom(int statusCode) throws UnauthorizedException, HttpRequestException {
        if (statusCode > 199 && statusCode < 300) {
            return;
        }
        if (statusCode == 401) {
            throw new UnauthorizedException();
        }
        throw new HttpRequestException("HTTP error " + statusCode);
    }

    @NonNull
    public HttpURLConnection requestHttp(@NonNull IEndpoint endpoint) throws Exception {
        String fullUrl = endpoint.getBaseUrl() + endpoint.getUrl();
        Object payload = endpoint.getPayload();

        if ("GET".equals(endpoint.getMethod().name()) && payload != null) {
            fullUrl = serializedGetUrl(fullUrl, payload);
        }

        Log.d("[APIService]", "Full URL: " + fullUrl);
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
        connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));
        connection.setRequestMethod(endpoint.getMethod().name());

        boolean isMultipart = payload instanceof Log || payload instanceof LogBatch || payload instanceof LogEntity;

        connection.setRequestProperty("Accept", "application/json");
        if (isMultipart) {
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        } else {
            connection.setRequestProperty("Content-Type", "application/json");
        }

        addAuthorizationHeaderIfNeeded(connection);

        if (!"GET".equals(endpoint.getMethod().name()) && !"DELETE".equals(endpoint.getMethod().name())) {
            connection.setDoOutput(true);
            if (payload != null) {
                OutputStream os = connection.getOutputStream();

                if (isMultipart) {
                    try {
                        String boundary = connection.getRequestProperty("Content-Type").split("boundary=")[1];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream debugStream = new DataOutputStream(baos);
                        MultipartFormData.getOutputString(payload, debugStream, boundary, 0, true);
                        debugStream.flush();
                        debugStream.close();

                        String multipartBody = baos.toString(StandardCharsets.UTF_8.name());
                        Log.d("[HTTP-Request-Body]", "Multipart:\n" + multipartBody);


                        os.write(baos.toByteArray());
                        os.flush();
                        os.close();

                    }catch (Exception e) {
                        Log.e("[APIService]", "Error during multipart serialization: " + e.getMessage());
                        throw new IOException("Error during multipart serialization", e);
                    }

                } else {
                    String json = JsonConvertUtils.toJson(payload);
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                    os.close();
                }
            }
        }
        return connection;
    }

    private void addAuthorizationHeaderIfNeeded(HttpURLConnection connection) {
        String token = getToken();
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    private static JSONObject serializeToJSONStringContent(Object payload) {
        if (payload == null) {
            return new JSONObject();
        }

        JSONObject json = new JSONObject();
        Class<?> cls = payload.getClass();

        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String key = field.isAnnotationPresent(JsonKey.class)
                        ? Objects.requireNonNull(field.getAnnotation(JsonKey.class)).value()
                        : field.getName();

                Object value = field.get(payload);
                if (value != null) {
                    if (value instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) value;
                        JSONObject mapJson = new JSONObject();
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            mapJson.put(entry.getKey().toString(), entry.getValue().toString());
                        }
                        json.put(key, mapJson);
                    } else {
                        json.put(key, value);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return json;
    }

    private static String serializeStringPayload(Object payload) throws UnsupportedEncodingException, IllegalAccessException {
        if (payload == null) {
            return null;
        }

        StringBuilder serializedPayload = new StringBuilder();
        Field[] fields = payload.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(payload);
            if (value != null) {
                if (serializedPayload.length() > 0) {
                    serializedPayload.append("&");
                }
                serializedPayload.append(URLEncoder.encode(field.getName(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(value.toString(), "UTF-8"));
            }
        }
        return serializedPayload.toString();
    }

    private static String serializedGetUrl(String url, Object payload) throws UnsupportedEncodingException, IllegalAccessException {
        var serializedParameters = serializeStringPayload(payload);
        if (serializedParameters == null) {
            return url;
        }

        return url + "?" + serializedParameters;
    }
}