package com.appambit.kotlinapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.appambit.sdk.CloudCode
import com.appambit.sdk.PushNotifications
import com.appambit.sdk.enums.HttpMethodEnum
import com.appambit.sdk.models.cloudcode.CloudCodeError
import com.appambit.sdk.models.cloudcode.CloudCodeRequest
import com.appambit.sdk.models.cloudcode.CloudCodeResponse
import com.appambit.sdk.models.cloudcode.CloudCodeResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class CloudCodeSummary {
    var task_count: Int? = null
    var database_available: Boolean = false
    var database_tables_ready: Boolean = false
    var posts: List<Any?>? = null
    var platform: String = ""
}

@Composable
fun CloudCode() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var taskTitle by remember { mutableStateOf("Buy coffee") }
    var taskId by remember { mutableStateOf("") }
    var postUuid by remember { mutableStateOf("") }
    var publishTitle by remember { mutableStateOf("Cloud Code sample post") }
    var publishBody by remember { mutableStateOf("Published through an HTTP Cloud Function.") }
    var databaseStatus by remember { mutableStateOf("Not available") }
    var databaseAvailable by remember { mutableStateOf(false) }
    var databaseTablesReady by remember { mutableStateOf(false) }
    var cmsStatus by remember { mutableStateOf("Not available") }
    var isVerifyingBackend by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var lastResultDemoId by remember { mutableStateOf<String?>(null) }
    var resultTitle by remember { mutableStateOf("Latest result") }
    var resultText by remember { mutableStateOf("Run a function to see its response here.") }
    var resultExpanded by remember { mutableStateOf(true) }
    var pendingConfirmation by remember { mutableStateOf<CloudCodeDemo?>(null) }
    var pendingRequest by remember { mutableStateOf<CloudCodeRequest<*>?>(null) }

    fun headers() = mapOf("X-Sample-Client" to "kotlin")

    fun verifyBackend() {
        if (isVerifyingBackend) return
        isVerifyingBackend = true
        databaseStatus = "Checking..."
        cmsStatus = "Checking..."
        CloudCode.call(
            "cloud-demo-dashboard-summary-android",
            HttpMethodEnum.GET,
            null,
            null,
            headers(),
            CloudCodeSummary::class.java
        ).then { result ->
            isVerifyingBackend = false
            databaseAvailable = result.getData()?.database_available == true
            databaseTablesReady = result.getData()?.database_tables_ready == true
            databaseStatus = when {
                !databaseAvailable -> "Not available"
                databaseTablesReady -> "Tables ready"
                else -> "Available"
            }
            cmsStatus = if (result.getData()?.posts != null) "Available" else "Not available"
        }.onError {
            isVerifyingBackend = false
            databaseAvailable = false
            databaseTablesReady = false
            databaseStatus = "Not available"
            cmsStatus = "Not available"
        }
    }

    fun showResult(id: String, title: String, text: String) {
        lastResultDemoId = id
        resultTitle = title
        resultText = text
        resultExpanded = true
    }

    fun formatError(error: Throwable?, elapsed: Long): String {
        if (error is CloudCodeError && error.getCode() == CloudCodeError.Code.HTTP) {
            return "Duration: ${formatDuration(elapsed)}\nrequestId: ${error.getRequestId() ?: "none"}" +
                "\nHTTP error body: ${jsonText(error.getBody())}\nError: ${error.message}"
        }
        return "Duration: ${formatDuration(elapsed)}\nError: ${error?.message ?: "Unknown error"}"
    }

    fun requestConfiguration(demo: CloudCodeDemo): RequestConfiguration {
        val action = demo.action
        return when (action) {
            CloudCodeAction.SETUP_DATABASE -> RequestConfiguration(demo.slug, HttpMethodEnum.POST)
            CloudCodeAction.CREATE_TASK -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, body = mapOf("title" to taskTitle))
            CloudCodeAction.LIST_TASKS -> RequestConfiguration(demo.slug, HttpMethodEnum.GET, query = mapOf("limit" to "20"))
            CloudCodeAction.COMPLETE_TASK -> RequestConfiguration(demo.slug, HttpMethodEnum.PATCH, body = mapOf("task_id" to (taskId.toIntOrNull() ?: 0)))
            CloudCodeAction.DELETE_TASK -> RequestConfiguration(demo.slug, HttpMethodEnum.DELETE, body = mapOf("task_id" to (taskId.toIntOrNull() ?: 0)))
            CloudCodeAction.CREATE_ORDER -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, body = mapOf("idempotency_key" to UUID.randomUUID().toString(), "amount" to 100))
            CloudCodeAction.SUMMARY -> RequestConfiguration(demo.slug, HttpMethodEnum.GET)
            CloudCodeAction.PUBLISH_POST -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, body = mapOf("title" to publishTitle, "body" to publishBody))
            CloudCodeAction.READ_POSTS -> RequestConfiguration(demo.slug, HttpMethodEnum.GET, query = postUuid.trim().takeIf { it.isNotEmpty() }?.let { mapOf("uuid" to it) })
            CloudCodeAction.INSPECTOR -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, mapOf("source" to "kotlin"), mapOf("message" to "hello", "count" to 2))
            CloudCodeAction.JSON_VALUES -> RequestConfiguration(demo.slug, HttpMethodEnum.POST)
            CloudCodeAction.NULL_CONTRACT -> RequestConfiguration(demo.slug, HttpMethodEnum.GET)
            CloudCodeAction.RESPONSE_SHAPES -> RequestConfiguration(demo.slug, HttpMethodEnum.POST)
            CloudCodeAction.CONTROLLED_ERROR -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, body = mapOf("invalid" to true))
            CloudCodeAction.TIMEOUT -> RequestConfiguration(demo.slug, HttpMethodEnum.GET)
            CloudCodeAction.RUNTIME_CONTEXT -> RequestConfiguration(demo.slug, HttpMethodEnum.GET)
            CloudCodeAction.PUSH -> RequestConfiguration(demo.slug, HttpMethodEnum.POST, body = mapOf("title" to "Cloud Code Android demo", "body" to "Push from Kotlin sample"))
        }
    }

    lateinit var callRequest: (CloudCodeDemo) -> Unit

    fun callDemo(demo: CloudCodeDemo) {
        if (demo.action == CloudCodeAction.SETUP_DATABASE && (!databaseAvailable || databaseTablesReady)) return
        if (isRunning) return
        if ((demo.action == CloudCodeAction.COMPLETE_TASK || demo.action == CloudCodeAction.DELETE_TASK) && taskId.toIntOrNull() == null) {
            showResult(demo.id, "Input required", "Enter a numeric task id first.")
            return
        }
        if (demo.action == CloudCodeAction.PUSH) {
            if (activity == null) {
                showResult(demo.id, "Permission required", "Cloud Code Push requires a ComponentActivity host.")
                return
            }
            ensurePushReady(context, activity) { granted ->
                if (granted) callRequest(demo)
                else showResult(demo.id, "Permission required", "Notification permission is required. Enable notifications and try again.")
            }
        } else {
            callRequest(demo)
        }
    }

    callRequest = { demo ->
        isRunning = true
        lastResultDemoId = demo.id
        resultTitle = "Result · ${demo.slug}"
        resultText = "Calling ${demo.slug}..."
        resultExpanded = true
        val started = SystemClock.elapsedRealtime()
        val config = requestConfiguration(demo)
        if (demo.action == CloudCodeAction.SUMMARY) {
            val request = CloudCode.call(demo.slug, config.method, config.query, config.body, headers(), CloudCodeSummary::class.java)
            pendingRequest = request
            request.then { result ->
                isRunning = false
                val data = mapOf(
                    "database_available" to result.getData()?.database_available,
                    "database_tables_ready" to result.getData()?.database_tables_ready,
                    "task_count" to result.getData()?.task_count,
                    "posts" to (result.getData()?.posts ?: emptyList<Any?>()),
                )
                resultText = "HTTP ${result.getStatusCode()}\nDuration: ${formatDuration(SystemClock.elapsedRealtime() - started)}\nrequestId: ${result.getRequestId() ?: "none"}\nBody: ${jsonText(data)}"
            }.onError { error ->
                isRunning = false
                resultText = formatError(error, SystemClock.elapsedRealtime() - started)
            }
        } else {
            val request: CloudCodeRequest<CloudCodeResponse> = CloudCode.call(demo.slug, config.method, config.query, config.body, headers())
            pendingRequest = request
            request.then { response ->
                isRunning = false
                resultText = "HTTP ${response.getStatusCode()}\nDuration: ${formatDuration(SystemClock.elapsedRealtime() - started)}\nrequestId: ${response.getRequestId() ?: "none"}\nBody: ${jsonText(response.getData())}"
            }.onError { error ->
                isRunning = false
                resultText = formatError(error, SystemClock.elapsedRealtime() - started)
            }
        }
    }

    LaunchedEffect(Unit) { verifyBackend() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Cloud Code", style = MaterialTheme.typography.headlineSmall)
        Text("HTTP-triggered functions using the Android consumer token.")
        SetupGroup(
            "Database",
            "Create Database first",
            databaseStatus,
            isVerifyingBackend,
            0xFF2864D2.toInt(),
            setupDatabaseDemo,
            databaseAvailable && !databaseTablesReady && !isVerifyingBackend,
        ) { callDemo(it) }
        if (lastResultDemoId == setupDatabaseDemo.id) ResultCard(resultTitle, resultText, resultExpanded) { resultExpanded = !resultExpanded }
        SetupGroup("CMS", "Create Content Type first", cmsStatus, isVerifyingBackend, 0xFF7D4BB4.toInt(), null, false) {}

        listOf("Database", "CMS", "Push", "HTTP").forEach { section ->
            Text(section, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
            when (section) {
                "Database" -> {
                    OutlinedTextField(taskTitle, { taskTitle = it }, Modifier.fillMaxWidth(), label = { Text("Task title") }, singleLine = true)
                    OutlinedTextField(taskId, { taskId = it }, Modifier.fillMaxWidth(), label = { Text("Task id for update/delete") }, singleLine = true)
                }
                "CMS" -> {
                    OutlinedTextField(postUuid, { postUuid = it }, Modifier.fillMaxWidth(), label = { Text("CMS post UUID (optional)") }, singleLine = true)
                    OutlinedTextField(publishTitle, { publishTitle = it }, Modifier.fillMaxWidth(), label = { Text("Sample title") }, singleLine = true)
                    OutlinedTextField(publishBody, { publishBody = it }, Modifier.fillMaxWidth(), label = { Text("Sample body") }, minLines = 3)
                }
            }
            cloudCodeDemos.filter { it.section == section }.forEach { demo ->
                DemoCard(demo) { selected ->
                    if (selected.action == CloudCodeAction.DELETE_TASK || selected.action == CloudCodeAction.PUBLISH_POST || selected.action == CloudCodeAction.PUSH) {
                        pendingConfirmation = selected
                    } else {
                        callDemo(selected)
                    }
                }
                if (lastResultDemoId == demo.id) ResultCard(resultTitle, resultText, resultExpanded) { resultExpanded = !resultExpanded }
            }
        }
        if (isRunning) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Text("Calling Cloud Code...")
            }
        }
    }

    pendingConfirmation?.let { demo ->
        AlertDialog(
            onDismissRequest = { pendingConfirmation = null },
            title = { Text("Confirm Cloud Code action") },
            text = { Text("This calls a real backend operation. Continue only if the required service is configured.") },
            dismissButton = { TextButton({ pendingConfirmation = null }) { Text("Cancel") } },
            confirmButton = { TextButton({ pendingConfirmation = null; callDemo(demo) }) { Text("Run") } }
        )
    }
}

