package pt.estga.spotme.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import pt.estga.spotme.utils.LocaleHelper

class MyApp : Application() {

    companion object {
        const val PREFS_NAME = "user_prefs"
        const val DARK_MODE_KEY = "dark_mode"
        const val LANGUAGE_KEY = "language_code"

        lateinit var prefs: SharedPreferences
        var languageCode: String = "pt"
            private set

        fun setLanguageCode(newLang: String) {
            languageCode = newLang
            prefs.edit().putString(LANGUAGE_KEY, newLang).apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        languageCode = prefs.getString(LANGUAGE_KEY, "pt") ?: "pt"

        val isDarkMode = prefs.getBoolean(DARK_MODE_KEY, false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.setLocale(base, languageCode))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.setLocale(this, languageCode)
    }
}
