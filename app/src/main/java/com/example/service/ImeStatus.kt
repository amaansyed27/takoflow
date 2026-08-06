package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

object ImeStatus {
    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return manager.enabledInputMethodList.any { info ->
            info.packageName == context.packageName
        }
    }

    fun isSelected(context: Context): Boolean {
        val selected = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false

        val component = ComponentName.unflattenFromString(selected) ?: return false
        return component.packageName == context.packageName
    }
}
