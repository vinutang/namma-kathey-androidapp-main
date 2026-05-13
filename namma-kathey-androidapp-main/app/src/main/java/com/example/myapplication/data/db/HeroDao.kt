package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {
    @Query("SELECT * FROM heroes ORDER BY id ASC")
    fun observeAll(): Flow<List<HeroEntity>>

    @Query("SELECT * FROM heroes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): HeroEntity?

    @Query("SELECT COUNT(*) FROM heroes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HeroEntity>)
}
