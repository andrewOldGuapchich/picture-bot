package com.andrew.tg.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.io.InputStream
import java.io.ObjectInputFilter.Config

object Configuration {
    private val mapper = ObjectMapper(YAMLFactory())
    private lateinit var config: Map<String, Any>

    fun getProperty(propName: String): String {
        load()
        return getNestedValue(propName) as? String
            ?: throw IllegalStateException("Prop $propName not found in application.yaml")
    }

    fun getTelegramToken(): String {
        load()
        return getNestedValue("telegram.bot.token") as? String
            ?: throw IllegalStateException("Telegram токен не найден в конфигурации")
    }

    fun getFreepikToken(): String {
        load()
        return getNestedValue("freepik.token") as? String ?: ""
    }

    fun getSubscribersFilePath(): String {
        load()
        return getNestedValue("file.subscribers.path") as? String ?: ""
    }
    fun getCategoryFilePath(): String {
        load()
        return getNestedValue("file.category.path") as? String ?: ""
    }

    //private fun load(configPath: String = "/Users/andrew_kononov/IdeaProjects/tg-bot/application.yaml") {
    private fun load(configPath: String = "/opt/application/picture-bot/application.yaml") {
        val file = File(configPath)
        config = if (file.exists()) {
            file.inputStream().use { loadFromStream(it) }
        } else {
            val resource = Config::class.java.classLoader.getResourceAsStream(configPath)
            if (resource != null) {
                loadFromStream(resource)
            } else {
                throw IllegalArgumentException("Конфигурационный файл не найден: $configPath")
            }
        }
    }

    private fun loadFromStream(inputStream: InputStream): Map<String, Any> {
        return try {
            @Suppress("UNCHECKED_CAST")
            mapper.readValue(inputStream, Map::class.java) as Map<String, Any>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun getNestedValue(path: String): Any? {
        val keys = path.split(".")
        var current: Any? = config

        for (key in keys) {
            current = when (current) {
                is Map<*, *> -> (current as Map<*, *>)[key]
                else -> return null
            }
        }

        return current
    }
}