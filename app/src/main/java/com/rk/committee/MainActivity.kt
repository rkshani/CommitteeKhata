package com.rk.committee

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.rk.committee.data.prefs.AppPrefs
import com.rk.committee.ui.navigation.AppNavHost
import com.rk.committee.ui.theme.CommitteeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light mode only: ignore the system dark theme everywhere.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(LIGHT_NAV_SCRIM, LIGHT_NAV_SCRIM),
        )
        val showLanguagePicker = !prefs.isLanguageChosen
        setContent {
            CommitteeTheme {
                AppNavHost(startOnLanguage = showLanguagePicker)
            }
        }
    }
}

// Same translucent white scrim that enableEdgeToEdge uses for light 3-button navigation.
private val LIGHT_NAV_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
