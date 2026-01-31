package com.appambit.kotlinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appambit.sdk.RemoteConfig

@Composable
fun RemoteConfigActivity() {
    var data: String? by remember { mutableStateOf(RemoteConfig.getString("data")) }
    var showBanner by remember { mutableStateOf(RemoteConfig.getBoolean("banner")) }
    var discountValue by remember { mutableIntStateOf(RemoteConfig.getNumber("discount")) }

    fun refreshData() {
        data = RemoteConfig.getString("data")
        showBanner = RemoteConfig.getBoolean("banner")
        discountValue = RemoteConfig.getNumber("discount")
    }

    LaunchedEffect(Unit) {
        RemoteConfig.fetch().then { success ->
            if (success) {
                println("Remote Config fetch successful")
            } else {
                println("Remote Config fetch failed")
            }
            refreshData()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (showBanner) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF66A0A))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "NEW FEATURE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFC34700))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Welcome to the Future",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Discover what we have prepared for you in this new update enabled by Remote Config.",
                            color = Color(0xFFE3F2FD),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "MESSAGE OF THE DAY",
                        color = Color(0xFF757575),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    data?.let {
                        Text(
                            text = it,
                            color = Color(0xFF212121),
                            fontSize = 18.sp
                        )
                    }
                }
            }

            if (discountValue > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "SPECIAL OFFER",
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Get your discount now!",
                                color = Color(0xFF1B5E20),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$discountValue% OFF",
                            color = Color(0xFF2E7D32),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
