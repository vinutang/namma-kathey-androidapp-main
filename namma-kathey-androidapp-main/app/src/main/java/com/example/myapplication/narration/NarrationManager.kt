package com.example.myapplication.narration

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Bilingual narration using Android [TextToSpeech] with optional spoken-word highlighting
 * via [UtteranceProgressListener.onRangeStart] on API 26+.
 */
class NarrationManager(context: Context) {

    private var tts: TextToSpeech? = null
    private val engineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = engineReady.asStateFlow()

    private val highlightRangeInternal = MutableStateFlow<IntRange?>(null)
    val highlightCharacterRange: StateFlow<IntRange?> = highlightRangeInternal.asStateFlow()

    private val speakingInternal = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = speakingInternal.asStateFlow()

    private var onCompleteListener: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            engineReady.value = status == TextToSpeech.SUCCESS
        }
    }

    fun setOnUtteranceCompleteListener(listener: (() -> Unit)?) {
        onCompleteListener = listener
    }

    fun speak(text: String, locale: Locale) {
        val engine = tts ?: return
        highlightRangeInternal.value = null
        speakingInternal.value = true
        engine.language = locale

        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speakingInternal.value = true
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        highlightRangeInternal.value = start..end
                    }
                }

                override fun onDone(utteranceId: String?) {
                    finishSpeaking()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    finishSpeaking()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    finishSpeaking()
                }
            },
        )

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }

        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        if (result == TextToSpeech.ERROR) {
            finishSpeaking()
        }
    }

    private fun finishSpeaking() {
        highlightRangeInternal.value = null
        speakingInternal.value = false
        onCompleteListener?.invoke()
    }

    fun stop() {
        tts?.stop()
        highlightRangeInternal.value = null
        speakingInternal.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        engineReady.value = false
    }

    companion object {
        private const val UTTERANCE_ID = "namma_kathey_narration"
    }
}
