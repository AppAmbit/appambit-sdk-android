package com.appambit.javaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.appambit.javaapp.models.CloudCodeDashboardSummary;
import com.appambit.sdk.CloudCode;
import com.appambit.sdk.PushNotifications;
import com.appambit.sdk.enums.HttpMethodEnum;
import com.appambit.sdk.models.cloudcode.CloudCodeError;
import com.appambit.sdk.models.cloudcode.CloudCodeRequest;
import com.appambit.sdk.models.cloudcode.CloudCodeResponse;
import com.appambit.sdk.models.cloudcode.CloudCodeResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CloudCodeFragment extends Fragment {
    private static final int BLUE = Color.rgb(40, 100, 210);
    private static final int PURPLE = Color.rgb(125, 75, 180);

    private LinearLayout content;
    private EditText taskTitle;
    private EditText taskId;
    private EditText postUuid;
    private EditText publishTitle;
    private EditText publishBody;
    private TextView databaseStatus;
    private TextView cmsStatus;
    private Button setupDatabaseButton;
    private boolean databaseAvailable;
    private boolean databaseTablesReady;
    private boolean verifyingBackend;
    private ProgressBar spinner;
    private boolean running;
    private String lastResultId;
    private TextView lastResultText;
    private TextView lastResultTitle;
    private Button lastResultToggle;
    private LinearLayout resultContainer;
    private String fullResultText;
    private boolean resultExpanded = true;
    private CloudCodeRequest<?> pendingRequest;
    private final Map<String, View> functionCards = new LinkedHashMap<>();

    private static final List<CloudCodeDemoCatalog.Demo> DEMOS = CloudCodeDemoCatalog.DEMOS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(32, 24, 32, 32);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buildUi();
        verifyBackend();
    }

    private void buildUi() {
        addText("Cloud Code", 24, Typeface.BOLD, 0);
        addText("HTTP-triggered functions using the Android consumer token.", 14, Typeface.NORMAL, 4);

        addSetupGroup("Database", "Create Database first", BLUE, "cloud-demo-setup-database-android", CloudCodeDemoCatalog.Action.SETUP_DATABASE);
        addSetupGroup("CMS", "Create Content Type first", PURPLE, null, null);

        for (String section : new String[]{"Database", "CMS", "Push", "HTTP"}) {
            addText(section, 20, Typeface.BOLD, 18);
            if ("Database".equals(section)) {
                taskTitle = addField("Task title", "Buy coffee", false);
                taskId = addField("Task id for update/delete", "", true);
            } else if ("CMS".equals(section)) {
                postUuid = addField("CMS post UUID (optional)", "", false);
                publishTitle = addField("Sample title", "Cloud Code sample post", false);
                publishBody = addField("Sample body", "Published through an HTTP Cloud Function.", false);
            }

            for (CloudCodeDemoCatalog.Demo demo : DEMOS) {
                if (section.equals(demo.section)) addDemoCard(demo);
            }
        }

        spinner = new ProgressBar(requireContext());
        spinner.setVisibility(View.GONE);
        content.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
        addResultViews();
    }

    private void addSetupGroup(String title, String requirement, int tint, String slug, CloudCodeDemoCatalog.Action action) {
        LinearLayout group = card(0xFFF3F3F3, 10);
        group.setPadding(24, 20, 24, 20);
        group.addView(text(title, 20, Typeface.BOLD));
        TextView requirementView = text(requirement, 12, Typeface.BOLD);
        requirementView.setTextColor(tint);
        group.addView(requirementView, marginParams(0, 8, 0, 0));

        LinearLayout row = horizontal();
        databaseStatus = "Database".equals(title) ? text("Not available", 13, Typeface.BOLD) : databaseStatus;
        cmsStatus = "CMS".equals(title) ? text("Not available", 13, Typeface.BOLD) : cmsStatus;
        TextView status = "Database".equals(title) ? databaseStatus : cmsStatus;
        row.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        if (slug != null) {
            setupDatabaseButton = actionButton("Run", v -> runOrConfirm(action, "setup-database", slug));
            setupDatabaseButton.setEnabled(false);
            setupDatabaseButton.setContentDescription(slug);
            LinearLayout setupCard = card(0xFFF8F8F8, 10);
            setupCard.setPadding(24, 18, 24, 18);
            LinearLayout setupRow = horizontal();
            LinearLayout info = new LinearLayout(requireContext());
            info.setOrientation(LinearLayout.VERTICAL);
            TextView slugLabel = text(slug, 14, Typeface.BOLD);
            TextView detail = text("Create the tables used by the Database examples without destroying data.", 12, Typeface.NORMAL);
            detail.setTextColor(Color.DKGRAY);
            TextView prerequisite = text("Existing linked Database", 11, Typeface.NORMAL);
            prerequisite.setTextColor(Color.GRAY);
            info.addView(slugLabel);
            info.addView(detail, marginParams(0, 4, 0, 0));
            info.addView(prerequisite, marginParams(0, 4, 0, 0));
            setupRow.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            setupRow.addView(setupDatabaseButton);
            setupCard.addView(setupRow);
            group.addView(row, marginParams(0, 12, 0, 0));
            group.addView(setupCard, marginParams(0, 8, 0, 0));
            functionCards.put("setup-database", group);
            content.addView(group, marginParams(0, 12, 0, 0));
            return;
        }
        group.addView(row, marginParams(0, 12, 0, 0));
        content.addView(group, marginParams(0, 12, 0, 0));
    }

    private void addDemoCard(CloudCodeDemoCatalog.Demo demo) {
        LinearLayout card = card(0xFFF8F8F8, 10);
        card.setPadding(24, 18, 24, 18);
        LinearLayout row = horizontal();
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        TextView slug = text(demo.slug, 14, Typeface.BOLD);
        TextView detail = text(demo.detail, 12, Typeface.NORMAL);
        detail.setTextColor(Color.DKGRAY);
        TextView prerequisite = text(demo.prerequisite, 11, Typeface.NORMAL);
        prerequisite.setTextColor(Color.GRAY);
        info.addView(slug);
        info.addView(detail, marginParams(0, 4, 0, 0));
        info.addView(prerequisite, marginParams(0, 4, 0, 0));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(actionButton("Run", v -> runOrConfirm(demo.action, demo.id, demo.slug)));
        card.addView(row);
        content.addView(card, marginParams(0, 8, 0, 0));
        functionCards.put(demo.id, card);
    }

    private EditText addField(String hint, String value, boolean number) {
        EditText field = new EditText(requireContext());
        field.setHint(hint);
        field.setText(value);
        field.setSingleLine(!"Sample body".equals(hint));
        if (number) field.setInputType(InputType.TYPE_CLASS_NUMBER);
        else if ("Sample body".equals(hint)) field.setMinLines(3);
        content.addView(field, marginParams(0, 0, 0, 8));
        return field;
    }

    private void runOrConfirm(CloudCodeDemoCatalog.Action action, String id, String slug) {
        if (action == CloudCodeDemoCatalog.Action.SETUP_DATABASE && (!databaseAvailable || databaseTablesReady)) return;
        if (action == CloudCodeDemoCatalog.Action.DELETE_TASK || action == CloudCodeDemoCatalog.Action.PUBLISH_POST || action == CloudCodeDemoCatalog.Action.PUSH) {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Cloud Code action")
                    .setMessage("This calls a real backend operation. Continue only if the required service is configured.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Run", (dialog, which) -> run(action, id, slug))
                    .show();
            return;
        }
        run(action, id, slug);
    }

    private void run(CloudCodeDemoCatalog.Action action, String id, String slug) {
        if (action == CloudCodeDemoCatalog.Action.SETUP_DATABASE && (!databaseAvailable || databaseTablesReady)) return;
        if (running) return;
        if ((action == CloudCodeDemoCatalog.Action.COMPLETE_TASK || action == CloudCodeDemoCatalog.Action.DELETE_TASK) && parseTaskId() == null) {
            showResult(id, "Input required", "Enter a numeric task id first.");
            return;
        }
        if (action == CloudCodeDemoCatalog.Action.PUSH) {
            ensurePushReady(ready -> {
                if (!ready) {
                    showResult(id, "Permission required", "Notification permission is required. Enable notifications and try again.");
                    return;
                }
                call(action, id, slug);
            });
        } else {
            call(action, id, slug);
        }
    }

    private void call(CloudCodeDemoCatalog.Action action, String id, String slug) {
        running = true;
        updateDatabaseButtonState();
        pendingRequest = null;
        prepareResult(id, slug);
        long started = SystemClock.elapsedRealtime();
        CloudCodeDemoCatalog.RequestConfig config = configuration(action, slug);
        if (action == CloudCodeDemoCatalog.Action.SUMMARY) {
            CloudCodeRequest<CloudCodeResult<CloudCodeDashboardSummary>> request = CloudCode.call(
                    config.slug, config.method, config.query, config.body, headers(), CloudCodeDashboardSummary.class);
            pendingRequest = request;
            request.then(result -> finishTyped(result, null, started))
                    .onError(error -> finishTyped(null, error, started));
            return;
        }
        CloudCodeRequest<CloudCodeResponse> request = CloudCode.call(
                config.slug, config.method, config.query, config.body, headers());
        pendingRequest = request;
        request.then(response -> finish(response, null, started))
                .onError(error -> finish(null, error, started));
    }

    private CloudCodeDemoCatalog.RequestConfig configuration(CloudCodeDemoCatalog.Action action, String slug) {
        Map<String, String> query = null;
        Map<String, Object> body = null;
        HttpMethodEnum method = HttpMethodEnum.POST;
        switch (action) {
            case SETUP_DATABASE: method = HttpMethodEnum.POST; break;
            case CREATE_TASK: body = mapOf("title", taskTitle.getText().toString()); break;
            case LIST_TASKS: method = HttpMethodEnum.GET; query = mapOfString("limit", "20"); break;
            case COMPLETE_TASK: method = HttpMethodEnum.PATCH; body = mapOf("task_id", parseTaskId()); break;
            case DELETE_TASK: method = HttpMethodEnum.DELETE; body = mapOf("task_id", parseTaskId()); break;
            case CREATE_ORDER: body = new LinkedHashMap<>(); body.put("idempotency_key", java.util.UUID.randomUUID().toString()); body.put("amount", 100); break;
            case SUMMARY: method = HttpMethodEnum.GET; break;
            case PUBLISH_POST: body = new LinkedHashMap<>(); body.put("title", publishTitle.getText().toString()); body.put("body", publishBody.getText().toString()); break;
            case READ_POSTS: method = HttpMethodEnum.GET; query = postUuid.getText().toString().trim().isEmpty() ? null : mapOfString("uuid", postUuid.getText().toString().trim()); break;
            case INSPECTOR: query = mapOfString("source", "java"); body = new LinkedHashMap<>(); body.put("message", "hello"); body.put("count", 2); break;
            case JSON_VALUES: break;
            case NULL_CONTRACT: method = HttpMethodEnum.GET; break;
            case RESPONSE_SHAPES: break;
            case CONTROLLED_ERROR: body = mapOf("invalid", true); break;
            case TIMEOUT: method = HttpMethodEnum.GET; break;
            case RUNTIME_CONTEXT: method = HttpMethodEnum.GET; break;
            case PUSH: body = new LinkedHashMap<>(); body.put("title", "Cloud Code Android demo"); body.put("body", "Push from Java sample"); break;
        }
        return new CloudCodeDemoCatalog.RequestConfig(slug, method, query, body);
    }

    private void verifyBackend() {
        if (databaseStatus == null || cmsStatus == null) return;
        verifyingBackend = true;
        databaseAvailable = false;
        databaseTablesReady = false;
        updateDatabaseButtonState();
        databaseStatus.setText("Checking...");
        cmsStatus.setText("Checking...");
        CloudCode.call("cloud-demo-dashboard-summary-android", HttpMethodEnum.GET, null, null, headers(), CloudCodeDashboardSummary.class)
                .then(result -> {
                    verifyingBackend = false;
                    CloudCodeDashboardSummary summary = result.getData();
                    databaseAvailable = summary != null && summary.database_available;
                    databaseTablesReady = summary != null && summary.database_tables_ready;
                    if (!databaseAvailable) databaseStatus.setText("Not available");
                    else if (databaseTablesReady) databaseStatus.setText("Tables ready");
                    else databaseStatus.setText("Available");
                    cmsStatus.setText(summary != null && summary.posts != null ? "Available" : "Not available");
                    updateDatabaseButtonState();
                })
                .onError(error -> {
                    verifyingBackend = false;
                    databaseAvailable = false;
                    databaseTablesReady = false;
                    databaseStatus.setText("Not available");
                    cmsStatus.setText("Not available");
                    updateDatabaseButtonState();
                });
    }

    private void updateDatabaseButtonState() {
        if (setupDatabaseButton != null) {
            setupDatabaseButton.setEnabled(databaseAvailable && !databaseTablesReady && !running && !verifyingBackend);
        }
    }

    private void ensurePushReady(final PermissionCallback callback) {
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            PushNotifications.setNotificationsEnabled(requireContext(), true);
            callback.ready(true);
            return;
        }
        PushNotifications.requestNotificationPermission(requireActivity(), granted -> {
            if (granted) PushNotifications.setNotificationsEnabled(requireContext(), true);
            callback.ready(granted);
        });
    }

    private interface PermissionCallback { void ready(boolean granted); }

    private void finish(CloudCodeResponse response, Throwable error, long started) {
        running = false;
        updateDatabaseButtonState();
        long elapsed = SystemClock.elapsedRealtime() - started;
        if (response != null) {
            showResult(lastResultId, "Result · " + lastResultId,
                    "HTTP " + response.getStatusCode() + "\nDuration: " + duration(elapsed)
                            + "\nrequestId: " + safe(response.getRequestId()) + "\nBody: " + jsonText(response.getData()));
        } else {
            showResult(lastResultId, "Result · " + lastResultId,
                    formatError(error, elapsed));
        }
        if ("setup-database".equals(lastResultId)) verifyBackend();
    }

    private void finishTyped(CloudCodeResult<CloudCodeDashboardSummary> result, Throwable error, long started) {
        running = false;
        long elapsed = SystemClock.elapsedRealtime() - started;
        if (result != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("task_count", result.getData() == null ? null : result.getData().task_count);
            data.put("posts", result.getData() == null ? new ArrayList<>() : result.getData().posts);
            showResult(lastResultId, "Result · " + lastResultId,
                    "HTTP " + result.getStatusCode() + "\nDuration: " + duration(elapsed)
                            + "\nrequestId: " + safe(result.getRequestId()) + "\nBody: " + jsonText(data));
        } else {
            showResult(lastResultId, "Result · " + lastResultId, formatError(error, elapsed));
        }
    }

    private void prepareResult(String id, String slug) {
        lastResultId = id;
        running = true;
        if (resultContainer != null) {
            View anchor = functionCards.get(id);
            ViewGroup parent = (ViewGroup) resultContainer.getParent();
            if (parent != null) parent.removeView(resultContainer);
            if (anchor != null) {
                int index = content.indexOfChild(anchor);
                content.addView(resultContainer, Math.min(index + 1, content.getChildCount()));
            } else {
                content.addView(resultContainer);
            }
            resultContainer.setVisibility(View.VISIBLE);
        }
        showResult(id, "Result · " + slug, "Calling " + slug + "...");
    }

    private void showResult(String id, String title, String text) {
        if (spinner != null) spinner.setVisibility(running ? View.VISIBLE : View.GONE);
        if (lastResultText == null) return;
        lastResultTitle.setText(title);
        fullResultText = text;
        updateResultText();
    }

    private void addResultViews() {
        resultContainer = card(0xFFF0F0F0, 10);
        resultContainer.setPadding(20, 16, 20, 16);
        resultContainer.setVisibility(View.GONE);
        LinearLayout header = horizontal();
        lastResultTitle = text("Latest result", 15, Typeface.BOLD);
        header.addView(lastResultTitle, new LinearLayout.LayoutParams(0, -2, 1));
        lastResultToggle = new Button(requireContext());
        lastResultToggle.setText("Collapse");
        lastResultToggle.setOnClickListener(v -> {
            resultExpanded = !resultExpanded;
            updateResultText();
        });
        header.addView(lastResultToggle);
        resultContainer.addView(header);
        lastResultText = text("Run a function to see its response here.", 12, Typeface.NORMAL);
        lastResultText.setTypeface(Typeface.MONOSPACE);
        lastResultText.setTextIsSelectable(true);
        resultContainer.addView(lastResultText, marginParams(0, 8, 0, 0));
        content.addView(resultContainer, marginParams(0, 16, 0, 0));
    }

    private void updateResultText() {
        if (lastResultText == null) return;
        if (resultExpanded || fullResultText == null) {
            lastResultText.setText(fullResultText);
            lastResultToggle.setText("Collapse");
        } else {
            String[] lines = fullResultText.split("\\n");
            lastResultText.setText(lines.length <= 2 ? fullResultText : lines[0] + "\n" + lines[1]);
            lastResultToggle.setText("Expand");
        }
    }

    private String formatError(Throwable error, long elapsed) {
        if (error instanceof CloudCodeError) {
            CloudCodeError cloudError = (CloudCodeError) error;
            if (cloudError.getCode() == CloudCodeError.Code.HTTP) {
                return "Duration: " + duration(elapsed) + "\nrequestId: " + safe(cloudError.getRequestId())
                        + "\nHTTP error body: " + jsonText(cloudError.getBody())
                        + "\nError: " + cloudError.getMessage();
            }
        }
        return "Duration: " + duration(elapsed) + "\nError: "
                + (error == null ? "Unknown error" : error.getMessage());
    }

    private String jsonText(Object value) {
        try {
            if (value == null) return "null";
            if (value instanceof Map) return new JSONObject((Map<?, ?>) value).toString(2);
            if (value instanceof List) return new JSONArray((List<?>) value).toString(2);
            if (value instanceof String) return JSONObject.quote((String) value);
            return String.valueOf(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String duration(long millis) { return String.format(Locale.US, "%.2f s", millis / 1000.0); }
    private String safe(String value) { return value == null ? "none" : value; }

    private Map<String, Object> mapOf(String key, Object value) { Map<String, Object> map = new LinkedHashMap<>(); map.put(key, value); return map; }
    private Map<String, String> mapOfString(String key, String value) { Map<String, String> map = new LinkedHashMap<>(); map.put(key, value); return map; }
    private Map<String, String> headers() { return mapOfString("X-Sample-Client", "java"); }
    private Integer parseTaskId() { try { return Integer.parseInt(taskId.getText().toString().trim()); } catch (NumberFormatException ignored) { return null; } }

    private Button actionButton(String label, View.OnClickListener listener) { Button button = new Button(requireContext()); button.setText(label); button.setOnClickListener(listener); return button; }
    private TextView addText(String value, int size, int style, int top) { TextView view = text(value, size, style); content.addView(view, marginParams(0, top, 0, 0)); return view; }
    private TextView text(String value, int size, int style) { TextView view = new TextView(requireContext()); view.setText(value); view.setTextSize(size); view.setTypeface(Typeface.DEFAULT, style); return view; }
    private LinearLayout horizontal() { LinearLayout row = new LinearLayout(requireContext()); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL); return row; }
    private LinearLayout card(int color, float radius) { LinearLayout layout = new LinearLayout(requireContext()); layout.setOrientation(LinearLayout.VERTICAL); android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable(); background.setColor(color); background.setCornerRadius(radius * getResources().getDisplayMetrics().density); layout.setBackground(background); return layout; }
    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(left, top, right, bottom); return params; }
}
