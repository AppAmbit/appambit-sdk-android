package com.appambit.sdk.services;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.logs.LogBatch;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.services.endpoints.CloudCodeEndpoint;
import com.appambit.sdk.services.endpoints.TokenEndpoint;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.interfaces.HttpTransport;
import com.appambit.sdk.services.interfaces.HttpTransportCredentials;
import com.appambit.sdk.services.interfaces.HttpTransportResponse;
import com.appambit.sdk.services.interfaces.IEndpoint;
import com.appambit.sdk.utils.CloudCodeJson;
import com.appambit.sdk.utils.JsonConvertUtils;
import com.appambit.sdk.utils.JsonKey;
import com.appambit.sdk.utils.MultipartFormData;
import com.appambit.sdk.utils.UrlQueryBuilder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Shared OkHttp transport for SDK, CMS and Cloud Code requests. */
public final class OkHttpTransport implements HttpTransport {
    private static final String TAG = OkHttpTransport.class.getSimpleName();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final String USER_AGENT = "AppAmbitSDK (Android)";

    private final Context context;
    private final ExecutorService executor;
    private final HttpTransportCredentials credentials;
    private final OkHttpClient client;

    public OkHttpTransport(
            @NonNull Context context,
            @NonNull ExecutorService executor,
            @NonNull HttpTransportCredentials credentials) {
        this.context = context.getApplicationContext();
        this.executor = executor;
        this.credentials = credentials;
        this.client = new OkHttpClient.Builder().build();
    }

    @Override
    public void executeRaw(
            @NonNull IEndpoint endpoint,
            int timeoutMillis,
            @NonNull Callback callback) {
        executor.execute(() -> callback.onComplete(executeBlocking(endpoint, timeoutMillis)));
    }

    @Override
    public HttpTransportResponse executeBlocking(
            @NonNull IEndpoint endpoint,
            int timeoutMillis) {
        if (!hasInternetConnection(context)) {
            return failure(new IOException("No internet connection"));
        }
        long deadlineNanos = deadlineNanos(timeoutMillis);
        return executeOkHttp(endpoint, deadlineNanos);
    }

    private HttpTransportResponse executeOkHttp(
            @NonNull IEndpoint endpoint,
            long deadlineNanos) {
        try {
            ensureDeadline(deadlineNanos);
            String fullUrl = endpoint.getBaseUrl() + endpoint.getUrl();
            Object payload = endpoint.getPayload();
            if (isGet(endpoint) && payload != null && !(endpoint instanceof CloudCodeEndpoint)) {
                fullUrl = serializedGetUrl(fullUrl, payload);
            }

            Log.d(TAG, "Full URL: " + fullUrl);
            RequestBody requestBody = requestBody(endpoint, payload);
            Request.Builder requestBuilder = new Request.Builder().url(fullUrl);
            configureHeaders(requestBuilder, endpoint, requestBody);
            Request request = requestBuilder
                    .method(endpoint.getMethod().name(), requestBody)
                    .build();

            int remainingTimeoutMillis = remainingTimeoutMillis(deadlineNanos);
            OkHttpClient requestClient = client.newBuilder()
                    .connectTimeout(remainingTimeoutMillis, TimeUnit.MILLISECONDS)
                    .readTimeout(remainingTimeoutMillis, TimeUnit.MILLISECONDS)
                    .writeTimeout(remainingTimeoutMillis, TimeUnit.MILLISECONDS)
                    .callTimeout(remainingTimeoutMillis, TimeUnit.MILLISECONDS)
                    .build();

            try (Response response = requestClient.newCall(request).execute()) {
                byte[] body = response.body() == null ? null : response.body().bytes();
                int statusCode = response.code();
                Log.d(TAG, "HTTP-Response-Header: " + statusCode + ": " + response.message());
                Log.d(TAG, "[HTTP-Response-Body] " + loggedResponseBody(endpoint, body));
                return new HttpTransportResponse(statusCode, body, flattenHeaders(response.headers()), null);
            }
        } catch (Exception error) {
            return failure(error);
        }
    }

