package eg.radio.ui.common

import java.util.Locale

/**
 * Display-only Arabic translations. Stored values (DB, backups, Radio Browser
 * matching) stay exactly as they were — only what the user *sees* is translated.
 */
private val ARABIC = Locale("ar")

private val TAGS_AR: Map<String, String> = mapOf(
    // Genres
    "pop" to "بوب",
    "electronic" to "إلكترونية",
    "techno" to "تكنو",
    "dub" to "داب",
    "ambient" to "أمبينت",
    "breakbeat" to "بريك بيت",
    "uk garage" to "يو كي غاراج",
    "jungle" to "جانجل",
    "house" to "هاوس",
    "trance" to "ترانس",
    "drum and bass" to "درَم آند بيس",
    "hip hop" to "هيب هوب",
    "hip-hop" to "هيب هوب",
    "rap" to "راب",
    "rock" to "روك",
    "metal" to "ميتال",
    "punk" to "بانك",
    "post-punk" to "بوست بانك",
    "jazz" to "جاز",
    "blues" to "بلوز",
    "classical" to "كلاسيكية",
    "folk" to "فولك",
    "country" to "كانتري",
    "reggae" to "ريغي",
    "soul" to "سول",
    "funk" to "فانك",
    "disco" to "ديسكو",
    "r&b" to "آر آند بي",
    "latin" to "لاتينية",
    "world" to "موسيقى عالمية",
    "oldies" to "أغاني قديمة",
    "news" to "أخبار",
    "talk" to "حوارية",
    "sports" to "رياضة",
    "religious" to "دينية",
    "quran" to "قرآن",
    "arabic" to "عربية",
    "experimental" to "تجريبية",
    "noise" to "نويز",
    "idm" to "آي دي إم",
    "downtempo" to "داون تيمبو",
    "lo-fi" to "لو فاي",
    "chillout" to "تشيل آوت",
    "indie" to "إندي",
    "alternative" to "بديلة",
    // Moods
    "energetic" to "حماسي",
    "heavy" to "ثقيل",
    "crowd safe" to "مناسب للجمهور",
    "calm" to "هادئ",
    "educational" to "تعليمي",
    // Styles
    "dj set" to "مجموعة دي جي",
    "radio show" to "برنامج إذاعي",
    "talk show" to "برنامج حواري"
)

/** Arabic label for a genre / mood / style tag; unknown (user-made) tags are shown as typed. */
fun tagAr(name: String): String = TAGS_AR[name.trim().lowercase()] ?: name

/** Arabic country name for an ISO alpha-2 code; falls back to the code itself. */
fun countryAr(code: String): String =
    Locale("", code).getDisplayCountry(ARABIC).takeIf { it.isNotBlank() && it != code.uppercase() } ?: code

/**
 * Arabic name for a stored language value. Radio Browser often stores several
 * names in one string ("arabic,العربية,عربي"), so each part is translated and
 * duplicates are merged.
 */
fun languageAr(storedName: String): String =
    storedName.split(",", "،", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { part ->
            val code = Locale.getISOLanguages().firstOrNull {
                Locale(it).getDisplayLanguage(Locale.ENGLISH).equals(part, ignoreCase = true)
            }
            code?.let { Locale(it).getDisplayLanguage(ARABIC) }?.takeIf { it.isNotBlank() } ?: part
        }
        .distinct()
        .joinToString("، ")
        .ifEmpty { storedName }

/** Bitrate label used on cards and the player bar. */
fun bitrateAr(kbps: Int): String = "$kbps ك.ب/ث"
