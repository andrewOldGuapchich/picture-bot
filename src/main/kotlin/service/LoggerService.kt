package com.andrew.tg.service

import com.andrew.tg.config.Configuration
import java.io.File
import java.time.LocalDateTime

class LoggerService<T> (
    private val clazz: Class<T>
) {
    private val logFilePath = Configuration.getProperty("file.log.path")
    private val file = File(logFilePath)

    fun writeLogMessage(level: LogMessageLevel, text: String) {
        val logLine = "${LocalDateTime.now()} $level ${clazz.name}: $text \n"
        try {
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class LogMessageLevel {
    INFO,
    WARNING,
    ERROR
}