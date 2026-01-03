package com.andrew.tg.service

import com.andrew.tg.config.Configuration
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

class PictureService {
    private val imageToken = Configuration.getFreepikToken()
    private val categoryFilePath = Configuration.getCategoryFilePath()
    private val httpClient = HttpClient()

    fun getPictureUrl(): String {
        return getUrlList().random()
    }

    private fun getUrlList(): List<String> {
        val jsonBody = sendHttpRequest()
        val objectMapper = ObjectMapper()
        val rootNode: JsonNode = objectMapper.readTree(jsonBody)
        val imageUrls = mutableListOf<String>()

        val data = rootNode.path("data")

        data.forEach { dataItem ->
            val imageUrl = dataItem
                .path("image")
                .path("source")
                .path("url")
                .asText()

            if (imageUrl.isNotBlank()) {
                imageUrls.add(imageUrl)
            }
        }

        return imageUrls
    }

    private fun buildUrl(filter: Filter): String {
        return "https://api.freepik.com/v1/resources?" +
                "term=${filter.term}&" +
                "filters[content_type][photo]=${filter.count}&" +
                "filters[ai-generated][excluded]=1&" +
                "page=${filter.page}&" +
                "limit=${filter.limit}"
    }

    private fun sendHttpRequest(): String {
        val filter = Filter(
            term = getCategories().random(),
            page = (1..100).random(),
            limit = (1..50).random(),
            count = 1
        )

        val url = buildUrl(filter)
        val (status, body, error) = httpClient
            .get(url, mapOf("x-freepik-api-key" to imageToken))

        return when {
            status != 200 -> "Request error!"
            error != null -> "Request error!"
            else -> body.toString()
        }
    }

    private fun getCategories(): List<String> {
        var categories = emptyList<String>()
        try {
            val file = File(categoryFilePath)
            categories = if(file.exists() && file.length() > 0) {
                file.readLines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toMutableList()
            } else {
                listOf("cat", "dog", "otter")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return categories
    }
}

data class Filter(
    val term: String,
    val page: Number,
    val limit: Number,
    val count: Number
)