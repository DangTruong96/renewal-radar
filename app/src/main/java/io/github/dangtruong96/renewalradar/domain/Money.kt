package io.github.dangtruong96.renewalradar.domain

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money conversion and formatting that respects each currency's real
 * number of fraction digits. USD/EUR/GBP/AUD/CAD use 2 (cents); JPY, KRW
 * and VND use 0 — amounts in those currencies are stored as whole units.
 */
object Money {

    /** Currencies offered in the UI. */
    val SUPPORTED_CURRENCIES: List<String> =
        listOf("USD", "EUR", "GBP", "JPY", "VND", "AUD", "CAD", "KRW")

    /** Minor units per major unit for [currencyCode] (100 for USD, 1 for JPY). */
    fun minorPerMajor(currencyCode: String): Int {
        val digits = fractionDigits(currencyCode)
        var factor = 1
        repeat(digits) { factor *= 10 }
        return factor
    }

    fun fractionDigits(currencyCode: String): Int = try {
        Currency.getInstance(currencyCode).defaultFractionDigits.let {
            if (it < 0) 2 else it
        }
    } catch (e: IllegalArgumentException) {
        2
    }

    /** "12.99" (USD) -> 1299; "1500" (JPY) -> 1500. Returns null on bad input. */
    fun parseToMinor(text: String, currencyCode: String): Long? {
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val major = cleaned.toBigDecimalOrNull() ?: return null
        if (major.signum() < 0) return null
        return try {
            major.multiply(BigDecimal(minorPerMajor(currencyCode)))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    /** 1299 (USD) -> "12.99"; 1500 (JPY) -> "1500". For form fields. */
    fun minorToPlainString(amountMinor: Long, currencyCode: String): String {
        val factor = minorPerMajor(currencyCode)
        if (factor == 1) return amountMinor.toString()
        val major = BigDecimal(amountMinor).divide(BigDecimal(factor))
        return major.stripTrailingZeros().toPlainString()
    }

    /** Locale-aware display: 1299 USD -> "$12.99", 1500 JPY -> "¥1,500". */
    fun format(amountMinor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val currency = try {
            Currency.getInstance(currencyCode)
        } catch (e: IllegalArgumentException) {
            return "$amountMinor $currencyCode"
        }
        val factor = minorPerMajor(currencyCode)
        val major = BigDecimal(amountMinor).divide(BigDecimal(factor))
        val format = NumberFormat.getCurrencyInstance(locale)
        format.currency = currency
        // setCurrency() alone does not reset the fraction digits the
        // instance inherited from the locale's own currency, so a JPY
        // amount in a US locale would render as "¥1,500.00". Pin the
        // digits to the amount currency's real ones.
        val digits = fractionDigits(currencyCode)
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        return format.format(major)
    }
}
