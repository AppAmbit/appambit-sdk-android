package com.appambit.kotlintestapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.appambit.kotlintestapp.utils.dialogUtils
import com.appambit.sdk.Analytics
import com.appambit.sdk.Crashes
import com.appambit.sdk.PushNotifications
import java.util.UUID

@Composable
fun Crashes() {
    val context = LocalContext.current
    val activity = LocalContext.current as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for existing crash buttons
    var userId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var userEmail by remember { mutableStateOf("test@gmail.com") }
    var customLog by remember { mutableStateOf("Test Log Message") }
    val scrollState = rememberScrollState()

    // State for Push Notification Button
    var hasPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var isEnabled by remember { mutableStateOf(PushNotifications.isNotificationsEnabled(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    // This effect will run whenever the lifecycle owner changes or the app resumes.
    // It's used to refresh the button state if the user changes permissions from the system settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasNotificationPermission(context)
                isEnabled = PushNotifications.isNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pushButtonText = if (hasPermission) {
        if (isEnabled) "Disable Notifications" else "Enable Notifications"
    } else {
        "Allow Notifications"
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .align(Alignment.Center)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Push Notification Button
            Button(
                onClick = {
                    if (hasPermission) {
                        // If we have permission, the button toggles the enabled state.
                        val newState = !isEnabled
                        PushNotifications.setNotificationsEnabled(context, newState)
                        isEnabled = newState
                        dialogMessage = "Notifications have been ${if (newState) "enabled" else "disabled"}."
                        showDialog = true
                    } else {
                        // If we don't have permission, the button requests it.
                        PushNotifications.requestNotificationPermission(activity, object : PushNotifications.PermissionListener {
                            override fun onPermissionResult(granted: Boolean) {
                                hasPermission = granted
                                if (granted) {
                                    PushNotifications.setNotificationsEnabled(context, true)
                                    isEnabled = true
                                    dialogMessage = "Notifications have been enabled."
                                } else {
                                    dialogMessage = "Permission denied. Notifications cannot be enabled."
                                }
                                showDialog = true
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(text = pushButtonText)
            }

            // Existing Crash Buttons
            Button(
                onClick = { btnDidCrashInLastSession(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Did the app crash during your last session?")
            }

            // ... (rest of the buttons and UI elements from the original file) ...
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text(text = "User id") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    singleLine = true
                )

                Button(
                    onClick = { onUserIdSet(context, userId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = "Change user id")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text(text = "User email") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    singleLine = true
                )

                Button(
                    onClick = { onUserEmailSet(context, userEmail) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = "Change user email")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = customLog,
                    onValueChange = { customLog = it },
                    label = { Text(text = "User email") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                    singleLine = true
                )

                Button(
                    onClick = { onCustomLogError(context, customLog) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = "Send Custom LogError")
                }
            }

            Button(
                onClick = { onDefaultLogError(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Default LogError")
            }

            Button(
                onClick = { onExceptionLogError(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Exception LogError")
            }

            Button(
                enabled = false,
                onClick = { onGenerate30DailyErrors() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily errors")
            }

            Button(
                enabled = false,
                onClick = { onGenerate30DailyCrashes() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily crashes")
            }

            Button(
                onClick = { onThrowNewCrash() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Throw new Crash")
            }

            Button(
                onClick = { onGenerateTestCrash() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate Test Crash")
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Notification Status") },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun btnDidCrashInLastSession(context: Context) {
    val didCrash = Crashes.didCrashInLastSession()
    val message = if (didCrash) "Application crashed in the last session" else "Application did not crash in the last session"
    dialogUtils(context, "Crash", message)
}

fun onUserIdSet(context : Context, userId: String) {
    Analytics.setUserId(userId)
    dialogUtils(context, "Info", "User email changed")
}

fun onUserEmailSet(context : Context, userEmail: String) {
    Analytics.setUserEmail(userEmail)
    dialogUtils(context, "Info", "User email changed")
}

fun onCustomLogError(context : Context, customLog: String) {
    Crashes.logError(customLog, null)
    dialogUtils(context, "Info", "LogError sent")
}

fun onDefaultLogError(context : Context) {
    Crashes.logError("Test Log Error", null)
    dialogUtils(context, "Info", "LogError sent")
}

fun onExceptionLogError(context : Context) {
    val exception = Exception()
    val properties: MutableMap<String?, String?> = HashMap()
    properties.put("user_id", "1")
    Crashes.logError(exception, properties)
    dialogUtils(context, "Info", "Test Exception LogError sent")
}

fun onGenerate30DailyErrors() {

}

fun onGenerate30DailyCrashes() {

}

fun onThrowNewCrash() {
    throw NullPointerException()
}

fun onGenerateTestCrash() {
    Crashes.generateTestCrash()
}
