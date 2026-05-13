package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.data.Hero
import com.example.myapplication.data.karnataka.DistrictNameMatch
import com.example.myapplication.data.LocalizedText
import com.example.myapplication.ui.theme.VibrantAmber
import com.example.myapplication.ui.theme.VibrantCyan
import com.example.myapplication.ui.theme.VibrantGreen
import com.example.myapplication.ui.theme.VibrantOrange
import com.example.myapplication.ui.theme.VibrantPink
import com.example.myapplication.ui.theme.VibrantPurple
import com.example.myapplication.viewmodel.Language
import com.example.myapplication.viewmodel.StoryViewModel

@Composable
fun HeroListScreen(
    viewModel: StoryViewModel,
    districtFilterEnglish: String?,
    onHeroClick: (Int) -> Unit,
) {
    val heroes by viewModel.heroes.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    val filtered = remember(heroes, districtFilterEnglish) {
        val filter = districtFilterEnglish
            ?.let { DistrictNameMatch.normalizeWhitespace(it) }
            ?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
        if (filter == null) heroes
        else heroes.filter { hero ->
            DistrictNameMatch.equalsCanonical(hero.district.en, filter)
        }
    }

    HeroListContent(
        heroes = filtered,
        language = language,
        districtFilterEnglish = districtFilterEnglish,
        onHeroClick = onHeroClick,
        onListenClick = { viewModel.startNarration(it) },
    )
}

@Composable
fun HeroListContent(
    heroes: List<Hero>,
    language: Language,
    districtFilterEnglish: String?,
    onHeroClick: (Int) -> Unit,
    onListenClick: (Hero) -> Unit,
) {
    val cardColors = listOf(
        VibrantAmber,
        VibrantCyan,
        VibrantGreen,
        VibrantPink,
        VibrantOrange,
        VibrantPurple,
    )

    val title = when {
        districtFilterEnglish.isNullOrBlank() || districtFilterEnglish.equals("all", true) ->
            stringResource(R.string.choose_hero)

        else -> stringResource(R.string.heroes_in_district, districtFilterEnglish)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp),
        )

        if (heroes.isEmpty()) {
            Text(
                text = stringResource(R.string.no_heroes_in_district),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            itemsIndexed(heroes) { index, hero ->
                HeroCard(
                    hero = hero,
                    language = language,
                    backgroundColor = cardColors[index % cardColors.size],
                    onClick = { onHeroClick(hero.id) },
                    onListenClick = { onListenClick(hero) },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    hero: Hero,
    language: Language,
    backgroundColor: Color,
    onClick: () -> Unit,
    onListenClick: () -> Unit,
) {
    val langCode = when (language) {
        Language.EN -> "en"
        Language.KN -> "kn"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = hero.imageUrl,
                contentDescription = hero.name.get(langCode),
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hero.name.get(langCode),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = hero.district.get(langCode),
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.Black),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            IconButton(
                onClick = onListenClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, RoundedCornerShape(18.dp)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = stringResource(R.string.listen),
                        tint = backgroundColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.listen),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = backgroundColor,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeroListScreenPreview() {
    HeroListContent(
        heroes = listOf(
            Hero(
                id = 1,
                name = LocalizedText("Kittur Rani Chennamma", "ಕಿತ್ತೂರು ರಾಣಿ ಚೆನ್ನಮ್ಮ", "hi"),
                district = LocalizedText("Belagavi", "ಬೆಳಗಾವಿ", "hi"),
                story = LocalizedText("The queen...", "ಕಥೆ", "hi"),
                imageUrl = "",
            ),
        ),
        language = Language.EN,
        districtFilterEnglish = "Belagavi",
        onHeroClick = {},
        onListenClick = {},
    )
}
