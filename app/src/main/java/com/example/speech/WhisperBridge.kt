package com.example.speech

internal class WhisperBridge(modelPath: String) : AutoCloseable {
    companion object {
        init {
            System.loadLibrary("takoflow_whisper")
        }
    }

    private external fun nativeInit(modelPath: String): Long
    private external fun nativeTranscribe(contextPointer: Long, samples: FloatArray): String
    private external fun nativeFree(contextPointer: Long)

    private var contextPointer: Long = nativeInit(modelPath)

    init {
        check(contextPointer != 0L) { "Whisper could not load the selected model." }
    }

    fun transcribe(samples: FloatArray): String {
        check(contextPointer != 0L) { "Whisper engine is closed." }
        return nativeTranscribe(contextPointer, samples).trim()
    }

    override fun close() {
        if (contextPointer != 0L) {
            nativeFree(contextPointer)
            contextPointer = 0L
        }
    }
}
