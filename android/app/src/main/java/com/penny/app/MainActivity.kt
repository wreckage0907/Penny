package com.penny.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.penny.app.ui.PennyApp

/**
 * The v2.1 shell. The activity also hosts a small broadcast lever used ONLY
 * by the instrumented suite (S04 offline state):
 * `adb shell am broadcast -a com.penny.app.SET_OFFLINE --ez online false`.
 * It flips the [com.penny.app.core.FakeConnectivity] flag that the Budget
 * screen's offline banner reads — harmless with any real backend.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PennyApp(AppGraph.services) }

        val offlineReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.penny.app.SET_OFFLINE") {
                    AppGraph.services.connectivity.setOnline(intent.getBooleanExtra("online", true))
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            offlineReceiver,
            IntentFilter("com.penny.app.SET_OFFLINE"),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }
}
