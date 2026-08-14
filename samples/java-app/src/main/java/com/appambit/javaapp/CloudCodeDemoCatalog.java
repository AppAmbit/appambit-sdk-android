package com.appambit.javaapp;

import com.appambit.sdk.enums.HttpMethodEnum;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class CloudCodeDemoCatalog {
    enum Action {
        SETUP_DATABASE, CREATE_TASK, LIST_TASKS, COMPLETE_TASK, DELETE_TASK,
        CREATE_ORDER, SUMMARY, PUBLISH_POST, READ_POSTS, PUSH, INSPECTOR,
        JSON_VALUES, NULL_CONTRACT, RESPONSE_SHAPES, CONTROLLED_ERROR,
        TIMEOUT, RUNTIME_CONTEXT
    }

    static final class Demo {
        final String id;
        final String section;
        final String title;
        final String slug;
        final String detail;
        final String prerequisite;
        final Action action;

        Demo(String id, String section, String title, String slug, String detail,
             String prerequisite, Action action) {
            this.id = id;
            this.section = section;
            this.title = title;
            this.slug = slug;
            this.detail = detail;
            this.prerequisite = prerequisite;
            this.action = action;
        }
    }

    static final List<Demo> DEMOS = Arrays.asList(
            new Demo("create-task", "Database", "Create task", "cloud-demo-create-task-android", "Insert a task for the signed-in consumer.", "cloud_demo_tasks_android", Action.CREATE_TASK),
            new Demo("list-tasks", "Database", "List tasks", "cloud-demo-list-tasks-android", "Read the current consumer's tasks.", "cloud_demo_tasks_android", Action.LIST_TASKS),
            new Demo("complete-task", "Database", "Complete task", "cloud-demo-complete-task-android", "Update one task with consumer ownership.", "Task id", Action.COMPLETE_TASK),
            new Demo("delete-task", "Database", "Delete task", "cloud-demo-delete-task-android", "Delete one task owned by the consumer.", "Task id + confirmation", Action.DELETE_TASK),
            new Demo("order", "Database", "Create idempotent order", "cloud-demo-create-order-android", "Create an order without duplicate idempotency keys.", "cloud_demo_orders_android", Action.CREATE_ORDER),
            new Demo("summary", "Database", "Dashboard summary", "cloud-demo-dashboard-summary-android", "Combine Database and CMS in one typed response.", "Database + CMS", Action.SUMMARY),
            new Demo("create-sample-content", "CMS", "Create sample content", "cloud-demo-publish-post-android", "Create a published CMS entry.", "Confirmation", Action.PUBLISH_POST),
            new Demo("read-posts", "CMS", "Read CMS posts", "cloud-demo-read-posts-android", "List published entries using only CMS data.", "cloud_code_demo_posts_android", Action.READ_POSTS),
            new Demo("push", "Push", "Send push notification", "cloud-demo-send-push-android", "Send a notification to all Android consumers.", "Permission + FCM", Action.PUSH),
            new Demo("inspector", "HTTP", "Inspect HTTP context", "cloud-demo-http-inspector", "Inspect method, query, body and consumer context.", "HTTP trigger", Action.INSPECTOR),
            new Demo("json-values", "HTTP", "JSON values", "cloud-demo-json-values", "Return common JSON value types.", "HTTP trigger", Action.JSON_VALUES),
            new Demo("null-contract", "HTTP", "Null contract", "cloud-demo-null-contract", "Compare raw null and an explicit value.", "HTTP trigger", Action.NULL_CONTRACT),
            new Demo("response-shapes", "HTTP", "HTTP response shapes", "cloud-demo-response-shapes", "Demonstrate statuses, body and headers.", "HTTP trigger", Action.RESPONSE_SHAPES),
            new Demo("controlled-error", "HTTP", "Controlled error", "cloud-demo-error-response", "Return a safe client error response.", "HTTP trigger", Action.CONTROLLED_ERROR),
            new Demo("timeout", "HTTP", "Backend timeout", "cloud-demo-timeout-10s", "Observe the configured function timeout.", "Function timeout = 10 s", Action.TIMEOUT),
            new Demo("runtime-context", "HTTP", "Runtime context", "cloud-demo-runtime-context", "Use environment values, secrets and logs safely.", "DEMO_REGION + DEMO_SECRET", Action.RUNTIME_CONTEXT)
    );

    static final class RequestConfig {
        final String slug;
        final HttpMethodEnum method;
        final Map<String, String> query;
        final Map<String, Object> body;

        RequestConfig(String slug, HttpMethodEnum method, Map<String, String> query, Map<String, Object> body) {
            this.slug = slug;
            this.method = method;
            this.query = query;
            this.body = body;
        }
    }

    private CloudCodeDemoCatalog() {}
}
