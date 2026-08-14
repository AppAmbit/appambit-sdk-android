package com.appambit.sdk.services;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.util.Log;

import androidx.annotation.NonNull;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.models.logs.LogBatch;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.endpoints.CloudCodeEndpoint;
import com.appambit.sdk.services.endpoints.TokenEndpoint;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.SSLSocket;

/** Shared URLConnection-based transport for legacy API and Cloud Code requests. */
public final class HttpUrlConnectionTransport implements HttpTransport {
    private static final String TAG = HttpUrlConnectionTransport.class.getSimpleName();

    private final Context context;
    private final ExecutorService executor;
    private final HttpTransportCredentials credentials;

    public HttpUrlConnectionTransport(
            @NonNull Context context,
            @NonNull ExecutorService executor,
            @NonNull HttpTransportCredentials credentials) {
        this.context = context.getApplicationContext();
        this.executor = executor;
        this.credentials = credentials;
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
        if (endpoint instanceof CmsEndpoint) {
            return executeCms(endpoint, timeoutMillis);
        }
        return executeUrlConnection(endpoint, timeoutMillis);
    }

    private HttpTransportResponse executeUrlConnection(
            @NonNull IEndpoint endpoint,
            int timeoutMillis) {
        HttpURLConnection connection = null;
        try {
            String fullUrl = endpoint.getBaseUrl() + endpoint.getUrl();
            Object payload = endpoint.getPayload();
            if (isGet(endpoint) && payload != null && !(endpoint instanceof CloudCodeEndpoint)) {
                fullUrl = serializedGetUrl(fullUrl, payload);
            }

            Log.d(TAG, "Full URL: " + fullUrl);
            connection = (HttpURLConnection) new URL(fullUrl).openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setRequestMethod(endpoint.getMethod().name());
            configureHeaders(connection, endpoint, payload);

            boolean allowsLegacyBody = endpoint instanceof CloudCodeEndpoint
                    || endpoint.getMethod() != com.appambit.sdk.enums.HttpMethodEnum.DELETE;
            if (!isGet(endpoint) && payload != null && allowsLegacyBody) {
                connection.setDoOutput(true);
                writePayload(connection, endpoint, payload);
            }

            int statusCode = connection.getResponseCode();
            Log.d(TAG, "HTTP-Response-Header: " + statusCode + ": " + connection.getResponseMessage());
            InputStream input = statusCode >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            byte[] body = input == null ? null : readAllBytes(input);
            Log.d(TAG, "[HTTP-Response-Body] " + loggedResponseBody(endpoint, body));
            return new HttpTransportResponse(statusCode, body, flattenHeaders(connection.getHeaderFields()), null);
        } catch (Exception error) {
            Integer statusCode = null;
            Map<String, String> headers = null;
            if (connection != null) {
                try {
                    statusCode = connection.getResponseCode();
                    headers = flattenHeaders(connection.getHeaderFields());
                } catch (Exception ignored) {
                    // Preserve the original transport failure.
                }
            }
            return new HttpTransportResponse(statusCode, null, headers, error);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String loggedResponseBody(@NonNull IEndpoint endpoint, byte[] body) {
        if (endpoint instanceof TokenEndpoint) return "[redacted token response]";
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private void configureHeaders(
            @NonNull HttpURLConnection connection,
            @NonNull IEndpoint endpoint,
            Object payload) {
        boolean multipart = isMultipart(payload);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", multipart
                ? "multipart/form-data; boundary=" + boundaryFor(connection)
                : "application/json");

        if (endpoint.getCustomHeader() != null) {
            for (Map.Entry<String, String> header : endpoint.getCustomHeader().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (endpoint instanceof CmsEndpoint) {
            connection.setRequestProperty("X-App-Key", AppAmbit.getAppKey());
            return;
        }

        String token = credentials.getToken();
        if (token != null && !token.trim().isEmpty() && !endpoint.isSkipAuthorization()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    private void writePayload(
            @NonNull HttpURLConnection connection,
            @NonNull IEndpoint endpoint,
            Object payload) throws Exception {
        if (isMultipart(payload)) {
            String contentType = connection.getRequestProperty("Content-Type");
            String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
            try (OutputStream output = connection.getOutputStream();
                 DataOutputStream dataOutput = new DataOutputStream(output)) {
                MultipartFormData.getOutputString(payload, dataOutput, boundary, true);
            }
            return;
        }

        String json = endpoint instanceof CloudCodeEndpoint
                ? CloudCodeJson.encodeObject((Map<String, ?>) payload)
                : JsonConvertUtils.toJson(payload);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
    }

    private HttpTransportResponse executeCms(
            @NonNull IEndpoint endpoint,
            int timeoutMillis) {
        SSLSocket socket = null;
        try {
            URL parsed = new URL(endpoint.getBaseUrl() + endpoint.getUrl());
            String host = parsed.getHost();
            int port = parsed.getPort() != -1 ? parsed.getPort() : 443;
            String requestPath = parsed.getFile();

            @SuppressWarnings("deprecation")
            SSLCertificateSocketFactory sslFactory =
                    (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(timeoutMillis);
            socket = (SSLSocket) sslFactory.createSocket(host, port);
            sslFactory.setHostname(socket, host);
            socket.setSoTimeout(timeoutMillis);
            socket.startHandshake();

            String request = "GET " + requestPath + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Accept: application/json\r\n"
                    + "X-App-Key: " + AppAmbit.getAppKey() + "\r\n"
                    + "Connection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            byte[] raw = readAllBytes(socket.getInputStream());
            int bodyStart = headerBodyBoundary(raw);
            if (bodyStart < 0) return failure(new IOException("Malformed HTTP response"));

            String headerSection = new String(raw, 0, bodyStart - 4, StandardCharsets.UTF_8);
            byte[] body = Arrays.copyOfRange(raw, bodyStart, raw.length);
            int statusCode = statusCode(headerSection);
            if (headerSection.toLowerCase(Locale.US).contains("transfer-encoding: chunked")) {
                body = decodeChunkedBodyBytes(body);
            }
            Map<String, String> headers = parseHeaders(headerSection);
            return new HttpTransportResponse(statusCode, body, headers, null);
        } catch (Exception error) {
            return failure(error);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                    // Nothing else to do during cleanup.
                }
            }
        }
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
        return endpoint.getMethod() == com.appambit.sdk.enums.HttpMethodEnum.GET;
    }

    private static boolean isMultipart(Object payload) {
        return payload instanceof com.appambit.sdk.models.logs.Log
                || payload instanceof LogBatch || payload instanceof LogEntity;
    }

    private static String boundaryFor(HttpURLConnection connection) {
        return "*****" + System.currentTimeMillis() + "*****";
    }

    private static HttpTransportResponse failure(Throwable error) {
        return new HttpTransportResponse(null, null, null, error);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<>();
        if (headers == null) return result;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return result;
    }

    private static int headerBodyBoundary(byte[] raw) {
        for (int i = 0; i < raw.length - 3; i++) {
            if (raw[i] == '\r' && raw[i + 1] == '\n' && raw[i + 2] == '\r' && raw[i + 3] == '\n') {
                return i + 4;
            }
        }
        return -1;
    }

    private static int statusCode(String headers) {
        String[] lines = headers.split("\r\n");
        if (lines.length > 0) {
            String[] parts = lines[0].split(" ", 3);
            if (parts.length >= 2) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // Fall through to the safe default.
                }
            }
        }
        return 200;
    }

    private static Map<String, String> parseHeaders(String headerSection) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = headerSection.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            int separator = lines[i].indexOf(':');
            if (separator > 0) {
                headers.put(lines[i].substring(0, separator), lines[i].substring(separator + 1).trim());
            }
        }
        return headers;
    }

    private static byte[] decodeChunkedBodyBytes(byte[] data) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int position = 0;
        while (position < data.length) {
            int lineEnd = position;
            while (lineEnd + 1 < data.length && !(data[lineEnd] == '\r' && data[lineEnd + 1] == '\n')) {
                lineEnd++;
            }
            if (lineEnd + 1 >= data.length) break;
            int chunkSize;
            try {
                String sizeLine = new String(data, position, lineEnd - position, StandardCharsets.US_ASCII).trim();
                int extension = sizeLine.indexOf(';');
                if (extension >= 0) sizeLine = sizeLine.substring(0, extension);
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException error) {
                break;
            }
            if (chunkSize == 0) break;
            position = lineEnd + 2;
            if (position + chunkSize > data.length) break;
            result.write(data, position, chunkSize);
            position += chunkSize + 2;
        }
        return result.toByteArray();
    }
}
