package com.example.service

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Centralizes editor operations so the IME has one compatibility path for apps that
 * implement InputConnection differently.
 */
internal class ImeEditorController(
    private val connectionProvider: () -> InputConnection?,
    private val editorInfoProvider: () -> EditorInfo?
) {
    fun insertSpace() {
        val connection = connectionProvider() ?: return
        if (!connection.commitText(" ", 1)) {
            sendKeyPair(connection, KeyEvent.KEYCODE_SPACE)
        }
    }

    fun deleteBackward() {
        val connection = connectionProvider() ?: return

        val selected = runCatching { connection.getSelectedText(0) }.getOrNull()
        if (!selected.isNullOrEmpty()) {
            if (!connection.commitText("", 1)) {
                sendKeyPair(connection, KeyEvent.KEYCODE_DEL)
            }
            return
        }

        val deletedCodePoint = runCatching {
            connection.deleteSurroundingTextInCodePoints(1, 0)
        }.getOrDefault(false)
        if (deletedCodePoint) return

        val deletedCodeUnit = runCatching {
            connection.deleteSurroundingText(1, 0)
        }.getOrDefault(false)
        if (deletedCodeUnit) return

        sendKeyPair(connection, KeyEvent.KEYCODE_DEL)
    }

    fun sendEnter() {
        val connection = connectionProvider() ?: return
        val info = editorInfoProvider()
        val imeOptions = info?.imeOptions ?: EditorInfo.IME_ACTION_NONE
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0

        val actionable = !noEnterAction &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED

        if (actionable && runCatching { connection.performEditorAction(action) }.getOrDefault(false)) {
            return
        }

        val inputType = info?.inputType ?: 0
        val multiline = inputType and InputType.TYPE_CLASS_TEXT == InputType.TYPE_CLASS_TEXT &&
            inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0

        if (multiline && runCatching { connection.commitText("\n", 1) }.getOrDefault(false)) {
            return
        }

        sendKeyPair(connection, KeyEvent.KEYCODE_ENTER)
    }

    private fun sendKeyPair(connection: InputConnection, keyCode: Int) {
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
