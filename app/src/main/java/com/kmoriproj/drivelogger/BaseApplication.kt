package com.kmoriproj.drivelogger

import android.app.Application
import android.os.Environment
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord

@HiltAndroidApp
class BaseApplication: Application() {

    companion object {
        var log_handler: java.util.logging.FileHandler? = null

        fun flush_log() {
            log_handler?.close()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("OvO", "before setup")

        val dirFile = destinationFolder
        val file = File(dirFile,"logFile.txt")
        log_handler = java.util.logging.FileHandler(file.absolutePath, 5* 1024 * 1024/*5Mb*/, 1, true)
            .also {
                it.formatter = AndroidLogFormatter(filePath = file.absolutePath)
                it.level = java.util.logging.Level.FINE
            }
        val rootLogger = java.util.logging.LogManager.getLogManager().getLogger("")
            .also {
                it.level = java.util.logging.Level.FINE
                it.addHandler(log_handler)
            }
        Log.i("OvO", "after setup")

    }

    val applicationScope = GlobalScope

    val destinationFolder: File
        get() {
            val parent =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absoluteFile
            val destinationFolder = File(parent, "DriveLogger")
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs()
                destinationFolder.mkdir()
            }
            return destinationFolder
        }
}

class AndroidLogFormatter(val filePath: String = "", var tagPrefix: String = "") : Formatter() {

    override fun format(record: LogRecord): String {
        val tag = record.getTag(tagPrefix)
        val date = record.getDate()
        val level = record.getLogCatLevel()
        val message = record.getLogCatMessage()
        return "$date $level$tag: $message\n"
    }
}

fun LogRecord.getTag(tagPrefix: String): String {
    val name = loggerName
    val maxLength = 30
    val tag = tagPrefix + (if (name.length > maxLength) name.substring(name.length - maxLength) else name)
    return tag
}

fun LogRecord.getDate(): String? {
    return Date(millis).formatedBy("yyyy-MM-dd HH:mm:ss.SSS")
}

fun Date?.formatedBy(dateFormat: String): String? {
    val date = this
    date ?: return null
    val writeFormat = SimpleDateFormat(dateFormat, Locale.getDefault()) //MM в HH:mm
    return writeFormat.format(date)
}

fun LogRecord.getLogCatMessage(): String {
    var message = message

    if (thrown != null) {
        message += Log.getStackTraceString(thrown)
    }
    return message
}

fun Int.getAndroidLevel(): Int {
    return when {
        this >= Level.SEVERE.intValue() -> { // SEVERE
            Log.ERROR
        }
        this >= Level.WARNING.intValue() -> { // WARNING
            Log.WARN
        }
        this >= Level.INFO.intValue() -> { // INFO
            Log.INFO
        }
        else -> {
            Log.DEBUG
        }
    }
}

fun LogRecord.getLogCatLevel(): String {
    return when (level.intValue().getAndroidLevel()) {
        Log.ERROR -> { // SEVERE
            "E/"
        }
        Log.WARN -> { // WARNING
            "W/"
        }
        Log.INFO -> { // INFO
            "I/"
        }
        Log.DEBUG -> {
            "D/"
        }
        else -> {
            "D/"
        }
    }
}

fun getLoggerLevel(level: Int): Level {
    return when (level) {
        Log.ERROR -> { // SEVERE
            Level.SEVERE
        }
        Log.WARN -> { // WARNING
            Level.WARNING
        }
        Log.INFO -> { // INFO
            Level.INFO
        }
        Log.DEBUG -> {
            Level.FINE
        }
        else -> {
            Level.FINEST
        }
    }
}