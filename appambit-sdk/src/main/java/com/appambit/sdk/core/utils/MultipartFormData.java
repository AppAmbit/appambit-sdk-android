package com.appambit.sdk.core.utils;

import static com.appambit.sdk.core.utils.DateUtils.toIsoUtc;
import androidx.annotation.NonNull;
import com.appambit.sdk.core.models.logs.Log;
import com.appambit.sdk.core.models.logs.LogEntity;
import com.appambit.sdk.core.models.logs.LogBatch;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MultipartFormData {
    private static final String TAG = MultipartFormData.class.getSimpleName();
    public static void getOutputString(Object payload, DataOutputStream output, String boundary, boolean includeFinalBoundary) throws IOException {
        if (payload instanceof LogBatch) {
            List<LogEntity> logs = ((LogBatch) payload).getLogs();
            for (int i = 0; i < logs.size(); i++) {
                writeLogGeneric(logs.get(i), output, boundary, "logs["+i+"]");
            }
        } else if (payload instanceof Log) {
            writeLogGeneric((Log) payload, output, boundary, null);
        } else {
            throw new UnsupportedOperationException("Unsupported multipart object: " + payload.getClass().getSimpleName());
        }

        if (includeFinalBoundary) {
            output.writeBytes("--" + boundary + "--\r\n");
        }
    }
    private static void writeLogGeneric(@NonNull Log log, DataOutputStream output, String boundary, String prefix) throws IOException {
        try {
            JSONObject obj = JsonConvertUtils.objectToJson(log);
            for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                Object value = obj.get(key);
                if (value instanceof JSONObject) {
                    continue;
                }

                if ("created_at".equals(key) || "context".equals(key) || "file".equals(key)){
                    continue;
                }
                if ("type".equals(key)) {
                    value = value.toString().toLowerCase();
                }

                writeField(output, boundary, buildFieldName(prefix, key), value.toString());
            }
        } catch (Exception e) {
            android.util.Log.d(TAG, "Error converting log to JSON: " + e.getMessage());
        }

        if (log instanceof LogEntity) {
            Date createdAt = ((LogEntity) log).getCreatedAt();
            if (createdAt != null) {
                writeField(output, boundary, buildFieldName(prefix, "created_at"), toIsoUtc(createdAt));
            }
        }

        Map<String, String> context = log.getContext();
        if (context != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                writeField(output, boundary, buildFieldName(prefix, "context[" + entry.getKey() + "]"), entry.getValue());
            }
        }

        String fileContent = log.getFile();
        if (fileContent != null && !fileContent.isEmpty()) {
            String fileName = "log-" + formatFileNameDate(new Date()) + ".txt";
            writeMemoryFile(output, boundary, fileName, fileContent.getBytes(StandardCharsets.UTF_8), prefix);
        }
    }

    private static void writeField(@NonNull DataOutputStream output, String boundary, String fieldName, @NonNull String value) {
        try {
            output.writeBytes("--" + boundary + "\r\n");
            output.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n");
            output.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.writeBytes("\r\n");
        }catch (Exception e) {
            android.util.Log.d(TAG, "Error writing field: " + e.getMessage());
        }
    }

    private static void writeMemoryFile(@NonNull DataOutputStream output, String boundary, String filename, byte[] data, String fieldPrefix) throws IOException {
        String actualFieldName = fieldPrefix != null ? fieldPrefix + "[file]" : "file";

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + actualFieldName + "\"; filename=\"" + filename + "\"\r\n");
        output.writeBytes("Content-Type: text/plain\r\n\r\n");

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
    private static String buildFieldName(String prefix, String key) {
        return prefix != null ? prefix + "[" + key + "]" : key;
    }

    @NonNull
    private static String formatFileNameDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}