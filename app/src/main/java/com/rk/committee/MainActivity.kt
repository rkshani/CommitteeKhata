package com.rk.committee

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.rk.committee.data.prefs.AppPrefs
import com.rk.committee.ui.navigation.AppNavHost
import com.rk.committee.ui.theme.CommitteeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val showLanguagePicker = !prefs.isLanguageChosen
        setContent {
            CommitteeTheme {
                AppNavHost(startOnLanguage = showLanguagePicker)
            }
        }
    }
}
