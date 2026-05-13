package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_progress",
    primaryKeys = ["ownerUid", "heroId"],
    foreignKeys = [
        ForeignKey(
            entity = HeroEntity::class,
            parentColumns = ["id"],
            childColumns = ["heroId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("heroId"), Index("ownerUid")],
)
data class UserProgressEntity(
    /** Firebase UID or [com.example.myapplication.auth.UserSessionStore.GUEST_UID]. */
    val ownerUid: String,
    /** Display label stored with progress rows for heritage/badge sync debugging. */
    val displayName: String,
    val heroId: Int,
    val storyRead: Boolean = false,
    val quizBadgeEarned: Boolean = false,
)
