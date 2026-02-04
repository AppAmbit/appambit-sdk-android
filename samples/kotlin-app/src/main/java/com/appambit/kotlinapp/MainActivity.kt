package com.appambit.kotlinapp

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
        AppAmbit.start(this, "<YOUR-APPKEY>")

        // Initialize Push SDK on app start
        PushNotifications.start(applicationContext)

        RemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        RemoteConfig.fetch().then { success ->
            if (success) {
                println("Remote Config fetch successful")
            } else {
                println("Remote Config fetch failed")
            }
        }

        setContent {
            BottomBar()
        }
    }

    @Composable
    fun BottomBar() {
        val navController = rememberNavController()
        val items = listOf("Crashes", "Analytics", "RemoteConfig")

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
            }
        }
    }
}
