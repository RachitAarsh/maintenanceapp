package com.aarsh.maintenanceapp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aarsh.maintenanceapp.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000),
        label = "Alpha Animation"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_nfl_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alphaAnim.value)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "NFL IT Maintenance",
                color = Color(0xFF03A9F4),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(alphaAnim.value)
            )
            
            Text(
                text = "Your IT Support Partner",
                color = Color(0xFF03A9F4).copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
} 