    private static String loggedResponseBody(@NonNull IEndpoint endpoint, byte[] body) {
        if (endpoint instanceof TokenEndpoint) return "[redacted token response]";
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static RequestBody requestBody(
            @NonNull IEndpoint endpoint,
            Object payload) throws Exception {
        if (isGet(endpoint)) return null;
        if (payload == null) {
            return requiresRequestBody(endpoint.getMethod())
                    ? RequestBody.create(JSON_MEDIA_TYPE, new byte[0])
                    : null;
        }

        if (isMultipart(payload)) {
            String boundary = boundary();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (DataOutputStream dataOutput = new DataOutputStream(output)) {
                MultipartFormData.getOutputString(payload, dataOutput, boundary, true);
            }
            MediaType multipartType = MediaType.parse("multipart/form-data; boundary=" + boundary);
            return RequestBody.create(multipartType, output.toByteArray());
        }

        String json = endpoint instanceof CloudCodeEndpoint
                ? CloudCodeJson.encodeObject((Map<String, ?>) payload)
                : JsonConvertUtils.toJson(payload);
        return RequestBody.create(JSON_MEDIA_TYPE, json.getBytes(StandardCharsets.UTF_8));
    }

    private void configureHeaders(
            @NonNull Request.Builder requestBuilder,
            @NonNull IEndpoint endpoint,
            RequestBody requestBody) {
        requestBuilder.header("Accept", "application/json");
        requestBuilder.header("User-Agent", USER_AGENT);
        if (requestBody != null && requestBody.contentType() != null) {
            requestBuilder.header("Content-Type", requestBody.contentType().toString());
        }

        if (endpoint.getCustomHeader() != null) {
            for (Map.Entry<String, String> header : endpoint.getCustomHeader().entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
        }

        if (endpoint instanceof CmsEndpoint) {
            requestBuilder.header("X-App-Key", AppAmbit.getAppKey());
            return;
        }

        String token = credentials.getToken();
        if (token != null && !token.trim().isEmpty() && !endpoint.isSkipAuthorization()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
    }

    private static boolean requiresRequestBody(HttpMethodEnum method) {
        return method == HttpMethodEnum.POST
                || method == HttpMethodEnum.PUT
                || method == HttpMethodEnum.PATCH;
    }

    private static String serializedGetUrl(String baseUrl, Object payload)
            throws IllegalAccessException {
        Map<String, String> query = new java.util.LinkedHashMap<>();
        for (java.lang.reflect.Field field : payload.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            JsonKey annotation = field.getAnnotation(JsonKey.class);
            String key = annotation != null && !annotation.value().isEmpty()
                    ? annotation.value() : field.getName();
            Object value = field.get(payload);
            if (value == null) continue;
            String stringValue = value.toString().trim();
            if (!stringValue.isEmpty()) query.put(key, stringValue);
        }
        return UrlQueryBuilder.append(baseUrl, query);
    }

    private static boolean isGet(IEndpoint endpoint) {
        return endpoint.getMethod() == HttpMethodEnum.GET;
    }

    private static boolean isMultipart(Object payload) {
        return payload instanceof com.appambit.sdk.models.logs.Log
                || payload instanceof LogBatch || payload instanceof LogEntity;
    }

    private static String boundary() {
        return "*****" + System.currentTimeMillis() + "*****";
    }

    private static HttpTransportResponse failure(Throwable error) {
        return new HttpTransportResponse(null, null, null, error);
    }

    private static long deadlineNanos(int timeoutMillis) {
        return System.nanoTime() + timeoutMillis * 1_000_000L;
    }

    private static int remainingTimeoutMillis(long deadlineNanos) throws SocketTimeoutException {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) throw new SocketTimeoutException("HTTP request timed out");
        long remainingMillis = (remainingNanos + 999_999L) / 1_000_000L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, remainingMillis));
    }

    private static void ensureDeadline(long deadlineNanos) throws SocketTimeoutException {
        if (deadlineNanos - System.nanoTime() <= 0) {
            throw new SocketTimeoutException("HTTP request timed out");
        }
    }

    private static Map<String, String> flattenHeaders(Headers headers) {
        Map<String, String> result = new HashMap<>();
        for (String name : headers.names()) {
            List<String> values = headers.values(name);
            if (!values.isEmpty()) result.put(name, values.get(0));
        }
        return result;
    }
}
