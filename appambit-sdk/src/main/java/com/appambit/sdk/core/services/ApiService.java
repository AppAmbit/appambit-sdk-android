package com.appambit.sdk.core.services;

import static com.appambit.sdk.core.utils.JsonDeserializer.deserializeFromJSONStringContent;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import com.appambit.sdk.core.enums.ApiErrorType;
import com.appambit.sdk.core.models.Consumer;
import com.appambit.sdk.core.models.logs.LogBatch;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.models.responses.ApiResult;
import com.appambit.sdk.core.models.responses.TokenResponse;
import com.appambit.sdk.core.services.ExceptionsCustom.HttpRequestException;
import com.appambit.sdk.core.services.ExceptionsCustom.UnauthorizedException;
import com.appambit.sdk.core.services.endpoints.RegisterEndpoint;
import com.appambit.sdk.core.services.interfaces.IEndpoint;
import com.appambit.sdk.core.utils.AppAmbitTaskFuture;
import com.appambit.sdk.core.utils.JsonKey;
import com.appambit.sdk.core.utils.MultipartFormData;
import org.json.JSONObject;
import java.io.BufferedReader;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ApiService {

    private final Context context;
    private static String _token;
    private ApiErrorType currentTokenRenewalTask;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ApiService(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public <T> ApiResult<T> executeRequest(IEndpoint endpoint, Class<T> clazz) {

        if (!hasInternetConnection()) {
            Log.d("[APIService]", "No internet connection available.");
            return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet available");
        }

        try {
            HttpURLConnection httpResponse = requestHttp(endpoint);
            checkStatusCodeFrom(httpResponse.getResponseCode());
            Log.d("[APIService]", "Request successful: " + httpResponse.getResponseCode());
            Log.d("[HTTP-Response]", "Message: " + httpResponse.getResponseMessage());

            Map<String, List<String>> responseHeaders = httpResponse.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                Log.d("[HTTP-Response-Header]", entry.getKey() + ": " + entry.getValue());
            }

            Object payload = endpoint.getPayload();
            if (payload instanceof JSONObject) {
                Log.d("[HTTP-Request-Body]", payload.toString());
            } else if (payload != null) {
                JSONObject json = serializeToJSONStringContent(payload);
                Log.d("[HTTP-Request-Body]", json.toString());
            } else {
                Log.d("[HTTP-Request-Body]", "No payload");
            }

            InputStream is = (httpResponse.getResponseCode() >= 400)
                    ? httpResponse.getErrorStream()
                    : httpResponse.getInputStream();

            StringBuilder responseBuilder = new StringBuilder();
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
            }

            String json = responseBuilder.toString();
            T response = deserializeFromJSONStringContent(new JSONObject(json), clazz);
            Log.d("[HTTP-Response-Body]", json);

            return ApiResult.success(response);
        }catch (UnauthorizedException unauthorizedException) {

            if (endpoint instanceof RegisterEndpoint) {
                Log.d("[APIService]", "Token renew endpoint also failed. Session and Token must be cleared");
                ClearToken();
                return null;
            }
            if (!IsRenewingToken()) {
                try {
                    Log.d("[APIService]", "Token invalid - triggering renewal");

                    AppAmbitTaskFuture<ApiErrorType> currentTokenRenewalTask = GetNewToken("");
                    currentTokenRenewalTask.then(result -> {
                        if (!IsRenewSuccess(result)) {
                            HandleFailedRenewalResult(result);
                        }
                    });
                    currentTokenRenewalTask.onError(error ->
                            Log.d("[APIService]", "Error during token renewal: " + error));
                } catch (Exception e) {
                    return HandleTokenRenewalException(e);
                } finally {
                    currentTokenRenewalTask = null;
                }
            }
            Log.d("[APIService]", "Retrying request after token renewal");
            return executeRequest(endpoint, clazz);
        } catch (Exception e) {
            Log.d("[APIService]", "Exception during request: "+e);
            return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error during request");
        }
    }

    private boolean IsRenewingToken()
    {
        return currentTokenRenewalTask != null;
    }

    private boolean IsRenewSuccess(ApiErrorType result)
    {
        return result == ApiErrorType.None;
    }

    @NonNull
    private <T> ApiResult<T> HandleTokenRenewalException(Exception ex) {
        Log.d("[APIService]", "Error while renewing token: "+ex);
        ClearToken();
        return ApiResult.fail(ApiErrorType.Unknown, "Unexpected error during token renewal");
    }

    @NonNull
    private <T> ApiResult<T> HandleFailedRenewalResult(ApiErrorType result)
    {
        if (result == ApiErrorType.NetworkUnavailable)
        {
            Log.d("[APIService]", "Cannot retry request: no internet after token renewal");
            return ApiResult.fail(ApiErrorType.NetworkUnavailable, "No internet after token renewal");
        }

        Log.d("[APIService]", "Could not renew token. Cleaning up");
        return ApiResult.fail(result, "Token renewal failed");
    }

    public AppAmbitTaskFuture<ApiErrorType> GetNewToken(String appKey) {
        AppAmbitTaskFuture<ApiErrorType> newTokenFuture = new AppAmbitTaskFuture<>();
        executor.execute(() -> {
            try {
                Consumer consumer = new Consumer();
                consumer.setAppKey(appKey);
                consumer.setDeviceId("c33f5f41-3888-378b-af44-83978c02cb71");
                consumer.setDeviceModel("Google Pixel 8");
                consumer.setUserId("12");
                consumer.setUserEmail("lea60@weber.net");
                consumer.setOs("14.0");
                consumer.setCountry("US");
                consumer.setLanguage("en");

                RegisterEndpoint registerEndpoint = new RegisterEndpoint(consumer);
                ApiResult<TokenResponse> tokenResponse = executeRequest(registerEndpoint, TokenResponse.class);

                Log.d("[APIService]", "Token renew response [type]: " + (tokenResponse != null ? tokenResponse.errorType : "null"));

                if (tokenResponse == null) {
                    newTokenFuture.complete(ApiErrorType.Unknown);
                    return;
                }
                if (tokenResponse.errorType == ApiErrorType.NetworkUnavailable) {
                    newTokenFuture.complete(ApiErrorType.NetworkUnavailable);
                    return;
                }
                if (tokenResponse.errorType == ApiErrorType.None) {
                    _token = tokenResponse.data.getToken();
                    Log.d("[APIService]", "Token renew response [token]: " + _token);
                    newTokenFuture.complete(ApiErrorType.None);
                    return;
                }
                newTokenFuture.complete(tokenResponse.errorType);
            } catch (Exception e) {
                Log.d("[APIService]", "Exception during token renew attempt: " + e);
                newTokenFuture.fail(e);
            }
        });
        return newTokenFuture;
    }

    private void ClearToken() {
        Log.d("[APIService]", "Session is no longer valid. Clearing token.");
        _token = null;
    }

    public static String getToken() {
        return _token;
    }

    public void setToken(String token) {
        _token = token;
    }

    public static void checkStatusCodeFrom(int statusCode) throws UnauthorizedException, HttpRequestException {
        if (statusCode > 199 && statusCode < 300) {
            return;
        }
        if (statusCode == 401) {
            throw new UnauthorizedException();
        }
        throw new HttpRequestException("HTTP error " + statusCode);
    }

    @NonNull
    public static HttpURLConnection requestHttp(@NonNull IEndpoint endpoint) throws Exception {
        String fullUrl = endpoint.getBaseUrl() + endpoint.getUrl();
        Object payload = endpoint.getPayload();

        if ("GET".equals(endpoint.getMethod().name()) && payload != null) {
            fullUrl = serializedGetUrl(fullUrl, payload);
        }

        Log.d("[APIService]", "Full URL: " + fullUrl);
        URL url = new URL(fullUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout((int) TimeUnit.MINUTES.toMillis(2));
        connection.setReadTimeout((int) TimeUnit.MINUTES.toMillis(2));
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
                        DataOutputStream multipartStream = new DataOutputStream(os);
                        MultipartFormData.getOutputString(payload, multipartStream, boundary, 0, true);
                        multipartStream.flush();
                        multipartStream.close();

                    }catch (Exception e) {
                        Log.e("[APIService]", "Error during multipart serialization: " + e.getMessage());
                        throw new IOException("Error during multipart serialization", e);
                    }

                } else {
                    JSONObject json = serializeToJSONStringContent(payload);
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                    os.close();
                }
            }
        }
        return connection;
    }

    private static void addAuthorizationHeaderIfNeeded(HttpURLConnection connection) {
        String token = getToken();
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    public static JSONObject serializeToJSONStringContent(Object payload) {
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

    private boolean hasInternetConnection() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if(networkInfo == null) {
            return false;
        }
        return networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                networkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
    }
}