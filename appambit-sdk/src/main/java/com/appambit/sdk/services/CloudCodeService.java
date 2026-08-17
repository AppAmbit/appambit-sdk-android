package com.appambit.sdk.services;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appambit.sdk.AppConstants;
import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.cloudcode.CloudCodeCancellationToken;
import com.appambit.sdk.models.cloudcode.CloudCodeError;
import com.appambit.sdk.models.cloudcode.CloudCodeRequest;
import com.appambit.sdk.models.cloudcode.CloudCodeResponse;
import com.appambit.sdk.models.cloudcode.CloudCodeResult;
import com.appambit.sdk.services.endpoints.CloudCodeEndpoint;
import com.appambit.sdk.services.interfaces.HttpTransport;
import com.appambit.sdk.services.interfaces.HttpTransportResponse;
import com.appambit.sdk.utils.AppAmbitTaskFuture;
import com.appambit.sdk.utils.CloudCodeJson;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CloudCodeService {
    private static final Set<String> RESERVED_HEADERS = new HashSet<>();
    static {
        String[] names = {
                "authorization", "cookie", "host", "content-length", "content-type", "accept",
                "x-app-key", "x-request-id"
        };
        for (String name : names) RESERVED_HEADERS.add(name);
    }

    private final HttpTransport transport;

    public CloudCodeService(@NonNull HttpTransport transport) {
        this.transport = transport;
    }

    public CloudCodeRequest<CloudCodeResponse> call(
            String function,
            HttpMethodEnum method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers) {
        CloudCodeCancellationToken cancellation = new CloudCodeCancellationToken();
        AppAmbitTaskFuture<CloudCodeResponse> future = new AppAmbitTaskFuture<>();
        CloudCodeError validation = validate(function, method, query, body, headers, null);
        if (validation != null) {
            future.fail(validation);
            return new CloudCodeRequest<>(future, cancellation);
        }

        CloudCodeEndpoint endpoint;
        try {
            endpoint = new CloudCodeEndpoint(function, method, query, body, headers);
        } catch (IllegalArgumentException endpointError) {
            future.fail(CloudCodeError.invalidBody(endpointError.getCause() == null ? endpointError : endpointError.getCause()));
            return new CloudCodeRequest<>(future, cancellation);
        }
        return execute(endpoint, cancellation, future, response -> {
            HttpTransportResponse successful = requireSuccess(response);
            try {
                return new CloudCodeResponse(
                        CloudCodeJson.decodeDynamic(bodyAsString(successful)),
                        successful.getStatusCode(),
                        requestId(successful),
                        successful.getHeaders());
            } catch (Exception error) {
                throw CloudCodeError.decoding(error.getMessage(), error);
            }
        });
    }

    public <T> CloudCodeRequest<CloudCodeResult<T>> callTyped(
            String function,
            HttpMethodEnum method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers,
            Class<T> responseType) {
        CloudCodeCancellationToken cancellation = new CloudCodeCancellationToken();
        AppAmbitTaskFuture<CloudCodeResult<T>> future = new AppAmbitTaskFuture<>();
        if (responseType == null) {
            future.fail(CloudCodeError.invalidResponseType());
            return new CloudCodeRequest<>(future, cancellation);
        }
        CloudCodeError validation = validate(function, method, query, body, headers, responseType);
        if (validation != null) {
            future.fail(validation);
            return new CloudCodeRequest<>(future, cancellation);
        }

        CloudCodeEndpoint endpoint;
        try {
            endpoint = new CloudCodeEndpoint(function, method, query, body, headers);
        } catch (IllegalArgumentException endpointError) {
            future.fail(CloudCodeError.invalidBody(endpointError.getCause() == null ? endpointError : endpointError.getCause()));
            return new CloudCodeRequest<>(future, cancellation);
        }
        return execute(endpoint, cancellation, future, response -> {
            HttpTransportResponse successful = requireSuccess(response);
            String responseBody = bodyAsString(successful);
            if (successful.getStatusCode() == 204 || responseBody == null || responseBody.trim().isEmpty()) {
                return new CloudCodeResult<>(
                        null,
                        successful.getStatusCode(),
                        requestId(successful),
                        successful.getHeaders());
            }
            try {
                return new CloudCodeResult<>(
                        CloudCodeJson.decode(responseBody, responseType),
                        successful.getStatusCode(),
                        requestId(successful),
                        successful.getHeaders());
            } catch (Exception error) {
                throw CloudCodeError.decoding(error.getMessage(), error);
            }
        });
    }

    private interface ResponseMapper<T> {
        T map(HttpTransportResponse response) throws Exception;
    }

    private <T> CloudCodeRequest<T> execute(
            CloudCodeEndpoint endpoint,
            CloudCodeCancellationToken cancellation,
            AppAmbitTaskFuture<T> future,
            ResponseMapper<T> mapper) {
        try {
            transport.executeRaw(endpoint, AppConstants.CLOUD_CODE_TIMEOUT_MILLIS, response -> {
                if (cancellation.isCancelled()) return;
                try {
                    future.complete(mapper.map(response));
                } catch (CloudCodeError error) {
                    future.fail(error);
                } catch (Throwable error) {
                    future.fail(CloudCodeError.transport(error.getMessage(), error));
                }
            });
        } catch (Throwable transportFailure) {
            future.fail(transportFailure instanceof CloudCodeError
                    ? (CloudCodeError) transportFailure
                    : transportError(transportFailure));
        }
        return new CloudCodeRequest<>(future, cancellation);
    }

    @Nullable
    private static CloudCodeError validate(
            String function,
            @Nullable HttpMethodEnum method,
            @Nullable Map<String, String> query,
            @Nullable Map<String, Object> body,
            @Nullable Map<String, String> headers,
            @Nullable Class<?> responseType) {
        if (function == null || function.isEmpty() || function.contains("/")
                || function.chars().anyMatch(Character::isWhitespace)
                || function.chars().anyMatch(Character::isISOControl)) {
            return CloudCodeError.invalidFunction(function);
        }
        if (method == null) return CloudCodeError.invalidMethod();
        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                    return CloudCodeError.invalidQuery(String.valueOf(entry.getKey()));
                }
            }
        }
        if (headers != null) {
            for (String header : headers.keySet()) {
                String value = headers.get(header);
                if (header == null || header.isEmpty() || value == null
                        || header.indexOf('\r') >= 0 || header.indexOf('\n') >= 0
                        || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0
                        || RESERVED_HEADERS.contains(header.toLowerCase(Locale.US))) {
                    return CloudCodeError.invalidHeader(header);
                }
            }
        }
        if (responseType == Void.class) return CloudCodeError.invalidResponseType();
        return null;
    }

    @Nullable
    private static HttpTransportResponse requireSuccess(HttpTransportResponse response)
            throws CloudCodeError {
        if (response.getError() != null) throw transportError(response.getError());
        if (response.getStatusCode() == null) throw CloudCodeError.transport("Missing HTTP status code.", null);
        if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
            String rawBody = bodyAsString(response);
            Object parsedBody = null;
            try {
                parsedBody = CloudCodeJson.decodeDynamic(rawBody);
            } catch (Exception ignored) {
                // Preserve the raw body when the server did not return JSON.
            }
            if (!(parsedBody instanceof Map) && !(parsedBody instanceof java.util.List)) {
                parsedBody = null;
            }
            String preservedRawBody = parsedBody == null ? rawBody : null;
            throw CloudCodeError.http(
                    response.getStatusCode(), parsedBody, preservedRawBody, requestId(response));
        }
        return response;
    }

    private static CloudCodeError transportError(Throwable error) {
        if (error instanceof MalformedURLException) return CloudCodeError.invalidUrl(error);
        if (error instanceof SocketTimeoutException) return CloudCodeError.timedOut(error);
        if (error instanceof UnknownHostException || error instanceof ConnectException) {
            return CloudCodeError.networkUnavailable(error);
        }
        if (error instanceof IOException
                && "No internet connection".equals(error.getMessage())) {
            return CloudCodeError.networkUnavailable(error);
        }
        return CloudCodeError.transport(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(), error);
    }

    @Nullable
    private static String bodyAsString(HttpTransportResponse response) {
        byte[] body = response.getBody();
        return body == null || body.length == 0 ? null : new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Nullable
    private static String requestId(HttpTransportResponse response) {
        for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
            if ("x-request-id".equalsIgnoreCase(header.getKey())) return header.getValue();
        }
        try {
            Object body = CloudCodeJson.decodeDynamic(bodyAsString(response));
            if (body instanceof Map) {
                Object value = ((Map<?, ?>) body).get("request_id");
                return value == null ? null : String.valueOf(value);
            }
        } catch (Exception ignored) {
            // Request ID is best-effort metadata.
        }
        return null;
    }
}
