package com.h4ckm310n.s5w2c

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class LogItem(
    val type: Int,
    val content: String
)

object Logger {
    private const val logTag = "S5W2C"
    val items = mutableStateListOf<LogItem>()

    fun log(content: String) {
        MainScope().launch(Dispatchers.Main) {
            Log.d(logTag, content)
            items.add(LogItem(1, content))
        }
    }

    fun err(content: String) {
        MainScope().launch(Dispatchers.Main) {
            Log.e(logTag, content)
            items.add(LogItem(2, content))
        }
    }

    fun clearLogs() {
        MainScope().launch(Dispatchers.Main) {
            items.clear()
        }
    }
}