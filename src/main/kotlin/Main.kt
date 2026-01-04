package com.andrew.tg

import com.andrew.tg.bot.SimpleKotlinBot
import com.andrew.tg.service.LoggerService
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

class Main
val logger = LoggerService(Main::class.java)
fun main() {
    logger.info("#####################################")
    logger.info("######### PICTURE-BOT START #########")
    logger.info("#####################################")
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(SimpleKotlinBot())
        logger.info("Picture-Bot started successful.")
    } catch (e: Exception) {
        logger.info("Picture-Bot started failed. ${e.message}")
    }
}
