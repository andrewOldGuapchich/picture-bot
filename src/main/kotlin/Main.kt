package com.andrew.tg

import com.andrew.tg.bot.SimpleKotlinBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main() {
    println("Запуск Kotlin Telegram бота...")
    try {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        botsApi.registerBot(SimpleKotlinBot())
        println("Бот успешно запущен и готов к работе!")
    } catch (e: Exception) {
        println("Ошибка запуска бота: ${e.message}")
        e.printStackTrace()
    }
}