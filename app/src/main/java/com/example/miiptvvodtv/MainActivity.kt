package com.example.miiptvvodtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.miiptvvodtv.ui.screens.MainScreen
import com.example.miiptvvodtv.ui.theme.MiIPTVVODTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiIPTVVODTVTheme {
                MainScreen()
            }
        }
    }
}
