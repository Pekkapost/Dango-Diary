package com.dangodiary.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(dateFormatter)

/** Format a price in minor units (cents) for the given ISO 4217 code. Falls back to "—" on null. */
fun formatPrice(cents: Long?, currencyCode: String): String {
    if (cents == null) return "—"
    val currency = currencyOrDefault(currencyCode)
    val nf = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        this.currency = currency
        maximumFractionDigits = currency.defaultFractionDigits
        minimumFractionDigits = currency.defaultFractionDigits
    }
    return nf.format(cents.toDouble() / pow10(currency.defaultFractionDigits))
}

/** Render a cents amount as the editable decimal string the price field expects (e.g. "12.50"). */
fun centsToEditableString(cents: Long, currencyCode: String): String {
    val currency = currencyOrDefault(currencyCode)
    return (cents.toDouble() / pow10(currency.defaultFractionDigits)).toString()
}

/** Parse "12.50" / "12" into cents using the currency's fraction digits. Returns null on bad input. */
fun parsePriceInput(text: String, currencyCode: String): Long? {
    if (text.isBlank()) return null
    val currency = currencyOrDefault(currencyCode)
    val normalized = text.replace(',', '.').trim()
    val asDouble = normalized.toDoubleOrNull() ?: return null
    if (asDouble < 0) return null
    return Math.round(asDouble * pow10(currency.defaultFractionDigits))
}

private fun currencyOrDefault(code: String): Currency =
    runCatching { Currency.getInstance(code) }
        .getOrDefault(Currency.getInstance(Locale.getDefault()))

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10 }
    return r
}

/**
 * Best-effort city extraction from a free-form address. Tuned for the two major shapes
 * Google Places returns as `formatted_address`:
 *
 *     US / Canada / Japan etc.   [Place,] Street, City, State+Postcode, Country
 *     UK / FR / DE / ES / IT /   [Place,] Street, City+Postcode, Country
 *     AU / similar
 *
 * **Algorithm:**
 * 1. Strip trailing country / region tokens off the end (handles "London, UK" and the
 *    sometimes-seen "London, England, UK" — keep popping while the last segment is a known
 *    country name).
 * 2. Try to pull city words from the now-last segment, stripping postcode-like tokens
 *    (any token containing a digit) and 2–3-letter all-uppercase state/province codes
 *    (CA, IL, NY, NSW, ON, SP, RM, …).
 * 3. If step 2 found nothing — typical of US "CA 94133" or Canadian "ON M5C 2H6" — fall
 *    back to the segment before it, where the US/CA/JP-style formats put the bare city
 *    name.
 *
 * Examples by country:
 * ```
 *  "160 Jefferson St, San Francisco, CA 94133, USA"                  → "San Francisco"
 *  "Boudin Bakery, Pier 39, San Francisco, CA 94133, USA"            → "San Francisco"
 *  "221B Baker St, London NW1 6XE, UK"                               → "London"
 *  "10 Downing St, Westminster, London SW1A 2AA, UK"                 → "London"
 *  "5 Avenue Anatole France, 75007 Paris, France"                    → "Paris"
 *  "08013 Barcelona, Spain"                                          → "Barcelona"
 *  "Piazza del Colosseo, 1, 00184 Roma RM, Italy"                    → "Roma"
 *  "1 Macquarie St, Sydney NSW 2000, Australia"                      → "Sydney"
 *  "123 Yonge St, Toronto, ON M5C 2H6, Canada"                       → "Toronto"
 *  "1 Chome-1-1 Marunouchi, Chiyoda City, Tokyo 100-8994, Japan"     → "Tokyo"
 *  "Avenida Paulista, Bela Vista, São Paulo - SP, Brazil"            → "São Paulo"
 * ```
 *
 * Returns null for blank input.
 */
fun extractCity(addressText: String?): String? {
    if (addressText.isNullOrBlank()) return null
    val parts = addressText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null

    // Strip trailing country / region tokens. Pop repeatedly so "London, England, UK" loses
    // both "UK" and "England".
    var end = parts.size
    while (end > 0 && looksLikeCountry(parts[end - 1])) end--
    if (end == 0) return parts[0]

    // Try the now-last segment first — UK/EU shapes put the city here, mixed with postcode.
    extractCityFromSegment(parts[end - 1])?.let { return it }

    // No city words in the last segment (US "CA 94133", Canadian "ON M5C 2H6", etc.) — fall
    // back to the one before, which is where US/CA/JP put the bare city.
    if (end >= 2) {
        return extractCityFromSegment(parts[end - 2]) ?: parts[end - 2]
    }
    return parts[0]
}

/** True when [segment] is a recognised country / region name. Case-insensitive. */
private fun looksLikeCountry(segment: String): Boolean =
    segment.lowercase().trim() in COMMON_COUNTRIES

