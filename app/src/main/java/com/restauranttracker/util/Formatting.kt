package com.restauranttracker.util

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

fun parseDateInput(text: String): LocalDate? =
    runCatching { LocalDate.parse(text) }.getOrNull()

/** Format a price in minor units (cents) for the given ISO 4217 code. Falls back to "—" on null. */
fun formatPrice(cents: Long?, currencyCode: String): String {
    if (cents == null) return "—"
    val currency = runCatching { Currency.getInstance(currencyCode) }
        .getOrDefault(Currency.getInstance(Locale.getDefault()))
    val nf = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        this.currency = currency
        maximumFractionDigits = currency.defaultFractionDigits
        minimumFractionDigits = currency.defaultFractionDigits
    }
    val divisor = pow10(currency.defaultFractionDigits)
    return nf.format(cents.toDouble() / divisor)
}

/** Parse "12.50" / "12" into cents using the currency's fraction digits. Returns null on bad input. */
fun parsePriceInput(text: String, currencyCode: String): Long? {
    if (text.isBlank()) return null
    val currency = runCatching { Currency.getInstance(currencyCode) }
        .getOrDefault(Currency.getInstance(Locale.getDefault()))
    val normalized = text.replace(',', '.').trim()
    val asDouble = normalized.toDoubleOrNull() ?: return null
    if (asDouble < 0) return null
    return Math.round(asDouble * pow10(currency.defaultFractionDigits))
}

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10 }
    return r
}
