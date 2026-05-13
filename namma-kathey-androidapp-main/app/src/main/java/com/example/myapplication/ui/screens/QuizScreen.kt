package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.viewmodel.Language
import com.example.myapplication.viewmodel.StoryViewModel

@Composable
fun QuizScreen(
    viewModel: StoryViewModel,
    onFinish: () -> Unit,
) {
    val language by viewModel.currentLanguage.collectAsState()
    val hero by viewModel.currentQuizHero.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.quizScore.collectAsState()
    val finished by viewModel.quizFinished.collectAsState()

    val langCode = when (language) {
        Language.EN -> "en"
        Language.KN -> "kn"
    }

    if (hero == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.quiz_none_selected))
        }
        return
    }

    val quiz = hero?.quiz ?: emptyList()
    val quizHero = hero

    LaunchedEffect(finished, quizHero?.id) {
        if (finished && quizHero != null) {
            viewModel.onQuizCompleted(quizHero.id, score, quiz.size)
        }
    }

    if (finished) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.quiz_finished),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.score, score, quiz.size),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.resetQuiz()
                    onFinish()
                },
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.finish))
            }
        }
    } else if (currentQuestionIndex < quiz.size) {
        val question = quiz[currentQuestionIndex]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = hero?.name?.get(langCode) ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = (currentQuestionIndex + 1).toFloat() / quiz.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            Text(
                text = question.question.get(langCode),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            question.options.forEach { option ->
                Button(
                    onClick = { viewModel.answerQuestion(option.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Text(option.text.get(langCode))
                }
            }
        }
    }
}