/**
 * Extract city words from one comma-split segment. Drops:
 *   - tokens with any digit (postcodes: `94133`, `SW1A`, `M5C`, `2AA`, `100-8994`, ...);
 *   - tokens that are pure punctuation / contain no letters (e.g. `-`);
 *   - 2–3-letter all-uppercase tokens (state / province codes: `CA`, `NSW`, `ON`, `SP`, ...).
 *
 * Returns the remaining words joined with single spaces, or null when nothing survives.
 */
private fun extractCityFromSegment(segment: String): String? {
    val tokens = segment.split(' ').filter { it.isNotEmpty() }
    val cityWords = tokens.filter { token ->
        when {
            !token.any { it.isLetter() } -> false
            token.any { it.isDigit() } -> false
            token.length <= 3 && token.all { it.isUpperCase() } -> false
            else -> true
        }
    }
    return cityWords.takeIf { it.isNotEmpty() }?.joinToString(" ")
}

/**
 * Country / region names that may appear as the trailing segment of a `formatted_address`.
 *
 * **Complete** for North America, Europe, and Asia — every sovereign state in those three
 * regions is here, plus widely-used alternate names (`uk` / `united kingdom`,
 * `czechia` / `czech republic`, etc.) and the UK's constituent countries
 * (`england`, `scotland`, `wales`, `northern ireland`) so addresses like `"London,
 * England, UK"` strip both trailing tokens. South America and Africa carry the
 * commonly-encountered subset, not the full continent — add to that as gaps surface.
 *
 * All entries are lowercase; matching is done via `lowercase().trim()`.
 */
private val COMMON_COUNTRIES: Set<String> = setOf(
    // ===== NORTH AMERICA (complete) =====
    // Northern
    "usa", "u.s.a.", "us", "u.s.", "united states", "united states of america",
    "canada", "mexico",
    "greenland", "bermuda",
    // Central America
    "belize", "costa rica", "el salvador", "guatemala", "honduras", "nicaragua", "panama",
    // Caribbean
    "antigua and barbuda", "bahamas", "the bahamas", "barbados",
    "cuba", "dominica", "dominican republic",
    "grenada", "haiti", "jamaica",
    "saint kitts and nevis", "saint lucia", "saint vincent and the grenadines",
    "trinidad and tobago",
    "puerto rico",

    // ===== EUROPE (complete) =====
    // British Isles
    "uk", "u.k.", "united kingdom", "great britain",
    "england", "scotland", "wales", "northern ireland", "ireland",
    // Nordics + Baltics
    "sweden", "norway", "denmark", "finland", "iceland",
    "estonia", "latvia", "lithuania",
    // Western Europe
    "france", "germany", "netherlands", "the netherlands", "belgium", "luxembourg",
    "switzerland", "austria", "liechtenstein", "monaco",
    // Southern Europe + microstates
    "spain", "portugal", "italy", "greece", "malta", "cyprus",
    "andorra", "san marino", "vatican city", "vatican",
    // Central / Eastern Europe
    "poland", "czech republic", "czechia", "slovakia", "hungary",
    "romania", "bulgaria", "moldova",
    // Balkans
    "croatia", "serbia", "slovenia", "bosnia and herzegovina",
    "north macedonia", "macedonia", "montenegro", "kosovo", "albania",
    // Eastern Europe / Caucasus / transcontinental
    "russia", "ukraine", "belarus",
    "armenia", "azerbaijan", "georgia",
    "turkey",

    // ===== ASIA (complete) =====
    // East Asia
    "japan", "china", "hong kong", "macau", "macao", "taiwan",
    "south korea", "north korea", "korea", "republic of korea",
    "mongolia",
    // Southeast Asia
    "thailand", "vietnam", "cambodia", "laos", "myanmar", "burma",
    "philippines", "indonesia", "malaysia", "singapore", "brunei",
    "timor-leste", "east timor",
    // South Asia
    "india", "pakistan", "bangladesh", "sri lanka", "nepal", "bhutan", "maldives",
    "afghanistan",
    // Central Asia
    "kazakhstan", "kyrgyzstan", "tajikistan", "turkmenistan", "uzbekistan",
    // West Asia / Middle East
    "iran", "iraq", "israel", "palestine", "jordan", "lebanon", "syria",
    "saudi arabia", "uae", "united arab emirates",
    "qatar", "kuwait", "bahrain", "oman", "yemen",

    // ===== OCEANIA (frequent for English-speaking users) =====
    "australia", "new zealand", "fiji",

    // ===== SOUTH AMERICA (commonly encountered, not exhaustive) =====
    "brazil", "argentina", "chile", "colombia", "peru", "venezuela",
    "uruguay", "paraguay", "bolivia", "ecuador", "guyana", "suriname",

    // ===== AFRICA (commonly encountered, not exhaustive) =====
    "south africa", "egypt", "morocco", "tunisia", "algeria", "libya",
    "nigeria", "kenya", "ghana", "ethiopia", "tanzania", "uganda",
    "senegal", "cameroon", "zimbabwe", "rwanda",
)
