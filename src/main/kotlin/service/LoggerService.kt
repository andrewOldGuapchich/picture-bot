package com.andrew.tg.service

import com.andrew.tg.config.Configuration
import java.io.File
import java.time.LocalDateTime

class LoggerService<T> (
    private val clazz: Class<T>
) {
    private val logFilePath = Configuration.getProperty("logging.path")
    private val file = File(logFilePath)
    private val applicationLevelLog = LogMessageLevel.valueOf(Configuration.getProperty("logging.level"))

    fun info(text: String) {
        if(applicationLevelLog == LogMessageLevel.INFO || applicationLevelLog == LogMessageLevel.DEBUG) {
            val logLine = "${LocalDateTime.now()} [${LogMessageLevel.INFO}] [${clazz.name}]: $text \n"
            writeLogMessage(logLine)
        }
    }

    fun debug(text: String) {
        if(applicationLevelLog == LogMessageLevel.DEBUG) {
            val logLine = "${LocalDateTime.now()} [${LogMessageLevel.DEBUG}] [${clazz.name}]: $text \n"
            writeLogMessage(logLine)
        }
    }

    fun error(text: String) {
        val logLine = "${LocalDateTime.now()} [${LogMessageLevel.ERROR}] [${clazz.name}]: $text \n"
        writeLogMessage(logLine)
    }

    private fun writeLogMessage(logLine: String) {
        try {
            file.appendText(logLine)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class LogMessageLevel {
    INFO,
    ERROR,
    DEBUG
}