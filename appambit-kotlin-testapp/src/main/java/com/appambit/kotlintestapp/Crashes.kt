package com.appambit.kotlintestapp

import android.content.Context
import android.util.Log
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
import com.appambit.kotlintestapp.utils.StorageServiceTest
import com.appambit.kotlintestapp.utils.dialogUtils
import com.appambit.sdk.Analytics
import com.appambit.sdk.Crashes
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.models.logs.ExceptionInfo
import com.appambit.sdk.utils.DateUtils
import com.appambit.sdk.utils.InternetConnection
import com.appambit.sdk.utils.JsonConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random
import java.util.TimeZone
import java.util.UUID

lateinit var storableApp1: StorageServiceTest
private val TAG = MainActivity::class.java.simpleName

@Composable
fun Crashes() {
    val context = LocalContext.current
    var userId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var userEmail by remember { mutableStateOf("test@gmail.com") }
    var customLog by remember { mutableStateOf("Test Log Message") }
    val scrollState = rememberScrollState()
    storableApp1 = StorageServiceTest(context)
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
                    onClassInfoLogError(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send ClassInfo LogError")
            }

            Button(
                onClick = {
                    onGenerate30DailyErrors(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily errors")
            }

            Button(
                onClick = {
                    onGenerate30DailyCrashes(context)
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

fun btnDidCrashInLastSession(context : Context) {
    val didCrash = Crashes.didCrashInLastSession(context)
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
    Crashes.LogError(context, customLog, null, context.javaClass.name, null, null, 0, null)
    dialogUtils(context, "Info", "LogError sent")
}

fun onDefaultLogError(context : Context) {
    Crashes.LogError(context, "Test Log Error", null, context.javaClass.name, null, null, 0, null)
    dialogUtils(context, "Info", "LogError sent")
}

fun onExceptionLogError(context : Context) {
    val exception = Exception()
    val properties: MutableMap<String?, String?> = HashMap()
    properties.put("user_id", "1")
    Crashes.LogError(context, exception, "Test Exception Log Error", properties)
    dialogUtils(context, "Info", "Test Exception LogError sent")
}

fun onClassInfoLogError(context : Context) {
    val className = context.javaClass.name
    val properties: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()
    properties.put("user_id", "1")
    Crashes.LogError(context, "Test ClassInfo Log Error", properties, className, null, null, 0, null)
    dialogUtils(context, "Info", "LogError sent")
}

fun onGenerate30DailyErrors(context: Context) {
    if (InternetConnection.hasInternetConnection(context)) {
        dialogUtils(context, "Info", "Turn off internet and try again")
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        for (index in 1..30) {
            val sessionId = UUID.randomUUID()
            val errorDate = DateUtils.getDateDaysAgo(30 - index)

            val sessionData = SessionData().apply {
                id = sessionId
                sessionType = SessionType.START
                timestamp = errorDate
            }

            try {
                storableApp1.putSessionData(sessionData)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting start session", e)
                continue
            }

            delay(10)

            Crashes.LogError(
                context,
                "Test 30 Last Days Errors",
                null, null, null, null, 0,
                errorDate
            )

            delay(150)

            launch {
                storableApp1.updateLogSessionId(sessionId.toString())
            }

            try {
                val randomOffset = Random().nextInt(60 * 60 * 1000).toLong()
                val endSessionData = SessionData().apply {
                    id = sessionId
                    sessionType = SessionType.END
                    timestamp = Date(errorDate.time + randomOffset)
                }
                storableApp1.putSessionData(endSessionData)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting end session", e)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            dialogUtils(context, "Info", "Logs generated, turn on internet")
        }
    }
}

fun onGenerate30DailyCrashes(context: Context) {
    if (InternetConnection.hasInternetConnection(context)) {
        dialogUtils(context, "Info", "Turn off internet and try again")
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        val exception = NullPointerException()
        for (index in 1..30) {
            val sessionData = SessionData()
            val sessionId = UUID.randomUUID()
            sessionData.id = sessionId
            sessionData.sessionType = SessionType.START
            val crashDate = DateUtils.getDateDaysAgo(30 - index)
            sessionData.timestamp = crashDate

            try {
                storableApp1.putSessionData(sessionData)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting start session", e)
                continue
            }

            val info = ExceptionInfo.fromException(context, exception)
            info.createdAt = crashDate
            info.sessionId = sessionId.toString()

            try {
                val crashJson = JsonConvertUtils.toJson(info)

                val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val formattedDate = sdf.format(crashDate)

                val fileName = "crash_" + formattedDate + "_" + index + ".json"

                val crashFile = File(context.filesDir, fileName)
                Log.d(TAG, "Crash file saved to: " + crashFile.absolutePath)

                FileWriter(crashFile).use { writer ->
                    writer.write(crashJson)
                }
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Error saving crash file", e)
            }

            delay(150)

            launch {
                storableApp1.updateLogSessionId(sessionId.toString())
            }

            try {
                val randomOffset = Random().nextInt(60 * 60 * 1000).toLong()
                val endSessionData = SessionData().apply {
                    id = sessionId
                    sessionType = SessionType.END
                    timestamp = Date(crashDate.time + randomOffset)
                }
                storableApp1.putSessionData(endSessionData)
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting end session", e)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            dialogUtils(context, "Info", "Logs generated, turn on internet")
        }
    }
}

fun onThrowNewCrash() {
    throw NullPointerException()
}

fun onGenerateTestCrash() {
    Crashes.generateTestCrash()
}