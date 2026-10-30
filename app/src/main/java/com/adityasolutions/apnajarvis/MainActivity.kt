package com.adityasolutions.apnajarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var keyBox: EditText
    private lateinit var promptBox: EditText
    private lateinit var answerBox: TextView
    private lateinit var tts: TextToSpeech
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("jarvis_settings", MODE_PRIVATE) }
    private val model = "gemini-2.5-flash"
    private val micRequest = 81

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildScreen()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101114.toInt())
        }
        val title = TextView(this).apply {
            text = "APNA JARVIS"
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(0xFFE8D8B5.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "AI Assistant • Developer Studio"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(0xFFBDBDBD.toInt())
        }
        keyBox = EditText(this).apply {
            hint = "Gemini API key (saved only on this phone)"
            setSingleLine(true)
            setTextColor(-1)
            setHintTextColor(0xFF999999.toInt())
            setText(prefs.getString("api_key", ""))
        }
        val saveKey = Button(this).apply {
            text = "SAVE API KEY"
            setOnClickListener {
                val k = keyBox.text.toString().trim()
                if (k.isBlank()) toast("API key paste karo")
                else {
                    prefs.edit().putString("api_key", k).apply()
                    toast("API key is phone par save ho gayi")
                }
            }
        }
        promptBox = EditText(this).apply {
            hint = "Jarvis, kya kaam karna hai?"
            minLines = 2
            setTextColor(-1)
            setHintTextColor(0xFF999999.toInt())
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val speak = Button(this).apply {
            text = "🎙 VOICE"
            setOnClickListener { startVoiceInput() }
        }
        val ask = Button(this).apply {
            text = "ASK AI"
            setOnClickListener { sendToAi(false) }
        }
        row.addView(speak, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(ask, LinearLayout.LayoutParams(0, -2, 1f))
        val buildWeb = Button(this).apply {
            text = "BUILD 3D WEBSITE"
            setOnClickListener { sendToAi(true) }
        }
        answerBox = TextView(this).apply {
            text = "Namaste! Main Apna JARVIS hoon.\\nAPI key save karke mujhse baat karo."
            textSize = 16f
            setTextColor(-1)
            setPadding(8, 18, 8, 18)
        }
        page.addView(title)
        page.addView(subtitle)
        page.addView(keyBox)
        page.addView(saveKey)
        page.addView(promptBox)
        page.addView(row)
        page.addView(buildWeb)
        page.addView(answerBox)
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS ko command dein")
            }
            startActivityForResult(intent, 82)
        } catch (_: Exception) { toast("Phone speech recognition available nahi hai") }
    }

    @Deprecated("Legacy activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 82 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            promptBox.setText(spoken)
            if (spoken.isNotBlank()) sendToAi(false)
        }
    }

    private fun sendToAi(makeWebsite: Boolean) {
        val prompt = promptBox.text.toString().trim()
        if (prompt.isBlank()) { toast("Pehle apna kaam likho ya Voice dabao"); return }
        val key = prefs.getString("api_key", keyBox.text.toString().trim()).orEmpty()
        if (key.isBlank()) { toast("Gemini API key save karo"); return }

        val local = handlePhoneCommand(prompt)
        if (local != null) {
            answerBox.text = local
            speak(local)
            return
        }

        answerBox.text = if (makeWebsite) "JARVIS: Website code bana raha hoon..." else "JARVIS: AI se jawab la raha hoon..."
        worker.execute {
            try {
                val instruction = if (makeWebsite) {
                    "You are an expert web developer. Create a complete, polished, responsive single-file HTML website. Use CSS and Three.js from a public CDN for tasteful interactive 3D visuals, with graceful fallback if WebGL is unavailable. Include all CSS and JS inline. Return ONLY valid HTML, no markdown fences. User request: $prompt"
                } else {
                    "You are Apna JARVIS, a helpful Hindi/Hinglish voice assistant. Answer clearly and practically in the user's language. Never claim you performed a phone action unless the app actually did it. If asked to create code, provide useful code. User: $prompt"
                }
                val result = callGemini(key, instruction)
                runOnUiThread {
                    if (makeWebsite) {
                        val html = result.replace(Regex("^```html\\s*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("\\s*```$"), "")
                        val file = java.io.File(filesDir, "jarvis_website.html")
                        file.writeText(html)
                        showWebsite(html)
                        answerBox.text = "Website code ready. Preview neeche khul raha hai. File: ${file.name}"
                        speak("Website ka pehla version taiyar hai. Preview khol raha hoon.")
                    } else {
                        answerBox.text = result
                        speak(result.take(900))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    answerBox.text = "AI error: ${e.message ?: "unknown error"}\\nAPI key, internet aur model access check karo."
                    toast("AI request fail hui")
                }
            }
        }
    }

    private fun callGemini(apiKey: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.6).put("maxOutputTokens", 7000))
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(response)
        if (code !in 200..299) {
            val message = json.optJSONObject("error")?.optString("message") ?: "HTTP $code"
            throw Exception(message)
        }
        return json.getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    }

    private fun handlePhoneCommand(text: String): String? {
        val q = text.lowercase(Locale.ROOT)
        return when {
            q.contains("youtube") && (q.contains("open") || q.contains("खोल") || q.contains("chala")) -> {
                launchPackage("com.google.android.youtube"); "YouTube kholne ki koshish ki."
            }
            q.contains("whatsapp") && (q.contains("open") || q.contains("खोल")) -> {
                launchPackage("com.whatsapp"); "WhatsApp kholne ki koshish ki."
            }
            q.contains("google") && (q.contains("open") || q.contains("खोल")) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Google khol raha hoon."
            }
            q.contains("time") || q.contains("समय") -> "Abhi samay ${java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())} hai."
            else -> null
        }
    }

    private fun launchPackage(pkg: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) startActivity(launch) else toast("Yeh app phone mein install nahi hai")
    }

    private fun showWebsite(html: String) {
        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        }
        val dialog = android.app.Dialog(this)
        dialog.setContentView(web)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.show()
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale("hi", "IN") }
    override fun onDestroy() {
        worker.shutdown()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
