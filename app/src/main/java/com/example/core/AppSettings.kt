package com.example.core

import java.math.BigDecimal

/**
 * Typed snapshot of the `app_settings` table.
 * Every screen reads its default values from here instead of hardcoding them.
 */
data class AppSettings(
  val workshopName: String = "کارگاه تخصصی در و پنجره دوجداره",
  val baseCurrency: String = CurrencyHelper.CURRENCY_AFN,
  val exchangeRateUsdToAfn: BigDecimal = BigDecimal("72.50"),
  val defaultMarkupPercent: BigDecimal = BigDecimal("25.00"),
  val sawKerfMm: Int = 4,
  val endTrimMm: Int = 20,
  val quotationPaymentTerms: String =
    "۵۰٪ پیش‌پرداخت هنگام عقد قرارداد، ۴۰٪ هنگام تحویل، ۱۰٪ پس از نصب و تحویل نهایی.",
  val language: String = "fa"
) {
  companion object {
    const val KEY_WORKSHOP_NAME = "workshop_name"
    const val KEY_BASE_CURRENCY = "base_currency"
    const val KEY_EXCHANGE_RATE = "exchange_rate_usd_to_afn"
    const val KEY_DEFAULT_MARKUP = "default_markup_percent"
    const val KEY_SAW_KERF = "saw_kerf_mm"
    const val KEY_END_TRIM = "end_trim_mm"
    const val KEY_PAYMENT_TERMS = "quotation_payment_terms"
    const val KEY_APP_LANGUAGE = "app_language"

    /** Set to "true" until the bootstrap admin replaces the default password. */
    const val KEY_FORCE_PASSWORD_CHANGE = "force_password_change"

    /** Id of the last signed-in user so the session survives app restarts. */
    const val KEY_LAST_USER_ID = "last_user_id"

    /** Settings created on a fresh database (or restored from an older backup). */
    val DEFAULTS: List<Pair<String, String>> = listOf(
      KEY_WORKSHOP_NAME to "کارگاه تخصصی در و پنجره دوجداره",
      KEY_BASE_CURRENCY to CurrencyHelper.CURRENCY_AFN,
      KEY_EXCHANGE_RATE to "72.50",
      KEY_DEFAULT_MARKUP to "25.00",
      KEY_SAW_KERF to "4",
      KEY_END_TRIM to "20",
      KEY_PAYMENT_TERMS to
        "۵۰٪ پیش‌پرداخت هنگام عقد قرارداد، ۴۰٪ هنگام تحویل، ۱۰٪ پس از نصب و تحویل نهایی.",
      KEY_FORCE_PASSWORD_CHANGE to "false",
      KEY_APP_LANGUAGE to "fa"
    )

    fun fromMap(values: Map<String, String>): AppSettings {
      val defaults = AppSettings()
      fun read(key: String, fallback: String) = values[key]?.takeIf { it.isNotBlank() } ?: fallback
      val currency = read(KEY_BASE_CURRENCY, defaults.baseCurrency).uppercase()
        .takeIf { it == CurrencyHelper.CURRENCY_USD } ?: CurrencyHelper.CURRENCY_AFN
      return AppSettings(
        workshopName = read(KEY_WORKSHOP_NAME, defaults.workshopName),
        baseCurrency = currency,
        exchangeRateUsdToAfn = CurrencyHelper.parseAmount(read(KEY_EXCHANGE_RATE, "72.50"))
          .takeIf { it > BigDecimal.ZERO } ?: defaults.exchangeRateUsdToAfn,
        defaultMarkupPercent = CurrencyHelper.parseAmount(read(KEY_DEFAULT_MARKUP, "25.00"))
          .max(BigDecimal.ZERO),
        sawKerfMm = CurrencyHelper.parseAmount(read(KEY_SAW_KERF, "4")).toInt().coerceIn(0, 50),
        endTrimMm = CurrencyHelper.parseAmount(read(KEY_END_TRIM, "20")).toInt().coerceIn(0, 500),
        quotationPaymentTerms = read(KEY_PAYMENT_TERMS, defaults.quotationPaymentTerms),
        language = read(KEY_APP_LANGUAGE, defaults.language)
      )
    }

    fun toMap(settings: AppSettings): List<Pair<String, String>> = listOf(
      KEY_WORKSHOP_NAME to settings.workshopName,
      KEY_BASE_CURRENCY to settings.baseCurrency,
      KEY_EXCHANGE_RATE to settings.exchangeRateUsdToAfn.toPlainString(),
      KEY_DEFAULT_MARKUP to settings.defaultMarkupPercent.toPlainString(),
      KEY_SAW_KERF to settings.sawKerfMm.toString(),
      KEY_END_TRIM to settings.endTrimMm.toString(),
      KEY_PAYMENT_TERMS to settings.quotationPaymentTerms,
      KEY_APP_LANGUAGE to settings.language
    )
  }
}
