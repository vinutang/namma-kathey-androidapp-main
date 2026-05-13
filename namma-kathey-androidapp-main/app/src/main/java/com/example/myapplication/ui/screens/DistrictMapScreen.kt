package com.example.myapplication.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.Hero
import com.example.myapplication.data.karnataka.DistrictNameMatch
import com.example.myapplication.data.karnataka.KarnatakaDistrictLabels
import com.example.myapplication.data.karnataka.KarnatakaDistrictShape
import com.example.myapplication.data.karnataka.KarnatakaGeoJsonLoader
import com.example.myapplication.ui.theme.DeepSaffron
import com.example.myapplication.ui.theme.ForestGreen
import com.example.myapplication.ui.theme.StorybookDeepGreen
import com.example.myapplication.ui.theme.TempleGold
import com.example.myapplication.viewmodel.Language
import com.example.myapplication.viewmodel.StoryViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/** Fixed tint behind the map so dynamic Material colors cannot wash out district fills. */
private val MapCanvasBackground = Color(0xFFE8F5E9)

private fun buildPixelPath(ring: List<Offset>, drawW: Float, drawH: Float, padPx: Float): Path {
    val p = Path()
    if (ring.size < 3) return p
    val first = ring[0]
    p.moveTo(first.x * drawW + padPx, first.y * drawH + padPx)
    for (i in 1 until ring.size) {
        val pt = ring[i]
        p.lineTo(pt.x * drawW + padPx, pt.y * drawH + padPx)
    }
    p.close()
    return p
}

@Composable
fun DistrictMapScreen(
    viewModel: StoryViewModel,
    onDistrictSelected: (districtKeyEnglish: String) -> Unit,
) {
    val heroes by viewModel.heroes.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    DistrictMapContent(
        heroes = heroes,
        language = language,
        onDistrictSelected = onDistrictSelected,
    )
}

@Composable
fun DistrictMapContent(
    heroes: List<Hero>,
    language: Language,
    onDistrictSelected: (districtKeyEnglish: String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shapes by produceState<List<KarnatakaDistrictShape>>(
        initialValue = emptyList(),
        key1 = context.applicationContext,
    ) {
        value = withContext(Dispatchers.IO) {
            KarnatakaGeoJsonLoader.load(context.applicationContext)
        }
    }

    val pulse = rememberInfiniteTransition(label = "districtPulse")
    val pulseAlpha = pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 8.sp,
        shadow = Shadow(
            color = Color.White.copy(alpha = 0.88f),
            offset = Offset(0.5f, 0.5f),
            blurRadius = 2.5f,
        ),
    )
    val mapLabelColor = StorybookDeepGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.map_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.map_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MapCanvasBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MapCanvasBackground),
            ) {
                val padPx = with(density) { 10.dp.toPx() }
                val boxWpx = with(density) { maxWidth.toPx() }
                val boxHpx = with(density) { maxHeight.toPx() }
                val drawWBox = boxWpx - 2f * padPx
                val drawHBox = boxHpx - 2f * padPx

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(shapes, boxWpx, boxHpx, padPx) {
                            detectTapGestures { tap ->
                                if (shapes.isEmpty() || drawWBox <= 0f || drawHBox <= 0f) return@detectTapGestures
                                val norm = Offset(
                                    x = ((tap.x - padPx) / drawWBox).coerceIn(0f, 1f),
                                    y = ((tap.y - padPx) / drawHBox).coerceIn(0f, 1f),
                                )
                                KarnatakaGeoJsonLoader.districtAt(shapes, norm)?.let { d ->
                                    onDistrictSelected(d.canonicalEnglish)
                                }
                            }
                        },
                ) {
                    drawRect(color = MapCanvasBackground, size = size)

                    if (shapes.isEmpty()) return@Canvas

                    val drawW = size.width - 2f * padPx
                    val drawH = size.height - 2f * padPx
                    if (drawW <= 0f || drawH <= 0f) return@Canvas

                    val boundaryStrokePx = kotlin.math.max(2.25f.dp.toPx(), kotlin.math.min(drawW, drawH) * 0.0022f)
                    val langIsKn = language == Language.KN

                    for (d in shapes) {
                        val path = buildPixelPath(d.outerRing, drawW, drawH, padPx)
                        val hasHeroes = heroes.any { h ->
                            DistrictNameMatch.equalsCanonical(h.district.en, d.canonicalEnglish)
                        }
                        val stripe = ((d.canonicalEnglish.hashCode() ushr 1) and 1) == 0
                        val baseAlpha = when {
                            !hasHeroes -> if (stripe) 0.40f else 0.34f
                            stripe -> 0.62f
                            else -> 0.55f
                        }
                        drawPath(
                            path = path,
                            color = ForestGreen.copy(alpha = baseAlpha),
                        )
                    }

                    for (d in shapes) {
                        val path = buildPixelPath(d.outerRing, drawW, drawH, padPx)
                        drawPath(
                            path = path,
                            color = StorybookDeepGreen.copy(alpha = 0.92f),
                            style = Stroke(
                                width = boundaryStrokePx,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }

                    for (d in shapes) {
                        val hasHeroes = heroes.any { h ->
                            DistrictNameMatch.equalsCanonical(h.district.en, d.canonicalEnglish)
                        }
                        if (!hasHeroes) continue
                        val path = buildPixelPath(d.outerRing, drawW, drawH, padPx)
                        drawPath(
                            path = path,
                            color = TempleGold.copy(alpha = 0.38f * pulseAlpha.value),
                            style = Stroke(width = 3f.dp.toPx(), join = StrokeJoin.Round),
                        )
                        drawPath(
                            path = path,
                            color = DeepSaffron.copy(alpha = 0.30f * pulseAlpha.value),
                            style = Stroke(width = 2f.dp.toPx(), join = StrokeJoin.Round),
                        )
                    }

                    for (d in shapes) {
                        val cx = d.centroid.x * drawW + padPx
                        val cy = d.centroid.y * drawH + padPx
                        val labelEn = d.canonicalEnglish
                        val label = if (langIsKn) {
                            KarnatakaDistrictLabels.kannadaFor(labelEn)
                        } else {
                            labelEn
                        }
                        val measured = textMeasurer.measure(
                            text = AnnotatedString(label),
                            style = labelTextStyle,
                            maxLines = 2,
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = cx - measured.size.width / 2f,
                                y = cy - measured.size.height / 2f,
                            ),
                            color = mapLabelColor,
                        )
                    }
                }
            }
        }
    }
}
