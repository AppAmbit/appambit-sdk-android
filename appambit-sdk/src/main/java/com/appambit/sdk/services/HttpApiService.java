package com.appambit.sdk.services;

import static com.appambit.sdk.utils.InternetConnection.hasInternetConnection;
import static com.appambit.sdk.utils.JsonDeserializer.deserializeFromJSONResponse;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.appambit.sdk.AppAmbit;
import com.appambit.sdk.enums.ApiErrorType;
import com.appambit.sdk.models.logs.LogBatch;
import com.appambit.sdk.models.logs.LogEntity;
import com.appambit.sdk.models.responses.ApiResult;
import com.appambit.sdk.models.responses.TokenResponse;
import com.appambit.sdk.services.endpoints.CmsEndpoint;
import com.appambit.sdk.services.endpoints.TokenEndpoint;
import com.appambit.sdk.services.exceptionsCustom.HttpRequestException;
import com.appambit.sdk.services.exceptionsCustom.UnauthorizedException;
import com.appambit.sdk.services.endpoints.RegisterEndpoint;
import com.appambit.sdk.services.interfaces.ApiService;
import com.appambit.sdk.services.interfaces.IEndpoint;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.JsonConvertUtils;
import com.appambit.sdk.utils.JsonKey;
import com.appambit.sdk.utils.MultipartFormData;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import android.net.SSLCertificateSocketFactory;
import javax.net.ssl.SSLSocket;

public class HttpApiService implements ApiService {

    private final Context context;
    private static String _token;
    private static final String TAG = HttpApiService.class.getSimpleName();
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

