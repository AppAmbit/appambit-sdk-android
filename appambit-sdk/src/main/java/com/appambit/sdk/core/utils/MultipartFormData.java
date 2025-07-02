package com.appambit.sdk.core.utils;

import static com.appambit.sdk.core.utils.DateUtils.toIsoUtc;

import androidx.annotation.NonNull;
import com.appambit.sdk.core.models.logs.Log;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.models.logs.LogBatch;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MultipartFormData {

    private static final String TAG = MultipartFormData.class.getSimpleName();

    public static void getOutputString(Object payload, DataOutputStream output, String boundary, int indent, boolean includeFinalBoundary) throws IOException {
        if (payload instanceof LogBatch) {
            writeLogBatch((LogBatch) payload, output, boundary);
        } else if (payload instanceof Log) {
            writeLog((Log) payload, output, boundary);
        } else {
            throw new UnsupportedOperationException("Unsupported multipart object: " + payload.getClass().getSimpleName());
        }

        if (includeFinalBoundary) {
            output.writeBytes("--" + boundary + "--\r\n");
        }
    }

    private static void writeLogBatch(@NonNull LogBatch logBatch, DataOutputStream output, String boundary) throws IOException {
        List<LogEntity> logs = logBatch.getLogs();

        for (int i = 0; i < logs.size(); i++) {
            LogEntity log = logs.get(i);
            String indexPrefix = "logs[" + i + "]";

            writeFieldWithPrefix(output, boundary, indexPrefix + "[app_version]", log.getAppVersion());
            writeFieldWithPrefix(output, boundary, indexPrefix + "[classFQN]", log.getClassFQN());
            writeFieldWithPrefix(output, boundary, indexPrefix + "[file_name]", log.getFileName());
            writeFieldWithPrefix(output, boundary, indexPrefix + "[line_number]", String.valueOf(log.getLineNumber()));
            writeFieldWithPrefix(output, boundary, indexPrefix + "[message]", log.getMessage());
            writeFieldWithPrefix(output, boundary, indexPrefix + "[stack_trace]", log.getStackTrace());
            writeFieldWithPrefix(output, boundary, indexPrefix + "[type]", log.getType() != null ? log.getType().toString().toLowerCase(Locale.ROOT) : null);

            Date createdAt = log.getCreatedAt();
            if (createdAt != null) {
                writeFieldWithPrefix(output, boundary, indexPrefix + "[created_at]", toIsoUtc(createdAt));
            }

            Map<String, String> context = log.getContext();
            if (context != null) {
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    writeFieldWithPrefix(output, boundary, indexPrefix + "[context][" + entry.getKey() + "]", entry.getValue());
                }
            }

            String fileContent = log.getFile();
            if (fileContent != null && !fileContent.isEmpty()) {
                String fileName = "log-" + formatFileNameDate(new Date()) + ".txt";
                writeMemoryFileWithPrefix(output, boundary, indexPrefix + "[file]", fileName, fileContent.getBytes(StandardCharsets.UTF_8));
            } else {
                writeFieldWithPrefix(output, boundary, indexPrefix + "[file]", "");
            }
        }
    }

    private static void writeLog(@NonNull Log log, DataOutputStream output, String boundary) throws IOException {
        writeField(output, boundary, "app_version", log.getAppVersion());
        writeField(output, boundary, "classFQN", log.getClassFQN());
        writeField(output, boundary, "file_name", log.getFileName());
        writeField(output, boundary, "line_number", String.valueOf(log.getLineNumber()));
        writeField(output, boundary, "message", log.getMessage());
        writeField(output, boundary, "stack_trace", log.getStackTrace());
        writeField(output, boundary, "type", log.getType() != null ? log.getType().toString().toLowerCase(Locale.ROOT) : null);

        if (log instanceof LogEntity) {
            Date createdAt = ((LogEntity) log).getCreatedAt();
            if (createdAt != null) {
                writeField(output, boundary, "created_at", toIsoUtc(createdAt));
            }
        }

        Map<String, String> context = log.getContext();
        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                writeField(output, boundary, "context[" + entry.getKey() + "]", entry.getValue());
            }
        }

        String fileContent = log.getFile();
        if (fileContent != null && !fileContent.isEmpty()) {
            String fileName = "log-" + formatFileNameDate(new Date()) + ".txt";
            writeMemoryFile(output, boundary, fileName, fileContent.getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static void writeField(DataOutputStream output, String boundary, String fieldName, String value) throws IOException {
        if (value == null) return;

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n");
        output.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
        output.writeBytes("\r\n");
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.writeBytes("\r\n");
    }

    private static void writeFieldWithPrefix(@NonNull DataOutputStream output, String boundary, String fieldName, String value) throws IOException {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n");
        output.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
        output.writeBytes("\r\n");
        if (value != null) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
        output.writeBytes("\r\n");
    }

    private static void writeMemoryFile(@NonNull DataOutputStream output, String boundary, String filename, byte[] data) throws IOException {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + "file" + "\"; filename=\"" + filename + "\"\r\n");
        output.writeBytes("Content-Type: text/plain\r\n");
        output.writeBytes("\r\n");

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bais.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                android.util.Log.d(TAG, "Writing " + bytesRead + " bytes to output for file: " + filename);
            }
        }

        output.writeBytes("\r\n");
    }

    private static void writeMemoryFileWithPrefix(@NonNull DataOutputStream output, String boundary, String fieldName, String filename, byte[] data) throws IOException {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n");
        output.writeBytes("Content-Type: text/plain\r\n");
        output.writeBytes("\r\n");

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bais.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        output.writeBytes("\r\n");
    }

    @NonNull
    private static String formatFileNameDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

}