package com.example.myapplication.data

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedText(
    val en: String,
    val kn: String,
    val hi: String
) {
    fun get(language: String): String {
        return when (language.lowercase()) {
            "kn" -> kn
            "hi" -> hi
            else -> en
        }
    }
}

@Serializable
data class QuizOption(
    val id: Int,
    val text: LocalizedText
)

@Serializable
data class QuizQuestion(
    val id: Int,
    val question: LocalizedText,
    val options: List<QuizOption>,
    val correctOptionId: Int
)

@Serializable
data class Hero(
    val id: Int,
    val name: LocalizedText,
    val district: LocalizedText,
    val story: LocalizedText,
    val imageUrl: String,
    val audioUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val quiz: List<QuizQuestion>? = null
)
