package eg.radio.data

import eg.radio.data.remote.RadioBrowserStation

/**
 * دعم المحطات المصرية: كلمات بحث إضافية لالتقاط المحطات التي لا يصنّفها
 * Radio Browser تحت رمز الدولة EG، وقائمة محطات مدمجة تُضاف فقط إذا لم تكن
 * موجودة أصلاً في الدليل.
 *
 * تنبيه: الروابط المدمجة مأخوذة من قوائم عامة ولم تُختبر داخل التطبيق؛ قد يتعطل
 * بعضها مع الوقت. روابط الدليل نفسها تُفلتر بـ hidebroken فهي أوثق.
 */
object EgyptStations {

    /** أسماء تُبحث بها المحطات (لاتيني + عربي). */
    val searchKeywords = listOf(
        "nile fm", "nogoum", "mega fm", "radio masr", "nagham", "radio hits",
        "mix fm", "9090", "mahatet masr", "sada el balad", "shaabi", "el gouna",
        "quran cairo", "onsport", "arabesk",
        "نجوم", "راديو مصر", "محطة مصر", "نغم", "القرآن الكريم", "إذاعة القاهرة",
        "الشرق الأوسط", "صوت العرب", "البرنامج العام", "الشباب والرياضة",
        "صدى البلد", "شعبي", "الإسكندرية"
    )

    /** وسوم يبحث بها الدليل. */
    val searchTags = listOf("egypt", "egyptian", "cairo", "egyptian arabic", "masr")

    private class Builtin(val key: String, val station: RadioBrowserStation)

    private fun builtin(
        id: String, name: String, url: String, homepage: String, tags: String, language: String = "arabic"
    ) = Builtin(
        key = normalize(id),
        station = RadioBrowserStation(
            stationUuid = "builtin-$id",
            name = name,
            urlResolved = url,
            url = url,
            tags = tags,
            countryCode = "EG",
            language = language,
            homepage = homepage
        )
    )

    private val builtins = listOf(
        builtin("nilefm", "Nile FM 104.2", "http://ice31.securenetsystems.net/NILE", "https://www.nilefm.com", "pop,english hits", "english"),
        builtin("nogoum", "Nogoum FM 100.6", "http://ice33.securenetsystems.net/NOGOUM", "https://www.nogoumfm.net", "arabic hits"),
        builtin("megafm", "Mega FM 92.7", "http://mgstrm9.twesto.com:7281/megafm214", "https://megafm927.com", "pop,arabic"),
        builtin("radiohits", "Radio Hits 88.2", "http://audiostreaming.twesto.com/radiohits215", "", "hits,pop"),
        builtin("radiomasr", "Radio Masr 88.7", "http://live.radiomasr.net:8060/RADIOMASR", "https://radiomasr.net", "news,pop"),
        builtin("9090", "Radio 90.90 FM", "http://9090streaming.mobtada.com/9090FMEGYPT", "https://9090.fm", "variety"),
        builtin("arabesk", "Radio Arabesk 90.90", "http://9090streaming2.mobtada.com/9090FMEGYPT", "", "variety"),
        builtin("mahatetmasr", "Mahatet Masr", "http://radio.mahatetmasr.com/mahatetmasr", "https://mahatetmasr.com", "pop")
    )

    fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    private fun normalizeUrl(u: String): String =
        u.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/', '?', ';')

    private val egyptHints = listOf("egypt", "مصر", "cairo", "القاهرة")

    /** مصرية إذا كان رمز دولتها EG، أو بلا رمز ويدل اسمها/وسومها على مصر. */
    fun isEgyptian(st: RadioBrowserStation): Boolean {
        if (st.countryCode.equals("EG", ignoreCase = true)) return true
        if (st.countryCode.isNotBlank()) return false
        val hay = (st.name + " " + st.tags).lowercase()
        return egyptHints.any { hay.contains(it) }
    }

    /** يدمج بدون تكرار (بالمعرّف ثم بالرابط)، ثم يضيف المدمجة الغائبة عن الدليل فقط. */
    fun merge(fromDirectory: List<RadioBrowserStation>): List<RadioBrowserStation> {
        val seenUuid = HashSet<String>()
        val seenUrl = HashSet<String>()
        val out = ArrayList<RadioBrowserStation>()
        for (st in fromDirectory) {
            if (st.name.isBlank()) continue
            val url = st.streamUrl
            if (!(url.startsWith("http://") || url.startsWith("https://"))) continue
            if (!seenUuid.add(st.stationUuid)) continue
            if (!seenUrl.add(normalizeUrl(url))) continue
            out += st
        }
        val names = out.map { normalize(it.name) }
        for (b in builtins) {
            val url = b.station.streamUrl
            if (names.any { it.contains(b.key) }) continue
            if (!seenUrl.add(normalizeUrl(url))) continue
            out += b.station
        }
        return out
    }

    /** القائمة المدمجة وحدها (تُعرض إذا تعذّر الوصول للدليل). */
    fun builtInOnly(): List<RadioBrowserStation> = builtins.map { it.station }
}
