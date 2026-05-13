package com.example.myapplication.data.karnataka

/** Kannada labels keyed by canonical English district names used in the app & navigation. */
object KarnatakaDistrictLabels {
    val byEnglish: Map<String, String> = mapOf(
        "Bagalkote" to "ಬಾಗಲಕೋಟೆ",
        "Ballari" to "ಬಳ್ಳಾರಿ",
        "Belagavi" to "ಬೆಳಗಾವಿ",
        "Bengaluru Rural" to "ಬೆಂಗಳೂರು ಗ್ರಾಮಾಂತರ",
        "Bengaluru Urban" to "ಬೆಂಗಳೂರು ನಗರ",
        "Bidar" to "ಬೀದರ್",
        "Chamarajanagara" to "ಚಾಮರಾಜನಗರ",
        "Chikkaballapura" to "ಚಿಕ್ಕಬಳ್ಳಾಪುರ",
        "Chikkamagaluru" to "ಚಿಕ್ಕಮಗಳೂರು",
        "Chitradurga" to "ಚಿತ್ರದುರ್ಗ",
        "Dakshina Kannada" to "ದಕ್ಷಿಣ ಕನ್ನಡ",
        "Davanagere" to "ದಾವಣಗೆರೆ",
        "Dharwad" to "ಧಾರವಾಡ",
        "Gadag" to "ಗದಗ",
        "Hassan" to "ಹಾಸನ",
        "Haveri" to "ಹಾವೇರಿ",
        "Kalaburagi" to "ಕಲಬುರಗಿ",
        "Kodagu" to "ಕೊಡಗು",
        "Kolar" to "ಕೋಲಾರ",
        "Koppal" to "ಕೊಪ್ಪಳ",
        "Mandya" to "ಮಂಡ್ಯ",
        "Mysuru" to "ಮೈಸೂರು",
        "Raichur" to "ರಾಯಚೂರು",
        "Ramanagara" to "ರಾಮನಗರ",
        "Shivamogga" to "ಶಿವಮೊಗ್ಗ",
        "Tumakuru" to "ತುಮಕೂರು",
        "Udupi" to "ಉಡುಪಿ",
        "Uttara Kannada" to "ಉತ್ತರ ಕನ್ನಡ",
        "Vijayapura" to "ವಿಜಯಪುರ",
        "Vijayanagara" to "ವಿಜಯನಗರ",
        "Yadgir" to "ಯಾದಗಿರಿ",
    )

    fun kannadaFor(canonicalEnglish: String): String =
        byEnglish[canonicalEnglish] ?: canonicalEnglish
}
