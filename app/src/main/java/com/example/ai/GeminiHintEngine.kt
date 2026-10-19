package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Generates Socratic hints (never full solutions) for DPP questions a student
 * has been stuck on for more than 3 minutes.
 *
 * Security measures implemented:
 *  - The Gemini key stays in BuildConfig (injected from the local .env at build
 *    time; never committed to git since .env is gitignored).
 *  - Rate limiting: max [MAX_HINTS_PER_HOUR] requests per hour, shared app-wide
 *    via a mutex-protected sliding window.
 *  - User input is length-capped and control-characters are stripped before it
 *    is embedded in the prompt (Prompt-injection / XSS hardening).
 *  - Logs never contain the API key, the request URL or raw response bodies;
 *    only sanitized status codes are logged.
 */
class GeminiHintEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String get() = try {
        BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
        ""
    }

    private fun isKeyConfigured(): Boolean =
        apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    data class HintResult(
        val hint: String?,
        val failureReason: String? = null
    )

    suspend fun generateHint(
        chapterName: String,
        lectureTitle: String,
        questionText: String
    ): HintResult = withContext(Dispatchers.IO) {
        if (!isKeyConfigured()) {
            return@withContext HintResult(
                hint = null,
                failureReason = "AI key set nahi hai — Gemini API key app settings (.env) mein configure karo."
            )
        }
        if (!HintRateLimiter.tryAcquire()) {
            return@withContext HintResult(
                hint = null,
                failureReason = "Aaj bahut zyada hints maang liye — 1 ghante mein max $MAX_HINTS_PER_HOUR. Thoda mehnat kar tabhi hint! 🙂"
            )
        }

        val cleanChapter = sanitize(chapterName, 120)
        val cleanLecture = sanitize(lectureTitle, 150)
        val cleanQuestion = sanitize(questionText, 600)

        try {
            val prompt = """
                You are a friendly but strict study-hint assistant for a JEE student.
                The student is solving a Daily Practice Problem (DPP) question and has
                been stuck for over 3 minutes.
                Chapter: "$cleanChapter" | Lecture: "$cleanLecture"
                Student's question / statement of the problem:
                "$cleanQuestion"

                Rules:
                - Give ONE short hint that points toward the right CONCEPT or first step.
                - NEVER give the full solution or the final answer.
                - Reply in the same language mix the student likely uses (Hinglish if question is Hinglish).
                - Max 4 sentences.
            """.trimIndent()

            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "text/plain")
                    put("maxOutputTokens", 256)
                })
            }

            val request = Request.Builder()
                // SECURITY: key travels in a header, never in the URL or query string
                .url(makeModelUrl())
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "hint request failed with status=${response.code}") // no URL, no body
                    return@use HintResult(
                        hint = null,
                        failureReason = "AI service busy (${response.code}). Thodi der mein try karo."
                    )
                }
                val text = extractText(response.body?.string().orEmpty())
                HintResult(hint = text.ifBlank { null })
            }
        } catch (e: Exception) {
            Log.e(TAG, "hint generation exception") // no exception message: may contain URL/key
            HintResult(hint = null, failureReason = "Network issue. DPP skip nahi karneka! 😄")
        }
    }

    private fun makeModelUrl(): String =
                // SECURITY: key goes in a header, never in the URL query (avoids URL logging/history leaks)
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private fun extractText(raw: String): String = try {
        val candidate = JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
        candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .optString("text", "").trim()
    } catch (e: Exception) {
        ""
    }

    private fun sanitize(raw: String, maxLen: Int): String =
        raw.filter { it.code >= 32 || it == '\n' }
            .trim()
            .take(maxLen)

    companion object {
        private const val TAG = "HintEngine"
        private const val MODEL = "gemini-2.5-flash"
        private const val MAX_HINTS_PER_HOUR = 8
    }
}

/** Sliding-window rate limiter: up to MAX_HINTS_PER_HOUR tokens, UTC-window. */
object HintRateLimiter {

    private const val MAX_HINTS_PER_HOUR = 8
    private const val WINDOW_MS = 60L * 60L * 1000L

    private val mutex = Mutex()
    private val granted = ArrayDeque<Long>()

    suspend fun tryAcquire(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        while (granted.isNotEmpty() && now - granted.first() > WINDOW_MS) {
            granted.removeFirst()
        }
        if (granted.size < MAX_HINTS_PER_HOUR) {
            granted.addLast(now)
            true
        } else false
    }
}
