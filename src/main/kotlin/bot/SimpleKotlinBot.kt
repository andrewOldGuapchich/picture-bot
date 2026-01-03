package com.andrew.tg.bot

import com.andrew.tg.config.Configuration
import com.andrew.tg.service.PictureService
import com.andrew.tg.service.Status
import com.andrew.tg.service.SubscriberService
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.*
import kotlin.concurrent.schedule

class SimpleKotlinBot : TelegramLongPollingBot() {
    private val subscriberService = SubscriberService()
    private val botToken: String = Configuration.getTelegramToken()
    private val pictureService = PictureService()
    private lateinit var photoTimer: Timer
    init {
        startAutoSending()
    }

    @Deprecated("Deprecated in Java")
    override fun getBotToken(): String = botToken
    override fun getBotUsername(): String = "andrewOldGuapchich_bot"

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) return

        val chatId = update.message.chatId.toString()
        val text = update.message.text

        when (text) {
            "/start" -> handleStartCommand(chatId)
            "/stop" -> handleStopCommand(chatId)
            "/status" -> sendStatus(chatId)
            "/now" -> sendPhotoNow(chatId)
            "/help" -> sendHelp(chatId)
            else -> sendUnknownCommand(chatId)
        }
    }

    private fun handleStartCommand(chatId: String) {
        if(subscriberService.existUser(chatId)) {
            sendMessage(chatId, "ℹ️ Вы уже подписаны на рассылку фото!\n" +
                    "Следующее фото будет через 10 минут.\n" +
                    "Используйте /now чтобы получить фото сейчас.")
        } else {
            when (subscriberService.addUser(chatId)) {
                Status.OK -> {
                    println("User $chatId added")
                    sendWelcomeMessage(chatId)
                    sendPhotoWithDelay(chatId, 2000)
                }
                Status.ERROR -> println("Error!")
            }
        }
    }

    private fun handleStopCommand(chatId: String) {
        if(!subscriberService.existUser(chatId)) {
            sendMessage(chatId, "ℹ️ Вы не были подписаны на рассылку.")
        } else {
            when (subscriberService.deleteUser(chatId)) {
                Status.OK -> {
                    sendMessage(chatId, "❌ Вы отписались от рассылки фото.\n" +
                            "Чтобы снова получать фото, отправьте /start")
                    println("Пользователь отписался: $chatId")
                }
                Status.ERROR -> sendMessage(chatId, "ℹ️ Вы не были подписаны на рассылку.")
            }
        }
    }

    private fun sendWelcomeMessage(chatId: String) {
        val welcomeText = """
            🌟 Добро пожаловать в AutoPhotoBot! 🌟
            
            Я бот, который будет радовать вас красивыми фотографиями!
            
            📸 Начиная с этого момента, я буду присылать вам
            случайные фото каждые 10 минут.
            
            ⏰ Первое фото придет через несколько секунд...
            
            Доступные команды:
            /stop - Отписаться от рассылки
            /status - Статус подписки
            /now - Получить фото немедленно
            /help - Помощь
            
            Приятного просмотра! 😊
        """.trimIndent()

        sendMessage(chatId, welcomeText)
    }

    private fun startAutoSending() {
        println("Запуск таймера автоматической отправки фото...")
        photoTimer = Timer(true)
        photoTimer.schedule(60 * 1000L, 10 * 60 * 1000L) {
            sendPhotosToAllSubscribers()
        }
        println("Таймер запущен. Интервал: 10 минут")
    }

    private fun sendPhotosToAllSubscribers() {
        if (subscriberService.allUser().isEmpty()) {
            println("Время отправки, но нет подписчиков")
            return
        }
        val photoUrl = pictureService.getPictureUrl()
        val caption = getRandomCaption()
        println("Начинаю рассылку для ${subscriberService.allUser().size} подписчиков...")

        subscriberService.allUser().forEach { chatId ->
            try {
                Thread.sleep(100)
                sendPhotoToChat(chatId, photoUrl, caption)
            } catch (e: Exception) {
                println("Ошибка отправки в чат $chatId: ${e.message}")
                if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) {
                    when (subscriberService.deleteUser(chatId)) {
                        Status.OK -> println("Чат $chatId удален (бот заблокирован)")
                        Status.ERROR -> println("Ошибка при удалении чата!")
                    }
                }
            }
        }
        println("Рассылка завершена")
    }

    private fun sendPhotoToChat(chatId: String, photoUrl: String, caption: String = "") {
        try {
            val photo = InputFile(photoUrl)

            val sendPhoto = SendPhoto()
            sendPhoto.chatId = chatId
            sendPhoto.photo = photo
            sendPhoto.caption = caption

            execute(sendPhoto)
            println("Фото отправлено в чат $chatId")

        } catch (e: Exception) {
            throw e
        }
    }

    private fun sendPhotoWithDelay(chatId: String, delay: Long) {
        Timer().schedule(delay) {
            try {
                val photoUrl = pictureService.getPictureUrl()
                sendPhotoToChat(chatId, photoUrl, "Ваше первое фото! 🎉")
                sendMessage(chatId, "✅ Отлично! Следующее фото будет через 10 минут.")
            } catch (e: Exception) {
                sendMessage(chatId, "❌ Не удалось отправить первое фото. Попробуйте позже.")
            }
        }
    }

    private fun sendPhotoNow(chatId: String) {
        if (!subscriberService.existUser(chatId)) {
            sendMessage(chatId, "ℹ️ Сначала подпишитесь на рассылку командой /start")
            return
        }

        try {
            val photoUrl = pictureService.getPictureUrl()
            sendPhotoToChat(chatId, photoUrl, "Специально для вас! ⭐")
        } catch (e: Exception) {
            sendMessage(chatId, "❌ Ошибка при отправке фото: ${e.message}")
        }
    }

    private fun sendStatus(chatId: String) {
        val isSubscribed = subscriberService.existUser(chatId)
        val status = if (isSubscribed) "✅ Подписан" else "❌ Не подписан"
        val totalSubscribers = subscriberService.allUser().size

        val statusText = """
            📊 Статус подписки
            
            $status
            Всего подписчиков: $totalSubscribers
            
            ${if (isSubscribed) "Следующее фото будет через 10 минут" else "Отправьте /start для подписки"}
            
            Команды:
            /start - Подписаться
            /stop - Отписаться
            /now - Фото сейчас
            /help - Помощь
        """.trimIndent()

        sendMessage(chatId, statusText)
    }

    private fun sendHelp(chatId: String) {
        val helpText = """
            🤖 AutoPhotoBot - Помощь
            
            Я автоматически отправляю красивые фото каждые 10 минут.
            
            Основные команды:
            /start - Начать получать фото (подписаться)
            /stop - Прекратить получать фото (отписаться)
            /status - Проверить статус подписки
            /now - Получить фото немедленно
            /help - Эта справка
            
            📸 Фото берутся из открытых источников
            ⏰ Интервал отправки: 10 минут
            🔄 Бот работает 24/7
            
            Наслаждайтесь красивыми фото! ✨
        """.trimIndent()

        sendMessage(chatId, helpText)
    }

    private fun sendUnknownCommand(chatId: String) {
        sendMessage(chatId, "🤔 Неизвестная команда. Используйте /help для списка команд.")
    }

    private fun getRandomCaption(): String {
        val captions = listOf(
            "Красивое фото для вас! 📸",
            "Как вам это изображение? 😊",
            "Немного красоты в ваш день! 🌟",
            "Пусть это фото поднимет настроение! 😄",
            "Вот, что я нашел для вас! 🎯",
            "Прекрасный момент! ✨"
        )
        return captions.random()
    }

    private fun sendMessage(chatId: String, text: String) {
        try {
            val message = SendMessage()
            message.chatId = chatId
            message.text = text
            execute(message)
        } catch (e: Exception) {
            println("Ошибка отправки сообщения: ${e.message}")
        }
    }

    fun shutdown() {
        photoTimer.cancel()
        println("⏹️ Таймер остановлен")
    }
}