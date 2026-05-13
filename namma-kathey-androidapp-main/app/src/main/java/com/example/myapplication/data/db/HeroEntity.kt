package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heroes")
data class HeroEntity(
    @PrimaryKey val id: Int,
    val nameEn: String,
    val nameKn: String,
    val nameHi: String,
    val districtEn: String,
    val districtKn: String,
    val districtHi: String,
    val storyEn: String,
    val storyKn: String,
    val storyHi: String,
    val imageUrl: String,
    val audioUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val quizJson: String,
)
