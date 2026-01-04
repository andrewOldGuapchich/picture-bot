package com.andrew.tg.bot

import com.andrew.tg.config.Configuration
import com.andrew.tg.service.*
import kotlinx.coroutines.*
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import java.util.*
import kotlin.concurrent.schedule

class SimpleKotlinBot : TelegramLongPollingBot() {
    private val subscriberService = SubscriberService()
    private val botToken: String = Configuration.getTelegramToken()
    private val delay = Configuration.getProperty("telegram.bot.delay").toInt()
    private val pictureService = PictureService()
    private lateinit var photoTimer: Timer
    private val logger = LoggerService(SimpleKotlinBot::class.java)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val userCoroutines = mutableMapOf<String, Job>()

    init {
        logger.info( "Init bot!!!")
        userCoroutines.forEach { (_, j) ->
            j.cancel()
        }
        userCoroutines.clear()

        subscriberService.allUser().forEach { user ->
            startUserCoroutine(user)
        }
        logger.info( "Init bot successfully!")
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
            "Фото сейчас" -> sendPhotoNow(chatId)
            "Остановить рассылку" -> handleStopCommand(chatId)
            "Обо мне" -> sendHelp(chatId)
            else -> sendUnknownCommand(chatId)
        }
    }

    private fun handleStartCommand(user: String) {
        logger.info( "The 'Start' button is pressed.")
        if(subscriberService.existUser(user)) {
            sendMessageWithKeyboard(
                user,
                "ℹ️ Вы уже подписаны на рассылку фото!\n" +
                        "Следующее фото будет через 15 минут.\n" +
                        "Используйте кнопку ниже или /now чтобы получить фото сейчас."
            )
        } else {
            when (subscriberService.addUser(user)) {
                Status.OK -> {
                    sendWelcomeMessage(user)
                    sendPhotoWithDelay(user, 2000)
                    startUserCoroutine(user)
                }
                Status.ERROR -> return
            }
        }
    }

    private fun handleStopCommand(user: String) {
        logger.info( "The 'Stop' button is pressed.")
        if (!subscriberService.existUser(user)) {
            sendMessage(user, "ℹ️ Вы не были подписаны на рассылку.")
            return
        }

        userCoroutines[user]?.cancel()
        if(userCoroutines.containsKey(user))
            logger.info( "Coroutine for user $user was cancelled.")

        userCoroutines.remove(user)
        when (subscriberService.deleteUser(user)) {
            Status.OK -> {
                sendMessage(
                    user, "❌ Вы отписались от рассылки фото.\n" +
                            "Чтобы снова получать фото, отправьте /start"
                )
                logger.info( "User $user has unsubscribed from the mailing list.")
            }

            Status.ERROR -> sendMessage(user, "ℹ️ Вы не были подписаны на рассылку.")
        }
    }

    private fun sendWelcomeMessage(chatId: String) {
        val welcomeText = """
            🌟 Добро пожаловать! 🌟
            
            Я бот, который будет радовать вас красивыми фотографиями!
            
            📸 Начиная с этого момента, я буду присылать вам
            случайные фото каждые 15 минут.
            
            ⏰ Первое фото придет через несколько секунд...
            
            Используйте кнопки ниже для управления:
            
            Приятного просмотра! 😊
        """.trimIndent()

        sendMessageWithKeyboard(chatId, welcomeText)
    }

    private fun sendPhotoToChat(chatId: String, photoUrl: String, caption: String = "") {
        try {
            val photo = InputFile(photoUrl)

            val sendPhoto = SendPhoto()
            sendPhoto.chatId = chatId
            sendPhoto.photo = photo
            sendPhoto.caption = caption

            execute(sendPhoto)
            logger.info( "The photo was sent to the chat $chatId.")

        } catch (e: Exception) {
            logger.error("Error when sending a photo. ${e.message}")
            throw e
        }
    }

    private fun sendPhotoWithDelay(chatId: String, delay: Long) {
        Timer().schedule(delay) {
            try {
                val photoUrl = pictureService.getPictureUrl()
                sendPhotoToChat(chatId, photoUrl, "Ваше первое фото! 🎉")
                sendMessageWithKeyboard(chatId, "✅ Отлично! Следующее фото будет через 15 минут.")
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
            
            ${if (isSubscribed) "Следующее фото будет через 15 минут" else "Отправьте /start для подписки"}
        """.trimIndent()

        if (isSubscribed) {
            sendMessageWithKeyboard(chatId, statusText)
        } else {
            sendMessage(chatId, statusText)
        }
    }

    private fun sendHelp(chatId: String) {
        val helpText = """
            🤖 AutoPhotoBot - Помощь
            
            Я автоматически отправляю красивые фото каждые 15 минут.
            
            Основные команды:
            /start - Начать получать фото (подписаться)
            /stop - Прекратить получать фото (отписаться)
            /status - Проверить статус подписки
            /now - Получить фото немедленно
            /help - Эта справка
            
            📸 Фото берутся из открытых источников
            ⏰ Интервал отправки: 15 минут
            🔄 Бот работает 24/7
            
            Наслаждайтесь красивыми фото! ✨
        """.trimIndent()

        sendMessageWithKeyboard(chatId, helpText)
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
            logger.info( "The message was sent successfully.")
        } catch (e: Exception) {
            logger.error("Error when sending a message. ${e.message}")
        }
    }

    private fun sendMessageWithKeyboard(chatId: String, text: String) {
        try {
            val message = SendMessage()
            message.chatId = chatId
            message.text = text

            val keyboardMarkup = ReplyKeyboardMarkup()
            val keyboard = mutableListOf<KeyboardRow>()

            val row1 = KeyboardRow()
            row1.add("Фото сейчас")
            keyboard.add(row1)

            val row2 = KeyboardRow()
            row2.add("Остановить рассылку")
            row2.add("Обо мне")
            keyboard.add(row2)

            keyboardMarkup.keyboard = keyboard
            keyboardMarkup.resizeKeyboard = true
            keyboardMarkup.oneTimeKeyboard = false
            keyboardMarkup.selective = true

            message.replyMarkup = keyboardMarkup

            execute(message)
            logger.debug("The message with keyboard was sent successfully. User $chatId")
        } catch (e: Exception) {
            logger.error("Error when sending a message with keyboard. ${e.message}")
        }
    }

    private fun startUserCoroutine(user: String) {
        if(userCoroutines.containsKey(user))
            logger.info( "Stop kotlin-coroutine for user $user")
        userCoroutines[user]?.cancel()
        logger.info("Start new kotlin-coroutine for user $user")

        val job = coroutineScope.launch {
            try {
                while (isActive && subscriberService.existUser(user)) {
                    delay(delay * 60 * 1000L)

                    if(!subscriberService.existUser(user)) break

                    try {
                        val photoUrl = pictureService.getPictureUrl()
                        val caption = getRandomCaption()
                        sendPhotoToChat(user, photoUrl, caption)
                        logger.info( "Auto photo sent to $user")
                    } catch (e: Exception) {
                        logger.error("Error sending auto photo to $user: ${e.message}")

                        if (e.message?.contains("blocked") == true || e.message?.contains("403") == true) {
                            subscriberService.deleteUser(user)
                            break
                        }
                    }
                }
            } catch (e: CancellationException) {
                logger.info( "Coroutine for $user cancelled")
            } catch (e: Exception) {
                logger.error("Coroutine for $user failed: ${e.message}")
            }
        }

        userCoroutines[user] = job
    }

    fun shutdown() {
        photoTimer.cancel()
        println("⏹️ Таймер остановлен")
    }
}