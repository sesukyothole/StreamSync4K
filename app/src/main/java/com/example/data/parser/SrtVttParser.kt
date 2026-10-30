package com.example.data.parser

import com.example.data.model.SubtitleCue
import java.util.regex.Pattern

object SrtVttParser {

    private val TIME_ARROW_REGEX = Pattern.compile(
        "(\\d{1,2}:)?(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(\\d{1,2}:)?(\\d{2}):(\\d{2})[,.](\\d{3})"
    )
    private val HTML_TAG_REGEX = Pattern.compile("<[^>]*>")

    fun parse(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("WEBVTT", ignoreCase = true) || line.startsWith("NOTE", ignoreCase = true)) {
                i++
                continue
            }

            // Check if this line is an index (pure number)
            var timeLine = line
            if (line.all { it.isDigit() } && i + 1 < lines.size) {
                timeLine = lines[i + 1].trim()
                i++
            }

            val matcher = TIME_ARROW_REGEX.matcher(timeLine)
            if (matcher.find()) {
                val startMs = parseTimestamp(
                    matcher.group(1),
                    matcher.group(2) ?: "00",
                    matcher.group(3) ?: "00",
                    matcher.group(4) ?: "000"
                )
                val endMs = parseTimestamp(
                    matcher.group(5),
                    matcher.group(6) ?: "00",
                    matcher.group(7) ?: "00",
                    matcher.group(8) ?: "000"
                )

                i++
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].trim().isNotEmpty()) {
                    val textLine = lines[i].trim()
                    // Stop if encountering another timestamp line
                    if (TIME_ARROW_REGEX.matcher(textLine).find()) {
                        i--
                        break
                    }
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(cleanFormatting(textLine))
                    i++
                }

                val cleanedText = textBuilder.toString().trim()
                if (cleanedText.isNotEmpty() && endMs > startMs) {
                    cues.add(SubtitleCue(startMs, endMs, cleanedText))
                }
            }
            i++
        }

        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTimestamp(hoursStr: String?, minutesStr: String, secondsStr: String, millisStr: String): Long {
        val hours = hoursStr?.replace(":", "")?.toLongOrNull() ?: 0L
        val minutes = minutesStr.toLongOrNull() ?: 0L
        val seconds = secondsStr.toLongOrNull() ?: 0L
        val millis = millisStr.toLongOrNull() ?: 0L
        return hours * 3600000L + minutes * 60000L + seconds * 1000L + millis
    }

    private fun cleanFormatting(text: String): String {
        return HTML_TAG_REGEX.matcher(text).replaceAll("").trim()
    }

    fun getActiveCue(cues: List<SubtitleCue>, currentPositionMs: Long, offsetMs: Long): SubtitleCue? {
        if (cues.isEmpty()) return null
        // Subtitle sync offset:
        // If subtitle is late (lagging behind video audio), user increases offset (+ms) to show it sooner,
        // so adjusted position = currentPositionMs + offsetMs.
        val adjustedPosition = currentPositionMs + offsetMs
        return cues.firstOrNull { adjustedPosition in it.startTimeMs..it.endTimeMs }
    }
}
