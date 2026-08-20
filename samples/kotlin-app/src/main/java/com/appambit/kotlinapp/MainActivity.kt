package com.appambit.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appambit.sdk.Analytics
import com.appambit.kotlinapp.Analytics as AnalyticsScreen
import com.appambit.kotlinapp.Crashes as CrashesScreen
import com.appambit.sdk.PushNotifications
import com.appambit.sdk.AppAmbit
import com.appambit.sdk.RemoteConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Analytics.enableManualSession()
        RemoteConfig.enable()
        AppAmbit.start(this, "294f7dd6-987e-493b-b13c-dfdfd0cdcd3e")

        // Initialize Push SDK on app start
        PushNotifications.start(applicationContext)

        // Handle notification taps
        PushNotifications.setOpenedListener { notification ->
            Log.d("AppAmbitSample", "[OPENED] User tapped the notification")
            Log.d("AppAmbitSample", "  Title : ${notification.title}")
            Log.d("AppAmbitSample", "  Body  : ${notification.body}")
            Log.d("AppAmbitSample", "  Data  : ${notification.data}")
        }

        // Required to dispatch the opened callback when the app was completely closed.
        PushNotifications.handleNotificationOpened(this, intent)

        setContent {
            BottomBar()
        }
    }

    @Composable
    fun BottomBar() {
        val navController = rememberNavController()
        val items = listOf("Crashes", "Analytics", "RemoteConfig", "Cms", "Database", "Cloud Code")
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            bottomBar = {
                ScrollableTabRow(
                    selectedTabIndex = items.indexOfFirst { label ->
                        currentDestination?.hierarchy?.any { it.route == label } == true
                    }.coerceAtLeast(0),
                    edgePadding = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEach { label ->
                        Tab(
                            selected = currentDestination?.hierarchy?.any { it.route == label } == true,
                            onClick = {
                                navController.navigate(label) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            text = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "Crashes",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("Crashes") {
                    CrashesScreen()
                }
                composable("Analytics") {
                    AnalyticsScreen()
                }
                composable("RemoteConfig") {
                    RemoteConfigActivity()
                }
                composable("Cms") {
                    Cms()
                }
                composable("Database") {
                    Database()
                }
                composable("Cloud Code") {
                    CloudCode()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update the activity's intent so getIntent() always returns the latest one.
        setIntent(intent)
        // Dispatch the callback when the app was already running in the background.
        PushNotifications.handleNotificationOpened(this, intent)
    }
}
