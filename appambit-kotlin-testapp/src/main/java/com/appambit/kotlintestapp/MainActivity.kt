package com.appambit.kotlintestapp

import android.os.Bundle
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
import com.appambit.kotlintestapp.Analytics as AnalyticsScreen
import com.appambit.kotlintestapp.Crashes as CrashesScreen
import com.appambit.pushnotifications.AppAmbitPushNotifications
import com.appambit.sdk.Analytics
import com.appambit.sdk.AppAmbit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Push Notifications
        AppAmbitPushNotifications.start(applicationContext)
        AppAmbitPushNotifications.requestNotificationPermission(this)

        setContent {
            //Analytics.enableManualSession()
            AppAmbit.start(this, "<YOUR-APPKEY>")
            BottomBar()
        }
    }

    @Composable
    fun BottomBar() {
        val navController = rememberNavController()
        val items = listOf("Crashes", "Analytics")

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
            }
        }
    }
}