package com.example.myapplication.auth

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.myapplication.BuildConfig
import com.example.myapplication.viewmodel.Language

/**
 * Swaps launcher [android.app.Activity] aliases so the home-screen icon matches app language.
 * Customize [res/mipmap-anydpi-v26/ic_launcher_en.xml] vs [ic_launcher_kn.xml] (different foreground drawable).
 */
object AppIconAliasManager {

    /** Called when toggling KN / EN in-app. */
    fun applyForLanguage(context: Context, language: Language) {
        syncIcon(context, useEnglishLauncher = language == Language.EN)
    }

    /** @param useEnglishLauncher When true, English alias is visible in the launcher drawer. */
    fun syncIcon(context: Context, useEnglishLauncher: Boolean) {
        val pkg = context.packageName
        val enComponent = ComponentName(pkg, "com.example.myapplication.LauncherIconEn")
        val knComponent = ComponentName(pkg, "com.example.myapplication.LauncherIconKn")
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            knComponent,
            if (useEnglishLauncher) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
            enComponent,
            if (useEnglishLauncher) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** After reading saved UI language — keeps icon consistent across cold starts (non-guest installs). */
    fun syncOnColdStart(context: Context, savedLanguageUsesEnglish: Boolean) {
        syncIcon(context, useEnglishLauncher = savedLanguageUsesEnglish)
    }
}
