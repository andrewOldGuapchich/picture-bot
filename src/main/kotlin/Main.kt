package com.andrew.tg

import com.andrew.tg.bot.SimpleKotlinBot
import com.andrew.tg.service.LogMessageLevel
import com.andrew.tg.service.LoggerService
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

class Main
val logger = LoggerService(Main::class.java)
fun main() {
    logger.writeLogMessage(LogMessageLevel.INFO, "#####################################")
    logger.writeLogMessage(LogMessageLevel.INFO, "######### PICTURE-BOT START #########")
    logger.writeLogMessage(LogMessageLevel.INFO, "#####################################")
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(SimpleKotlinBot())
        logger.writeLogMessage(LogMessageLevel.INFO, "Picture-Bot started successful.")
    } catch (e: Exception) {
        logger.writeLogMessage(LogMessageLevel.INFO, "Picture-Bot started failed. ${e.message}")
    }
}