@Composable
private fun SetupGroup(
    title: String,
    requirement: String,
    status: String,
    checking: Boolean,
    tint: Int,
    demo: CloudCodeDemo?,
    canRun: Boolean,
    onRun: (CloudCodeDemo) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(requirement, color = Color(tint), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(status, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (checking) CircularProgressIndicator(modifier = Modifier.height(20.dp).padding(end = 8.dp))
            }
            demo?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(it.slug, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(it.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it.prerequisite, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Button(onClick = { onRun(it) }, enabled = canRun, modifier = Modifier.heightIn(min = 44.dp)) { Text("Run") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoCard(demo: CloudCodeDemo, onRun: (CloudCodeDemo) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(demo.slug, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(demo.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(demo.prerequisite, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Button(onClick = { onRun(demo) }, enabled = true, modifier = Modifier.heightIn(min = 44.dp)) { Text("Run") }
        }
    }
}

@Composable
private fun ResultCard(title: String, result: String, expanded: Boolean, onToggle: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onToggle) { Text(if (expanded) "Collapse" else "Expand") }
            }
            HorizontalDivider()
            Text(
                if (expanded) result else result.lineSequence().take(2).joinToString("\n"),
                modifier = Modifier.padding(top = 8.dp).heightIn(max = 240.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun ensurePushReady(context: Context, activity: ComponentActivity, completion: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        PushNotifications.setNotificationsEnabled(context, true)
        completion(true)
        return
    }
    PushNotifications.requestNotificationPermission(activity) { granted ->
        if (granted) PushNotifications.setNotificationsEnabled(context, true)
        completion(granted)
    }
}

private fun formatDuration(millis: Long): String = String.format(Locale.US, "%.2f s", millis / 1000.0)

private fun jsonText(value: Any?): String {
    if (value == null) return "null"
    return try {
        when (value) {
            is Map<*, *> -> JSONObject(value).toString(2)
            is List<*> -> JSONArray(value).toString(2)
            is String -> JSONObject.quote(value)
            else -> value.toString()
        }
    } catch (_: Exception) {
        value.toString()
    }
}
