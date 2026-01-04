package com.andrew.tg.service

import com.andrew.tg.config.Configuration
import java.io.File

class SubscriberService {
    private lateinit var subscribers: MutableList<String>
    private val path = Configuration.getSubscribersFilePath()
    private val file = File(path)
    private val logger = LoggerService(SubscriberService::class.java)

    fun addUser(user: String) = add(user)

    fun allUser(): List<String> {
        loadFile()
        return this.subscribers
    }

    fun deleteUser(user: String) = delete(user)

    fun existUser(user: String): Boolean = subscribers.contains(user)

    private fun loadFile() {
        logger.info("Starting to read the subscribers.txt file.")
        try {
            subscribers = if(file.exists() && file.length() > 0) {
                file.readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.matches(Regex("\\d+")) }
                    .toMutableList()
            } else {
                mutableListOf()
            }
            logger.debug("${subscribers.size} lines have been read.")
            logger.info("File subscribers.txt is reading successful.")
        } catch (e: Exception) {
            logger.error( "Error reading the subscribers.txt file. ${e.message}")
            subscribers = mutableListOf()
        }
    }

    private fun add(user: String): Status {
        try {
            file.appendText(user)
            file.appendText("\n")
            logger.info("User $user has been successfully added to the file.")
            return Status.OK
        } catch (e: Exception) {
            logger.error( "Error when adding user $user to the file. ${e.message}")
            return Status.ERROR
        }
    }

    private fun delete(user: String): Status {
        loadFile()
        this.subscribers.remove(user)
        clearFile()
        try {
            this.subscribers.forEach {
                file.appendText(it)
                file.appendText("\n")
            }
            logger.info("User $user has been successfully deleted to the file.")
            return Status.OK
        } catch (e: Exception) {
            logger.error( "Error when adding user $user to the file. ${e.message}")
            return Status.ERROR
        }
    }

    private fun clearFile() {
        logger.info("Start cleaning file.")
        try {
            file.writer().use {
                it.write("")
            }
            logger.info("The file has been successfully cleaned.")
        } catch (e: Exception) {
            logger.error( "Error when clearing the file. ${e.message}")
            e.printStackTrace()
        }
    }
}

enum class Status {
    OK,
    ERROR
}