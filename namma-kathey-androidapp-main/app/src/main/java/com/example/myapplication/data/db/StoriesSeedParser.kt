package com.example.myapplication.data.db

import android.content.Context
import com.example.myapplication.data.Hero
import com.example.myapplication.data.LocalizedText
import com.example.myapplication.data.QuizOption
import com.example.myapplication.data.QuizQuestion
import com.example.myapplication.data.karnataka.KarnatakaDistrictLabels
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses [assets/stories.json] (`{ "districts": [...] }`) into [Hero] rows used by Room.
 * [Hero.district] English matches [com.example.myapplication.data.karnataka.KarnatakaGeoJsonLoader]
 * canonical names so the district map highlights and filters work.
 */
object StoriesSeedParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Slug ids from JSON → English district keys used on the map / in [KarnatakaDistrictLabels]. */
    private val slugToCanonicalEnglish: Map<String, String> = mapOf(
        "bagalkot" to "Bagalkote",
        "ballari" to "Ballari",
        "belagavi" to "Belagavi",
        "bengaluru" to "Bengaluru Urban",
        "bengaluru_rural" to "Bengaluru Rural",
        "bengaluru_urban" to "Bengaluru Urban",
        "bidar" to "Bidar",
        "chamarajanagar" to "Chamarajanagara",
        "chikkaballapur" to "Chikkaballapura",
        "chikkamagaluru" to "Chikkamagaluru",
        "chitradurga" to "Chitradurga",
        "dakshina_kannada" to "Dakshina Kannada",
        "davanagere" to "Davanagere",
        "dharwad" to "Dharwad",
        "gadag" to "Gadag",
        "hassan" to "Hassan",
        "haveri" to "Haveri",
        "kalaburagi" to "Kalaburagi",
        "kodagu" to "Kodagu",
        "kolar" to "Kolar",
        "koppal" to "Koppal",
        "mandya" to "Mandya",
        "mysuru" to "Mysuru",
        "raichur" to "Raichur",
        "ramanagara" to "Ramanagara",
        "shivamogga" to "Shivamogga",
        "shivamogga_kuvempu" to "Shivamogga",
        "tumakuru" to "Tumakuru",
        "udupi" to "Udupi",
        "uttara_kannada" to "Uttara Kannada",
        "vijayanagara" to "Vijayanagara",
        "vijayapura" to "Vijayapura",
        "yadgir" to "Yadgir",
    )

    /** Fallback when [slugToCanonicalEnglish] misses a new slug — aligns spellings with GeoJSON / labels. */
    private fun normalizeDistrictEnglish(en: String): String = when (en.trim()) {
        "Bagalkot" -> "Bagalkote"
        "Chamarajanagar" -> "Chamarajanagara"
        "Chikkaballapur" -> "Chikkaballapura"
        "Bengaluru" -> "Bengaluru Urban"
        else -> en.trim()
    }

    fun parseToHeroes(context: Context, jsonText: String): List<Hero> {
        val root = json.decodeFromString<StoriesJsonRoot>(jsonText)
        val imageUrl = "android.resource://${context.packageName}/drawable/ic_launcher_foreground"

        return root.districts
            .sortedBy { it.id }
            .mapIndexed { index, row ->
                val canonicalEn = slugToCanonicalEnglish[row.id]
                    ?: normalizeDistrictEnglish(row.districtNameEn)

                val districtKn = KarnatakaDistrictLabels.byEnglish[canonicalEn]
                    ?: row.districtNameKn

                val quiz = row.quiz.mapIndexed { qIdx, q ->
                    q.toQuizQuestion(questionId = qIdx + 1)
                }

                Hero(
                    id = index + 1,
                    name = LocalizedText(
                        en = row.heroNameEn,
                        kn = row.heroNameKn,
                        hi = row.heroNameEn,
                    ),
                    district = LocalizedText(
                        en = canonicalEn,
                        kn = districtKn,
                        hi = canonicalEn,
                    ),
                    story = LocalizedText(
                        en = row.storyEn,
                        kn = row.storyKn,
                        hi = row.storyEn,
                    ),
                    imageUrl = imageUrl,
                    audioUrl = null,
                    latitude = row.statueLat,
                    longitude = row.statueLng,
                    quiz = quiz,
                )
            }
    }
}

@Serializable
private data class StoriesJsonRoot(
    val districts: List<StoryDistrictDto>,
)

@Serializable
private data class StoryDistrictDto(
    val id: String,
    @SerialName("district_name_en") val districtNameEn: String,
    @SerialName("district_name_kn") val districtNameKn: String,
    @SerialName("hero_name_en") val heroNameEn: String,
    @SerialName("hero_name_kn") val heroNameKn: String,
    @SerialName("story_en") val storyEn: String,
    @SerialName("story_kn") val storyKn: String,
    @SerialName("statue_lat") val statueLat: Double? = null,
    @SerialName("statue_lng") val statueLng: Double? = null,
    val quiz: List<StoryQuizDto> = emptyList(),
)

@Serializable
private data class StoryQuizDto(
    @SerialName("question_en") val questionEn: String,
    @SerialName("question_kn") val questionKn: String,
    @SerialName("options_en") val optionsEn: List<String>,
    @SerialName("options_kn") val optionsKn: List<String>,
    val answer: String,
) {
    fun toQuizQuestion(questionId: Int): QuizQuestion {
        val n = minOf(optionsEn.size, optionsKn.size).coerceAtLeast(0)
        val en = optionsEn.take(n)
        val kn = optionsKn.take(n)
        val correctIdx = en.indexOfFirst { it.trim() == answer.trim() }.takeIf { it >= 0 }
            ?: en.indexOfFirst { it.equals(answer.trim(), ignoreCase = true) }

        val options = List(n) { i ->
            QuizOption(
                id = i + 1,
                text = LocalizedText(
                    en = en[i],
                    kn = kn.getOrElse(i) { en[i] },
                    hi = en[i],
                ),
            )
        }

        val correctId = (correctIdx.takeIf { it >= 0 } ?: 0) + 1

        return QuizQuestion(
            id = questionId,
            question = LocalizedText(
                en = questionEn,
                kn = questionKn,
                hi = questionEn,
            ),
            options = options,
            correctOptionId = correctId,
        )
    }
}
