package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.data.Hero
import com.example.myapplication.data.LocalizedText
import com.example.myapplication.ui.theme.TempleGold
import com.example.myapplication.viewmodel.Language
import com.example.myapplication.viewmodel.StoryViewModel

@Composable
fun StoryViewerScreen(
    viewModel: StoryViewModel,
    initialStoryId: Int,
    onStartQuiz: (Hero) -> Unit,
) {
    val heroes by viewModel.heroes.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val isNarrating by viewModel.isNarrating.collectAsState()
    val highlightRange by viewModel.narrationHighlightRange.collectAsState()

    StoryViewerContent(
        heroes = heroes,
        language = language,
        isNarrating = isNarrating,
        highlightRange = highlightRange,
        initialStoryId = initialStoryId,
        onStartNarration = { viewModel.startNarration(it) },
        onStopNarration = { viewModel.stopNarration() },
        onStartQuiz = onStartQuiz,
        onStoryPageSeen = { viewModel.markStoryRead(it) },
    )
}

@Composable
fun StoryViewerContent(
    heroes: List<Hero>,
    language: Language,
    isNarrating: Boolean,
    highlightRange: IntRange?,
    initialStoryId: Int,
    onStartNarration: (Hero) -> Unit,
    onStopNarration: () -> Unit,
    onStartQuiz: (Hero) -> Unit,
    onStoryPageSeen: (Int) -> Unit,
) {
    if (heroes.isEmpty()) return

    val langCode = when (language) {
        Language.EN -> "en"
        Language.KN -> "kn"
    }

    val initialIndex = heroes.indexOfFirst { it.id == initialStoryId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { heroes.size })

    LaunchedEffect(pagerState.currentPage, heroes) {
        val hero = heroes.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        onStoryPageSeen(hero.id)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val hero = heroes[page]
        val context = LocalContext.current
        val storyText = hero.story.get(langCode)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Box {
                AsyncImage(
                    model = hero.imageUrl,
                    contentDescription = hero.name.get(langCode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentScale = ContentScale.Crop,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = hero.name.get(langCode),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = hero.district.get(langCode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            RowActions(
                isNarrating = isNarrating,
                canOpenMaps = hero.latitude != null && hero.longitude != null,
                onNarrateClick = {
                    if (isNarrating) onStopNarration() else onStartNarration(hero)
                },
                onMapsClick = {
                    openGoogleMapsForStatue(
                        context = context,
                        latitude = hero.latitude!!,
                        longitude = hero.longitude!!,
                    )
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            val highlightBg = TempleGold.copy(alpha = 0.55f)
            val highlightFg = MaterialTheme.colorScheme.onSurface

            val annotatedStory = buildAnnotatedString {
                val range = highlightRange
                if (range == null || range.first < 0 || range.last >= storyText.length) {
                    append(storyText)
                } else {
                    if (range.first > 0) append(storyText.substring(0, range.first))
                    withStyle(
                        SpanStyle(
                            background = highlightBg,
                            color = highlightFg,
                        ),
                    ) {
                        append(storyText.substring(range.first, range.last + 1))
                    }
                    if (range.last + 1 < storyText.length) {
                        append(storyText.substring(range.last + 1))
                    }
                }
            }

            Text(
                text = annotatedStory,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (!hero.quiz.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onStartQuiz(hero) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(stringResource(R.string.start_quiz))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowActions(
    isNarrating: Boolean,
    canOpenMaps: Boolean,
    onNarrateClick: () -> Unit,
    onMapsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilledTonalButton(
            onClick = onNarrateClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isNarrating) Icons.Default.Stop else Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = if (isNarrating) stringResource(R.string.stop_narration) else stringResource(R.string.narrate))
            }
        }

        if (canOpenMaps) {
            OutlinedButton(
                onClick = onMapsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = stringResource(R.string.statue_finder))
                }
            }
        }
    }
}

private fun openGoogleMapsForStatue(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
) {
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
private fun StoryViewerScreenPreview() {
    StoryViewerContent(
        heroes = listOf(
            Hero(
                id = 1,
                name = LocalizedText("Kittur Rani Chennamma", "ಕಿತ್ತೂರು ರಾಣಿ ಚೆನ್ನಮ್ಮ", "hi"),
                district = LocalizedText("Belagavi", "ಬೆಳಗಾವಿ", "hi"),
                story = LocalizedText("The queen led a brave fight.", "ಕಥೆ", "hi"),
                imageUrl = "",
                latitude = 15.6,
                longitude = 74.8,
            ),
        ),
        language = Language.EN,
        isNarrating = false,
        highlightRange = 4..10,
        initialStoryId = 1,
        onStartNarration = {},
        onStopNarration = {},
        onStartQuiz = {},
        onStoryPageSeen = {},
    )
}
