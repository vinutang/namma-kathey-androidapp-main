package com.example.myapplication.data

import com.example.myapplication.data.db.HeroEntity
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun HeroEntity.toHero(): Hero =
    Hero(
        id = id,
        name = LocalizedText(nameEn, nameKn, nameHi),
        district = LocalizedText(districtEn, districtKn, districtHi),
        story = LocalizedText(storyEn, storyKn, storyHi),
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        latitude = latitude,
        longitude = longitude,
        quiz = if (quizJson.isBlank() || quizJson == "[]") null else json.decodeFromString(quizJson),
    )
