package com.venice.brawlbot.vision

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.lowtext.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class Word(val text: String, val box: Rect)

object Ocr {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun read(bitmap: Bitmap): List<Word> = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { res ->
                val words = mutableListOf<Word>()
                for (block in res.textBlocks)
                    for (line in block.lines)
                        for (el in line.elements) words += Word(el.text, el.boundingBox ?: Rect())
                if (cont.isActive) cont.resume(words)
            }
            .addOnFailureListener { if (cont.isActive) cont.resume(emptyList()) }
    }

    fun matchBrawler(words: List<Word>, name: String): Word? {
        val norm = name.lowercase().replace(" ", "")
        return words.firstOrNull {
            val w = it.text.lowercase().replace(" ", "")
            w == norm || (w.length >= 4 && norm.length >= 4 && (w.contains(norm) || norm.contains(w)))
        }
    }

    fun containsAny(words: List<Word>, keys: List<String>): Boolean =
        words.any { w -> keys.any { w.text.uppercase().contains(it) } }

    fun firstInt(words: List<Word>): Int? =
        words.mapNotNull { it.text.filter { c -> c.isDigit() }.toIntOrNull() }
            .filter { it in 0..100000 }.maxOrNull()
}