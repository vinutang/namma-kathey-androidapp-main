package com.example.myapplication.data.db

import android.content.Context
import android.util.Log
import com.example.myapplication.data.Hero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DatabaseInitializer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun initializeIfEmpty(database: AppDatabase, context: Context) = withContext(Dispatchers.IO) {
        val heroDao = database.heroDao()
        if (heroDao.count() > 0) return@withContext

        try {
            val inputStream = context.assets.open("stories.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val heroes = StoriesSeedParser.parseToHeroes(context, jsonString)
            val entities = heroes.map { it.toEntity() }
            heroDao.insertAll(entities)
            Log.d("DatabaseInitializer", "Seeded ${entities.size} heroes from stories.json")
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Failed to seed database", e)
        }
    }

    private fun Hero.toEntity(): HeroEntity =
        HeroEntity(
            id = id,
            nameEn = name.en,
            nameKn = name.kn,
            nameHi = name.hi,
            districtEn = district.en,
            districtKn = district.kn,
            districtHi = district.hi,
            storyEn = story.en,
            storyKn = story.kn,
            storyHi = story.hi,
            imageUrl = imageUrl,
            audioUrl = audioUrl,
            latitude = latitude,
            longitude = longitude,
            quizJson = json.encodeToString(quiz ?: emptyList()),
        )
}
