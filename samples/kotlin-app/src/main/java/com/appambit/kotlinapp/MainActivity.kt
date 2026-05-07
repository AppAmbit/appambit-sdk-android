package com.appambit.kotlinapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        AppAmbit.start(this, "<YOUR-APPKEY>")

        // Initialize Push SDK on app start
        PushNotifications.start(applicationContext)

        // Handle notification taps
        PushNotifications.setOpenedNotificationListener { notification ->
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
        val items = listOf("Crashes", "Analytics", "RemoteConfig", "Cms")

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { label ->
                        NavigationBarItem(
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
                            icon = {},
                            label = { Text(label) }
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
