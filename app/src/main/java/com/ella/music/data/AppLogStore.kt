package com.ella.music.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppLogEntry(
    val time: Long,
    val level: String,
    val tag: String,
    val message: String,
    val throwable: String? = null
)

object AppLogStore {
    private const val FILE_NAME = "ella_logs.tsv"
    private const val MAX_LINES = 500
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun info(context: Context, tag: String, message: String) {
        append(context.applicationContext, AppLogEntry(System.currentTimeMillis(), "INFO", tag, message))
    }

    fun crash(context: Context, threadName: String, throwable: Throwable) {
        append(
            context.applicationContext,
            AppLogEntry(
                time = System.currentTimeMillis(),
                level = "CRASH",
                tag = threadName,
                message = throwable.message ?: throwable.javaClass.name,
                throwable = throwable.stackTraceToString()
            )
        )
    }

    fun read(context: Context): List<AppLogEntry> = synchronized(lock) {
        val file = logFile(context)
        if (!file.exists()) return@synchronized emptyList()
        file.readLines()
            .mapNotNull(::decode)
            .asReversed()
    }

    fun clear(context: Context) = synchronized(lock) {
        val file = logFile(context)
        if (file.exists()) file.delete()
    }

    fun formatTime(time: Long): String = synchronized(timeFormat) {
        timeFormat.format(Date(time))
    }

    private fun append(context: Context, entry: AppLogEntry) = synchronized(lock) {
        val file = logFile(context)
        val lines = if (file.exists()) file.readLines().takeLast(MAX_LINES - 1) else emptyList()
        file.parentFile?.mkdirs()
        file.writeText((lines + encode(entry)).joinToString(separator = "\n"))
    }

    private fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun encode(entry: AppLogEntry): String = listOf(
        entry.time.toString(),
        entry.level,
        entry.tag,
        entry.message,
        entry.throwable.orEmpty()
    ).joinToString("\t") { it.escape() }

    private fun decode(line: String): AppLogEntry? {
        val parts = line.split('\t').map { it.unescape() }
        if (parts.size < 5) return null
        return AppLogEntry(
            time = parts[0].toLongOrNull() ?: return null,
            level = parts[1],
            tag = parts[2],
            message = parts[3],
            throwable = parts[4].takeIf { it.isNotBlank() }
        )
    }

    private fun String.escape(): String = replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun String.unescape(): String {
        val result = StringBuilder(length)
        var escaping = false
        for (char in this) {
            if (escaping) {
                result.append(
                    when (char) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> char
                    }
                )
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else {
                result.append(char)
            }
        }
        if (escaping) result.append('\\')
        return result.toString()
    }
}
