package com.kinpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kinpulse.app.ui.KinPulseRoot
import com.kinpulse.app.ui.NotConfiguredScreen
import com.kinpulse.app.ui.theme.KinPulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KinPulseApp).container
        setContent {
            KinPulseTheme {
                if (container == null) NotConfiguredScreen() else KinPulseRoot(container)
            }
        }
    }
}
