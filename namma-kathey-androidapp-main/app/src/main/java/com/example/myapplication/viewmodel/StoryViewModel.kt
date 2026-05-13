package com.example.myapplication.viewmodel

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.NammaKatheyApplication
import com.example.myapplication.auth.AppIconAliasManager
import com.example.myapplication.auth.UserSessionStore
import com.example.myapplication.data.Hero
import com.example.myapplication.data.HeroRepository
import com.example.myapplication.data.HeroRepositoryRoom
import com.example.myapplication.data.db.UserProgressEntity
import com.example.myapplication.narration.NarrationManager
import com.example.myapplication.prefs.AppPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

enum class Language {
    EN,
    KN,
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NammaKatheyApplication.database(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences(AppPrefs.FILE_UI, Application.MODE_PRIVATE)
    private val userSession: UserSessionStore = NammaKatheyApplication.userSession(application)
    private val heroRepository: HeroRepository = HeroRepositoryRoom(database)
    private val narrationManager = NarrationManager(application.applicationContext)

    private val _heroes = MutableStateFlow<List<Hero>>(emptyList())
    val heroes: StateFlow<List<Hero>> = _heroes.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString(AppPrefs.KEY_LANGUAGE_CODE, AppPrefs.LANG_EN) ==
            AppPrefs.LANG_KN
        ) {
            Language.KN
        } else {
            Language.EN
        },
    )
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    val userProgress: StateFlow<Map<Int, UserProgressEntity>> =
        userSession.owner
            .flatMapLatest { own ->
                if (own.uid.isBlank()) {
                    flowOf(emptyList())
                } else {
                    database.userProgressDao().observeForOwner(own.uid)
                }
            }
            .map { list -> list.associateBy { it.heroId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val narrationHighlightRange: StateFlow<IntRange?> =
        narrationManager.highlightCharacterRange

    private val _isNarrating = MutableStateFlow(false)
    val isNarrating: StateFlow<Boolean> = _isNarrating.asStateFlow()

    private val _currentQuizHero = MutableStateFlow<Hero?>(null)
    val currentQuizHero: StateFlow<Hero?> = _currentQuizHero.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    init {
        narrationManager.setOnUtteranceCompleteListener {
            _isNarrating.value = false
        }
        viewModelScope.launch {
            try {
                heroRepository.observeHeroes().collect { list ->
                    Log.d("StoryViewModel", "Hero flow emitted ${list.size} heroes")
                    _heroes.value = list
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error observing heroes", e)
            }
        }

        AppIconAliasManager.syncOnColdStart(
            application,
            savedLanguageUsesEnglish = (_currentLanguage.value == Language.EN),
        )
    }

    fun toggleLanguage() {
        _currentLanguage.value = when (_currentLanguage.value) {
            Language.EN -> Language.KN
            Language.KN -> Language.EN
        }
        prefs.edit().putString(
            AppPrefs.KEY_LANGUAGE_CODE,
            if (_currentLanguage.value == Language.EN) AppPrefs.LANG_EN else AppPrefs.LANG_KN,
        ).apply()
        // Removed immediate icon alias swap to prevent app from closing.
        // The icon will sync on next cold start.
    }

    fun signOutToAuth(activity: Activity) {
        userSession.signOutReturningToAuth(activity)
    }

    fun startNarration(hero: Hero) {
        val langCode = when (_currentLanguage.value) {
            Language.EN -> "en"
            Language.KN -> "kn"
        }
        val text = hero.story.get(langCode)
        val locale = when (_currentLanguage.value) {
            Language.EN -> Locale.ENGLISH
            Language.KN -> Locale("kn", "IN")
        }
        _isNarrating.value = true
        narrationManager.speak(text, locale)
    }

    fun stopNarration() {
        narrationManager.stop()
        _isNarrating.value = false
    }

    fun markStoryRead(heroId: Int) {
        viewModelScope.launch {
            val own = userSession.owner.value
            if (own.uid.isBlank()) return@launch

            val existing = database.userProgressDao().getForOwnerAndHero(own.uid, heroId)
                ?: UserProgressEntity(
                    ownerUid = own.uid,
                    displayName = own.displayName,
                    heroId = heroId,
                    storyRead = false,
                    quizBadgeEarned = false,
                )

            if (!existing.storyRead) {
                database.userProgressDao().upsert(existing.copy(storyRead = true, displayName = own.displayName))
            }
        }
    }

    fun onQuizCompleted(heroId: Int, score: Int, totalQuestions: Int) {
        if (totalQuestions <= 0) return
        viewModelScope.launch {
            val own = userSession.owner.value
            if (own.uid.isBlank()) return@launch

            val existing = database.userProgressDao().getForOwnerAndHero(own.uid, heroId) ?: return@launch

            val perfect = score == totalQuestions
            if (perfect && !existing.quizBadgeEarned) {
                database.userProgressDao().upsert(
                    existing.copy(quizBadgeEarned = true, displayName = own.displayName),
                )
            }
        }
    }

    fun startQuiz(hero: Hero) {
        _currentQuizHero.value = hero
        _currentQuestionIndex.value = 0
        _quizScore.value = 0
        _quizFinished.value = false
    }

    fun answerQuestion(optionId: Int) {
        val hero = _currentQuizHero.value ?: return
        val quiz = hero.quiz ?: return
        if (_currentQuestionIndex.value >= quiz.size) return

        val question = quiz[_currentQuestionIndex.value]
        if (question.correctOptionId == optionId) {
            _quizScore.value += 1
        }

        if (_currentQuestionIndex.value + 1 < quiz.size) {
            _currentQuestionIndex.value += 1
        } else {
            _quizFinished.value = true
        }
    }

    fun resetQuiz() {
        _currentQuizHero.value = null
        _currentQuestionIndex.value = 0
        _quizScore.value = 0
        _quizFinished.value = false
    }

    override fun onCleared() {
        super.onCleared()
        narrationManager.shutdown()
    }
}
