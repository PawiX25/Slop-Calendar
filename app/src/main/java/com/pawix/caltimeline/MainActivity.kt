package com.pawix.caltimeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pawix.caltimeline.ui.HomeScreen
import com.pawix.caltimeline.ui.theme.CalendarTimelineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTimelineTheme {
                HomeScreen()
            }
        }
    }
}
