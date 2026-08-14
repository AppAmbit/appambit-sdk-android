package com.appambit.kotlinapp

import com.appambit.sdk.enums.HttpMethodEnum

internal enum class CloudCodeAction {
    SETUP_DATABASE, CREATE_TASK, LIST_TASKS, COMPLETE_TASK, DELETE_TASK,
    CREATE_ORDER, SUMMARY, PUBLISH_POST, READ_POSTS, PUSH, INSPECTOR,
    JSON_VALUES, NULL_CONTRACT, RESPONSE_SHAPES, CONTROLLED_ERROR,
    TIMEOUT, RUNTIME_CONTEXT
}

internal data class CloudCodeDemo(
    val id: String,
    val section: String,
    val title: String,
    val slug: String,
    val detail: String,
    val prerequisite: String,
    val action: CloudCodeAction
)

internal data class RequestConfiguration(
    val slug: String,
    val method: HttpMethodEnum,
    val query: Map<String, String>? = null,
    val body: Map<String, Any>? = null
)

internal val cloudCodeDemos = listOf(
    CloudCodeDemo("create-task", "Database", "Create task", "cloud-demo-create-task-android", "Insert a task for the signed-in consumer.", "cloud_demo_tasks_android", CloudCodeAction.CREATE_TASK),
    CloudCodeDemo("list-tasks", "Database", "List tasks", "cloud-demo-list-tasks-android", "Read the current consumer's tasks.", "cloud_demo_tasks_android", CloudCodeAction.LIST_TASKS),
    CloudCodeDemo("complete-task", "Database", "Complete task", "cloud-demo-complete-task-android", "Update one task with consumer ownership.", "Task id", CloudCodeAction.COMPLETE_TASK),
    CloudCodeDemo("delete-task", "Database", "Delete task", "cloud-demo-delete-task-android", "Delete one task owned by the consumer.", "Task id + confirmation", CloudCodeAction.DELETE_TASK),
    CloudCodeDemo("order", "Database", "Create idempotent order", "cloud-demo-create-order-android", "Create an order without duplicate idempotency keys.", "cloud_demo_orders_android", CloudCodeAction.CREATE_ORDER),
    CloudCodeDemo("summary", "Database", "Dashboard summary", "cloud-demo-dashboard-summary-android", "Combine Database and CMS in one typed response.", "Database + CMS", CloudCodeAction.SUMMARY),
    CloudCodeDemo("create-sample-content", "CMS", "Create sample content", "cloud-demo-publish-post-android", "Create a published CMS entry.", "Confirmation", CloudCodeAction.PUBLISH_POST),
    CloudCodeDemo("read-posts", "CMS", "Read CMS posts", "cloud-demo-read-posts-android", "List published entries using only CMS data.", "cloud_code_demo_posts_android", CloudCodeAction.READ_POSTS),
    CloudCodeDemo("push", "Push", "Send push notification", "cloud-demo-send-push-android", "Send a notification to all Android consumers.", "Permission + FCM", CloudCodeAction.PUSH),
    CloudCodeDemo("inspector", "HTTP", "Inspect HTTP context", "cloud-demo-http-inspector", "Inspect method, query, body and consumer context.", "HTTP trigger", CloudCodeAction.INSPECTOR),
    CloudCodeDemo("json-values", "HTTP", "JSON values", "cloud-demo-json-values", "Return common JSON value types.", "HTTP trigger", CloudCodeAction.JSON_VALUES),
    CloudCodeDemo("null-contract", "HTTP", "Null contract", "cloud-demo-null-contract", "Compare raw null and an explicit value.", "HTTP trigger", CloudCodeAction.NULL_CONTRACT),
    CloudCodeDemo("response-shapes", "HTTP", "HTTP response shapes", "cloud-demo-response-shapes", "Demonstrate statuses, body and headers.", "HTTP trigger", CloudCodeAction.RESPONSE_SHAPES),
    CloudCodeDemo("controlled-error", "HTTP", "Controlled error", "cloud-demo-error-response", "Return a safe client error response.", "HTTP trigger", CloudCodeAction.CONTROLLED_ERROR),
    CloudCodeDemo("timeout", "HTTP", "Backend timeout", "cloud-demo-timeout-10s", "Observe the configured function timeout.", "Function timeout = 10 s", CloudCodeAction.TIMEOUT),
    CloudCodeDemo("runtime-context", "HTTP", "Runtime context", "cloud-demo-runtime-context", "Use environment values, secrets and logs safely.", "DEMO_REGION + DEMO_SECRET", CloudCodeAction.RUNTIME_CONTEXT)
)

internal val setupDatabaseDemo = CloudCodeDemo(
    "setup-database",
    "Database",
    "Setup database",
    "cloud-demo-setup-database-android",
    "Create the tables used by the Database examples without destroying data.",
    "Existing linked Database",
    CloudCodeAction.SETUP_DATABASE
)
