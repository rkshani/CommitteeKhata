package com.rk.committee.data.prefs

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    /** True after the user picks a language on first launch. */
    var isLanguageChosen: Boolean
        get() = sp.getBoolean(KEY_LANGUAGE_CHOSEN, false)
        set(value) = sp.edit { putBoolean(KEY_LANGUAGE_CHOSEN, value) }

    private companion object {
        const val KEY_LANGUAGE_CHOSEN = "language_chosen"
    }
}

/** The whole app uses only ONE language at a time: English or Urdu. */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    URDU("ur");

    fun apply() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    companion object {
        fun current(): AppLanguage {
            val lang = AppCompatDelegate.getApplicationLocales()[0]?.language
                ?: Locale.getDefault().language
            return if (lang == URDU.tag) URDU else ENGLISH
        }
    }
}
