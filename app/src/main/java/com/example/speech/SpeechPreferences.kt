package com.example.speech

object WhisperModes {
    const val BATCH = "Batch"
    const val LIVE = "Live"

    val all: List<String> = listOf(BATCH, LIVE)

    fun normalize(value: String): String =
        if (value == LIVE) LIVE else BATCH
}
