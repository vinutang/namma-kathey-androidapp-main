package com.example.myapplication.data

import kotlinx.coroutines.flow.Flow

interface HeroRepository {
    fun observeHeroes(): Flow<List<Hero>>
    suspend fun getHeroById(id: Int): Hero?
}
