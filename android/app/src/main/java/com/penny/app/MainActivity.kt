package com.penny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.penny.app.ui.PennyApp

/** Hosts the migrated app (main.dart's MaterialApp), wired to fake services. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // dark system-bar icons on the light Penny background
        setContent { PennyApp(AppGraph.services) }
    }
}
