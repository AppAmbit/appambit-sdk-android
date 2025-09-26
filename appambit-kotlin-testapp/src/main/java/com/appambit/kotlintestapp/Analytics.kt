package com.appambit.kotlintestapp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appambit.kotlintestapp.utils.StorageServiceTest
import com.appambit.kotlintestapp.utils.dialogUtils
import com.appambit.sdk.Analytics
import com.appambit.sdk.Crashes
import com.appambit.sdk.enums.SessionType
import com.appambit.sdk.models.analytics.SessionData
import com.appambit.sdk.utils.DateUtils
import com.appambit.sdk.utils.InternetConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Random
import java.util.UUID

lateinit var storableApp2: StorageServiceTest
private val TAG = MainActivity::class.java.simpleName

@Composable
fun Analytics() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    storableApp2 = StorageServiceTest(context)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .align(Alignment.Center)
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    onStartSession()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Start Session")
            }

            Button(
                onClick = {
                    onEndSession()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "End Session")
            }

            Button(
                onClick = {
                    onGenerateLast30DailySessions(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Generate the last 30 daily sessions")
            }

            Button(
                onClick = {
                    onInvalidateToken()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Invalidate token")
            }

            Button(
                onClick = {
                    onTokenRefresh(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Token refresh test")
            }

            Button(
                onClick = {
                    onSendEventWProperty(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send 'Button Clicked' Event w/ property")
            }

            Button(
                onClick = {
                    onSendDefaultEventWProperty(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Default Event w/ property")
            }

            Button(
                onClick = {
                    onSendMax300LengthEvent(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Max-300-Length Event")
            }

            Button(
                onClick = {
                    onSend20MaxPropertiesEvent(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send 20-Max-Properties Event")
            }

            Button(
                onClick = {
                    onSend30DailyEvents(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send 30 Daily Events")
            }

            Button(
                onClick = {
                    onSend220BatchEvents(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Send Batch 220 Events")
            }

            Button(
                onClick = {
                    onChangeToSecondActivity(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp, vertical = 4.dp)
            ) {
                Text(text = "Change to Second Activity")
            }

        }
    }
}

fun onStartSession() {
    try {
        Analytics.startSession()
    } catch (e: Exception) {
        Log.e(TAG, "Error during log creation: " + e.message)
    }
}

fun onEndSession() {
    try {
        Analytics.endSession()
    } catch (e: Exception) {
        Log.e(TAG, "Error during log creation: " + e.message)
    }
}

fun onGenerateLast30DailySessions(context: Context) {
    if (InternetConnection.hasInternetConnection(context)) {
        dialogUtils(context, "Info", "Turn off internet and try again")
        return
    }

    for (index in 1..30) {
        val sessionData = SessionData()
        val sessionId = UUID.randomUUID()

        sessionData.id = sessionId
        sessionData.sessionType = SessionType.START
        val sessionDate = DateUtils.getDateDaysAgo(30 - index)
        sessionData.timestamp = sessionDate

        try {
            storableApp2.putSessionData(sessionData)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error inserting start session", e)
            continue
        }

        try {
            val finalIndex = index
            val random = Random()
            val randomOffset = random.nextInt(60 * 60 * 1000).toLong()
            storableApp2.putSessionData(object : SessionData() {
                init {
                    id = UUID.randomUUID()
                    sessionType = SessionType.END
                    timestamp = Date(
                        DateUtils.getDateDaysAgo(30 - finalIndex).time + randomOffset
                    )
                }
            })
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error inserting end session", e)
        }
    }
    dialogUtils(context, "Info", "Turn off and Turn on internet to send the sessions.")
}

fun onInvalidateToken() {
    Analytics.clearToken()
}

fun onTokenRefresh(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        Analytics.clearToken()

        val properties = mutableMapOf<String, String>()
        properties["user_id"] = "1"

        val logJobs = List(5) {
            async {
                Crashes.LogError(
                    context,
                    "Sending 5 errors after an invalid token",
                    properties,
                    context::class.java.name,
                    null,
                    null,
                    0,
                    DateUtils.getUtcNow()
                )
            }
        }
        logJobs.awaitAll()

        Analytics.clearToken()

        val eventJobs = List(5) {
            async {
                val eventData = mutableMapOf<String, String>()
                eventData["Test Token"] = "5 events sent"
                Analytics.trackEvent(
                    "Sending 5 events after an invalid token",
                    eventData,
                    null
                )
            }
        }
        eventJobs.awaitAll()

        withContext(Dispatchers.Main) {
            dialogUtils(context, "Info", "5 events and errors sent")
        }
    }
}

fun onSendEventWProperty(context: Context) {
    try {
        val map: MutableMap<String?, String?> = HashMap()
        map.put("Count", "41")
        Analytics.trackEvent("ButtonClicked", map)
        Toast.makeText(context, "OnClick event generated", Toast.LENGTH_SHORT).show()
    } catch (e: java.lang.Exception) {
        Log.e(TAG, "Error during log creation: " + e.message)
    }
}

fun onSendDefaultEventWProperty(context: Context) {
    Analytics.generateTestEvent()
    Toast.makeText(context, "Event generated", Toast.LENGTH_SHORT).show()
}

fun onSendMax300LengthEvent(context: Context) {
    val properties: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()
    val characters300 =
        "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
    val characters302 =
        "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"

    properties.put(characters300, characters300)
    properties.put(characters302, characters302)

    Analytics.trackEvent(characters300, properties, null)
    Toast.makeText(context, "1 event generated", Toast.LENGTH_SHORT).show()
}

fun onSend20MaxPropertiesEvent(context: Context) {
    val properties: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()
    properties.put("01", "01")
    properties.put("02", "02")
    properties.put("03", "03")
    properties.put("04", "04")
    properties.put("05", "05")
    properties.put("06", "06")
    properties.put("07", "07")
    properties.put("08", "08")
    properties.put("09", "09")
    properties.put("10", "10")
    properties.put("11", "11")
    properties.put("12", "12")
    properties.put("13", "13")
    properties.put("14", "14")
    properties.put("15", "15")
    properties.put("16", "16")
    properties.put("17", "17")
    properties.put("18", "18")
    properties.put("19", "19")
    properties.put("20", "20")
    properties.put("21", "21")
    properties.put("22", "22")
    properties.put("23", "23")
    properties.put("24", "24")
    properties.put("25", "25") //25

    Analytics.trackEvent("TestMaxProperties", properties, null)
    Toast.makeText(context, "1 event generated", Toast.LENGTH_SHORT).show()
}

fun onSend30DailyEvents(context: Context) {
    if (InternetConnection.hasInternetConnection(context)) {
        dialogUtils(context, "Info", "Turn off internet and try again")
        return
    }

    for (index in 0..29) {
        val sessionData = SessionData()
        val sessionId = UUID.randomUUID()

        sessionData.id = sessionId
        sessionData.sessionType = SessionType.START
        val eventDate = DateUtils.getDateDaysAgo(30 - index)
        sessionData.timestamp = eventDate

        try {
            storableApp2.putSessionData(sessionData)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error inserting start session", e)
            continue
        }

        val properties: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()
        properties.put("30 Daily events", "Event")

        Analytics.trackEvent("30 Daily events", properties, eventDate)

        try {
            Thread.sleep(100)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error during log creation: " + e.message)
        }

        storableApp2.updateEventSessionId(sessionId.toString())

        try {
            val finalIndex = index
            val random = Random()
            val randomOffset = random.nextInt(60 * 60 * 1000).toLong()
            storableApp2.putSessionData(object : SessionData() {
                init {
                    id = UUID.randomUUID()
                    sessionType = SessionType.END
                    timestamp = Date(
                        DateUtils.getDateDaysAgo(30 - finalIndex).time + randomOffset
                    )
                }
            })
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error inserting end session", e)
        }
    }
    dialogUtils(context, "Info", "30 events generated, turn on internet to send them")
}

fun onSend220BatchEvents(context: Context) {
    if (InternetConnection.hasInternetConnection(context)) {
        dialogUtils(context, "Info", "Turn off internet and try again")
        return
    }

    val properties: MutableMap<String?, String?> = java.util.HashMap<String?, String?>()

    repeat(220) {
        properties.put("property1", "value1")
        Analytics.trackEvent("Events 220", properties, null)
    }

    dialogUtils(context, "Info", "220 events generated, turn on internet to send them")
}

fun onChangeToSecondActivity(context: Context) {
    context.startActivity(Intent(context, SecondActivity::class.java))
}