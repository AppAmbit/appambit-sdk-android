package com.appambit.sdk.models.db;

import android.util.Log;
import com.appambit.sdk.annotations.DbColumn;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbResult {
    private List<String> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private int rowsRead;
    private int rowsWritten;
    private String error;

    DbResult() {}

    static DbResult fromJson(JSONObject json) throws JSONException {
        DbResult result = new DbResult();

        JSONArray colArray = json.optJSONArray("columns");
        if (colArray != null) {
            for (int i = 0; i < colArray.length(); i++) {
                result.columns.add(colArray.getString(i));
            }
        }

        JSONArray rowsArray = json.optJSONArray("rows");
        if (rowsArray != null) {
            for (int i = 0; i < rowsArray.length(); i++) {
                JSONArray rowArray = rowsArray.getJSONArray(i);
                List<Object> row = new ArrayList<>();
                for (int j = 0; j < rowArray.length(); j++) {
                    Object val = rowArray.get(j);
                    row.add(val == JSONObject.NULL ? null : val);
                }
                result.rows.add(row);
            }
        }

        result.rowsRead = json.optInt("rows_read", 0);
        result.rowsWritten = json.optInt("rows_written", 0);
        if (json.has("error") && !json.isNull("error")) {
            result.error = json.optString("error", null);
        }

        return result;
    }

    public boolean hasError() { return error != null; }
    public boolean succeeded() { return error == null; }
    public String getError() { return error; }
    public List<String> getColumns() { return columns; }
    public List<List<Object>> getRows() { return rows; }
    public int getRowsRead() { return rowsRead; }
    public int getRowsWritten() { return rowsWritten; }

    public List<Map<String, Object>> toMaps() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (List<Object> row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                map.put(columns.get(i), row.get(i));
            }
            result.add(map);
        }
        return result;
    }

    public <T> List<T> mapTo(Class<T> modelClass) {
        List<T> result = new ArrayList<>();
        for (List<Object> row : rows) {
            try {
                T instance = modelClass.getDeclaredConstructor().newInstance();
                for (int colIdx = 0; colIdx < columns.size() && colIdx < row.size(); colIdx++) {
                    setField(instance, columns.get(colIdx), row.get(colIdx), modelClass);
                }
                result.add(instance);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        modelClass.getSimpleName() + " requires a public no-arg constructor for DB mapping.", e);
            } catch (Exception e) {
                Log.w("AppAmbitDb", "Failed to map row to " + modelClass.getSimpleName(), e);
            }
        }
        return result;
    }

    private <T> void setField(T instance, String columnName, Object value, Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                DbColumn annotation = field.getAnnotation(DbColumn.class);
                String mapped = (annotation != null && !annotation.value().isEmpty())
                        ? annotation.value()
                        : field.getName();
                if (!mapped.equals(columnName)) continue;
                field.setAccessible(true);
                try {
                    field.set(instance, castValue(value, field.getType()));
                } catch (Exception e) {
                    Log.w("AppAmbitDb", "Cannot map column '" + columnName + "' to field '"
                            + field.getName() + "': " + e.getMessage());
                }
                return;
            }
            current = current.getSuperclass();
        }
    }

    private Object castValue(Object value, Class<?> type) {
        if (value == null) return null;
        if (type == String.class) return value.toString();
        if (type == int.class || type == Integer.class)
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
        if (type == long.class || type == Long.class)
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
        if (type == double.class || type == Double.class)
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
        if (type == float.class || type == Float.class)
            return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString());
        if (type == boolean.class || type == Boolean.class)
            return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
        return value;
    }
}
