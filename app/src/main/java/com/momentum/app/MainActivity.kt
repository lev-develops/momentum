package com.momentum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.momentum.app.ui.navigation.MomentumNavHost
import com.momentum.app.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomentumTheme {
                MomentumNavHost(container = (application as MomentumApplication).container)
            }
        }
    }
}
