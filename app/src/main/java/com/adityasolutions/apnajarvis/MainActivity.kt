package com.adityasolutions.apnajarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var tts: TextToSpeech
    private var speech: SpeechRecognizer? = null
    private val micRequest = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        }
    }

    private fun buildUi() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 48, 28, 28)
            setBackgroundColor(0xFF101114.toInt())
        }
        val title = TextView(this).apply {
            text = "APNA JARVIS"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFFE8D8B5.toInt())
        }
        status = TextView(this).apply {
            text = "Ready • Tap the microphone to speak"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 28, 0, 24)
        }
        val listen = Button(this).apply {
            text = "🎙  Speak to JARVIS"
            setOnClickListener { listenOnce() }
        }
        output = TextView(this).apply {
            text = "Namaste! Main Apna JARVIS hoon.\nAbhi voice button dabakar command do."
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(8, 30, 8, 20)
        }
        val help = TextView(this).apply {
            text = "Phase 1: Hindi voice input/output + basic phone actions. Wake-word listening and advanced AI will be added in later phases."
            textSize = 13f
            setTextColor(0xFFB9B9B9.toInt())
            setPadding(8, 24, 8, 8)
        }
        page.addView(title)
        page.addView(status)
        page.addView(listen)
        page.addView(output)
        page.addView(help)
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun listenOnce() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition is not available on this phone."
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS ko command dein")
        }
        status.text = "Listening..."
        speech?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "Sun raha hoon..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Processing..." }
            override fun onError(error: Int) { status.text = "Voice error ($error). Dobara try karein." }
            override fun onResults(results: Bundle?) {
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                output.text = "You: $words\n\n" + handleCommand(words)
                status.text = "Ready"
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speech?.startListening(intent)
    }

    private fun handleCommand(raw: String): String {
        val q = raw.lowercase(Locale.ROOT)
        val response: String
        when {
            q.contains("youtube") -> {
                openApp("com.google.android.youtube")
                response = "YouTube khol raha hoon."
            }
            q.contains("whatsapp") -> {
                openApp("com.whatsapp")
                response = "WhatsApp khol raha hoon."
            }
            q.contains("chrome") || q.contains("google") || q.contains("search") -> {
                startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", raw))
                response = "Internet search khol raha hoon."
            }
            q.contains("time") || q.contains("समय") || q.contains("वक्त") -> {
                response = "Abhi samay ${java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())} hai."
            }
            q.contains("namaste") || q.contains("hello") || q.contains("हेलो") -> {
                response = "Namaste! Bataiye, kya kaam karna hai?"
            }
            else -> response = "Maine suna: $raw. Is command ka action agle development phase mein add karenge."
        }
        if (::tts.isInitialized) tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
        return response
    }

    private fun openApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch)
        else status.text = "App installed nahi hai."
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("hi", "IN")
    }

    override fun onDestroy() {
        speech?.destroy()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
