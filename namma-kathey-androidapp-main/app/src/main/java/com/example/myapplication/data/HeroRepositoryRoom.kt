package com.example.myapplication.data

import com.example.myapplication.data.db.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HeroRepositoryRoom(private val database: AppDatabase) : HeroRepository {
    override fun observeHeroes(): Flow<List<Hero>> =
        database.heroDao().observeAll().map { list -> list.map { it.toHero() } }

    override suspend fun getHeroById(id: Int): Hero? =
        database.heroDao().getById(id)?.toHero()
}