        if (endpoint instanceof CmsEndpoint) {
            return executeCmsGetRequest(endpoint, clazz);
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
                Log.e(TAG, "InputStream is null. Possibly no response body.");
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading response: " + e.getMessage(), e);
                    return ApiResult.fail(ApiErrorType.Unknown, "Failed to read response");
                }
            }

            String json = responseBuilder.toString();
            Log.d(TAG, "[HTTP-Response-Body] " + json);
            checkStatusCodeFrom(httpResponse.getResponseCode());
            T response = deserializeFromJSONResponse(json, clazz);
            checkStatusCodeFrom(httpResponse.getResponseCode());

            return ApiResult.success(response);

        } catch (UnauthorizedException ex) {
            if (endpoint instanceof RegisterEndpoint || endpoint instanceof TokenEndpoint) {
                clearToken();
                return ApiResult.fail(ApiErrorType.Unauthorized, "Authentication endpoint returned 401");
            }

            Log.w(TAG, "401 Unauthorized. Need to renew token.");

            if (!isRenewingToken()) {
                try {
                    currentRenewalFuture = new AppAmbitTaskFuture<>();
                    Log.d(TAG, "Token invalid - triggering renewal");
                    ApiErrorType renewalResult = renewToken(currentRenewalFuture);

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

    public AppAmbitTaskFuture<ApiErrorType> GetNewToken() {
        AppAmbitTaskFuture<ApiErrorType> newTokenFuture = new AppAmbitTaskFuture<>();
        mExecutor.execute(() -> {
            ApiErrorType result = renewToken(null);
            newTokenFuture.complete(result);
        });
        return newTokenFuture;
    }

    private ApiErrorType renewToken(AppAmbitTaskFuture<ApiErrorType> asyncFuture) {
        try {
            TokenEndpoint tokenEndpoint = TokenService.createTokenendpoint();
            ApiResult<TokenResponse> tokenResponse = executeRequest(tokenEndpoint, TokenResponse.class);

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

        Log.d(TAG, "Full URL: " + fullUrl);
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

        addAuthorizationHeaderIfNeeded(connection, endpoint);

        if (!"GET".equals(endpoint.getMethod().name()) && !"DELETE".equals(endpoint.getMethod().name())) {
            connection.setDoOutput(true);
            if (payload != null) {
                OutputStream os = connection.getOutputStream();

                if (isMultipart) {
                    try {
                        String boundary = connection.getRequestProperty("Content-Type").split("boundary=")[1];
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream debugStream = new DataOutputStream(baos);
                        MultipartFormData.getOutputString(payload, debugStream, boundary, true);
                        debugStream.flush();
                        debugStream.close();

                        String multipartBody = baos.toString(StandardCharsets.UTF_8.name());
                        Log.d(TAG, "[HTTP-Request-Body] Multipart:\n" + multipartBody);


                        os.write(baos.toByteArray());
                        os.flush();
                        os.close();

                    }catch (Exception e) {
                        Log.e(TAG, "Error during multipart serialization: " + e.getMessage());
                        throw new IOException("Error during multipart serialization", e);
                    }

                } else {
                    String json = JsonConvertUtils.toJson(payload);
                    Log.d(TAG, "[HTTP-Response-Body] " + json);
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    os.flush();
                    os.close();
                }
            }
        }
        return connection;
    }

    private void addAuthorizationHeaderIfNeeded(HttpURLConnection connection, IEndpoint endpoint) {
        if (endpoint instanceof CmsEndpoint) {
            connection.setRequestProperty("X-App-Key", AppAmbit.getAppKey());
            return;
        }

        String token = getToken();
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResult<T> executeCmsGetRequest(IEndpoint endpoint, Class<T> clazz) {
        String fullUrl = endpoint.getBaseUrl() + endpoint.getUrl();
        Log.d(TAG, "Full URL: " + fullUrl);

        SSLSocket socket = null;
        try {
            URL parsed = new URL(fullUrl);
            String host = parsed.getHost();
            int port = parsed.getPort() != -1 ? parsed.getPort() : 443;
            String requestPath = parsed.getFile(); // preserves raw brackets — URL.getFile() never re-encodes

            // SSLCertificateSocketFactory.setHostname() sets the SNI extension — required for
            // virtual-host routing on shared infrastructure (CDNs, load balancers).
            // Without SNI the server cannot identify the target host and never responds.
            @SuppressWarnings("deprecation")
            SSLCertificateSocketFactory sslFactory =
                    (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(30000);
            socket = (SSLSocket) sslFactory.createSocket(host, port);
            sslFactory.setHostname(socket, host);
            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(30));
            socket.startHandshake();

            // Write raw HTTP/1.1 request — brackets reach server as-is
            String request = "GET " + requestPath + " HTTP/1.1\r\n"
                    + "Host: " + host + "\r\n"
                    + "Accept: application/json\r\n"
                    + "X-App-Key: " + AppAmbit.getAppKey() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            // Read full response as bytes to handle chunked encoding correctly
            ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = socket.getInputStream().read(buf)) != -1) {
                responseBytes.write(buf, 0, n);
            }
            byte[] raw = responseBytes.toByteArray();

            // Locate \r\n\r\n boundary between headers and body
            int bodyStart = -1;
            for (int i = 0; i < raw.length - 3; i++) {
                if (raw[i] == '\r' && raw[i+1] == '\n' && raw[i+2] == '\r' && raw[i+3] == '\n') {
                    bodyStart = i + 4;
                    break;
                }
            }
            if (bodyStart < 0) {
                return ApiResult.fail(ApiErrorType.Unknown, "Malformed HTTP response");
            }

            String headerSection = new String(raw, 0, bodyStart - 4, StandardCharsets.UTF_8);
            byte[] bodyBytes = Arrays.copyOfRange(raw, bodyStart, raw.length);

            // Parse status code from first header line
            int statusCode = 200;
            String[] headerLines = headerSection.split("\r\n");
            if (headerLines.length > 0) {
                String[] statusParts = headerLines[0].split(" ", 3);
                if (statusParts.length >= 2) {
                    try { statusCode = Integer.parseInt(statusParts[1]); }
                    catch (NumberFormatException ignore) {}
                }
            }
            Log.d(TAG, "HTTP-Response-Header: " + statusCode);

            // Decode chunked transfer encoding if needed
            if (headerSection.toLowerCase(Locale.US).contains("transfer-encoding: chunked")) {
                bodyBytes = decodeChunkedBodyBytes(bodyBytes);
            }

            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            Log.d(TAG, "[HTTP-Response-Body] " + body);
            checkStatusCodeFrom(statusCode);

            return ApiResult.success((T) body);

        } catch (UnauthorizedException ex) {
            return ApiResult.fail(ApiErrorType.Unauthorized, "CMS request unauthorized");
        } catch (HttpRequestException ex) {
            return ApiResult.fail(ApiErrorType.Unknown, ex.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "CMS request failed", e);
            return ApiResult.fail(ApiErrorType.Unknown, "CMS request failed");
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignore) {}
            }
        }
    }

    private static byte[] decodeChunkedBodyBytes(byte[] data) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < data.length) {
            // Find end of chunk-size line (\r\n)
            int lineEnd = pos;
            while (lineEnd + 1 < data.length && !(data[lineEnd] == '\r' && data[lineEnd + 1] == '\n')) {
                lineEnd++;
            }
            if (lineEnd + 1 >= data.length) break;
            int chunkSize;
            try {
                String sizeLine = new String(data, pos, lineEnd - pos, StandardCharsets.US_ASCII).trim();
                int ext = sizeLine.indexOf(';');
                if (ext >= 0) sizeLine = sizeLine.substring(0, ext);
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException e) {
                break;
            }
            if (chunkSize == 0) break;
            pos = lineEnd + 2; // skip \r\n after size
            if (pos + chunkSize > data.length) break;
            result.write(data, pos, chunkSize);
            pos += chunkSize + 2; // skip chunk data + trailing \r\n
        }
        return result.toByteArray();
    }

    private static String serializedGetUrl(String baseUrl, Object payload)
            throws IllegalAccessException, java.io.UnsupportedEncodingException {

        if (payload == null) return baseUrl;

        var pairs = new java.util.ArrayList<String>();

        for (var field : payload.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            JsonKey ann = field.getAnnotation(JsonKey.class);
            String key = (ann != null && !ann.value().isEmpty()) ? ann.value() : field.getName();

            Object value = field.get(payload);
            if (value == null) continue;

            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) continue;

            pairs.add(java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8.name())
                    + "="
                    + java.net.URLEncoder.encode(stringValue, java.nio.charset.StandardCharsets.UTF_8.name()));
        }

        if (pairs.isEmpty()) return baseUrl;

        return baseUrl + "?" + String.join("&", pairs);
    }
}