package com.momentum.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.momentum.app.ui.navigation.MomentumNavHost
import com.momentum.app.ui.theme.MomentumTheme
import com.momentum.app.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = (application as MomentumApplication).container
            val themePreference by container.appPrefsDataStore.themePreferenceFlow()
                .collectAsState(initial = ThemePreference.SYSTEM)
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            MomentumTheme(darkTheme = darkTheme) {
                MomentumNavHost(container = container)
            }
        }
    }
}
