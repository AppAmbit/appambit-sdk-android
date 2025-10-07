package com.appambit.kotlintestapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appambit.kotlintestapp.utils.dialogUtils
import com.appambit.sdk.Analytics
import com.appambit.sdk.Crashes
import java.util.UUID

@Composable
fun Crashes() {
    val context = LocalContext.current
    var userId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var userEmail by remember { mutableStateOf("test@gmail.com") }
    var customLog by remember { mutableStateOf("Test Log Message") }
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .align(Alignment.Center)
                .padding(top = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    btnDidCrashInLastSession(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Did the app crash during your last session?")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
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
                onClick = {
                    onDefaultLogError(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Default LogError")
            }

            Button(
                onClick = {
                    onExceptionLogError(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Exception LogError")
            }

            Button(
                onClick = {
                    onGenerate30DailyErrors()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily errors")
            }

            Button(
                onClick = {
                    onGenerate30DailyCrashes()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily crashes")
            }

            Button(
                onClick = {
                    onThrowNewCrash()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Throw new Crash")
            }

            Button(
                onClick = {
                    onGenerateTestCrash()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate Test Crash")
            }

        }
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