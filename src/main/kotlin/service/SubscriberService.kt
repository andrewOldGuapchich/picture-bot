package com.andrew.tg.service

import com.andrew.tg.config.Configuration
import java.io.File

class SubscriberService {
    private lateinit var subscribers: MutableList<String>
    private val path = Configuration.getSubscribersFilePath()
    private val file = File(path)

    init {
        loadFile()
    }

    fun addUser(user: String) = add(user)

    fun allUser(): List<String> {
        loadFile()
        return this.subscribers
    }

    fun deleteUser(user: String) = delete(user)

    fun existUser(user: String): Boolean = subscribers.contains(user)


    private fun loadFile() {
        try {
            subscribers = if(file.exists() && file.length() > 0) {
                file.readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.matches(Regex("\\d+")) }
                    .toMutableList()
            } else {
                mutableListOf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            subscribers = mutableListOf()
        }
    }

    private fun add(user: String): Status {
        try {
            file.writer().use {
                it.append(user)
                it.append("\n")
            }
            return Status.OK
        } catch (e: Exception) {
            e.printStackTrace()
            return Status.ERROR
        }
    }

    private fun addAll(users: List<String>): Status {
        loadFile()
        this.subscribers.addAll(users)
        clearFile()
        try {
            file.writer().use {
                this.subscribers.forEach { user ->
                    it.append(user)
                    it.append("\n")
                }
            }
            return Status.OK
        } catch (e: Exception) {
            e.printStackTrace()
            return Status.ERROR
        }
    }

    private fun delete(user: String): Status {
        loadFile()
        this.subscribers.remove(user)
        clearFile()
        try {
            file.writer().use {
                this.subscribers.forEach { user ->
                    it.append(user)
                    it.append("\n")
                }
            }
            return Status.OK
        } catch (e: Exception) {
            e.printStackTrace()
            return Status.ERROR
        }
    }

    private fun clearFile() {
        try {
            file.writer().use {
                it.write("")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class Status {
    OK,
    ERROR
}