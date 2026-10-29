package com.example.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyHelper {
  const val CURRENCY_AFN = "AFN"
  const val CURRENCY_USD = "USD"

  private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

  fun toPersianDigits(input: String): String {
    val builder = StringBuilder()
    for (char in input) {
      if (char in '0'..'9') {
        builder.append(persianDigits[char - '0'])
      } else {
        builder.append(char)
      }
    }
    return builder.toString()
  }

  fun toPersianDigits(number: Number): String {
    return toPersianDigits(number.toString())
  }

  fun formatMoney(
    amount: BigDecimal,
    currency: String = CURRENCY_AFN,
    includeSymbol: Boolean = true
  ): String {
    val rounded = amount.setScale(2, RoundingMode.HALF_UP)
    val symbols = DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = ','
      decimalSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0.00", symbols)
    val formattedEnglish = formatter.format(rounded)
    val formattedPersian = toPersianDigits(formattedEnglish)

    if (!includeSymbol) return formattedPersian

    val symbol = when (currency.uppercase()) {
      CURRENCY_USD -> "دالر ($)"
      else -> "افغانی (؋)"
    }
    return "$formattedPersian $symbol"
  }

  fun convertCurrency(
    amount: BigDecimal,
    fromCurrency: String,
    toCurrency: String,
    exchangeRateUsdToAfn: BigDecimal
  ): BigDecimal {
    if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
    if (exchangeRateUsdToAfn <= BigDecimal.ZERO) return amount

    return if (fromCurrency.equals(CURRENCY_USD, ignoreCase = true)) {
      // USD to AFN
      amount.multiply(exchangeRateUsdToAfn).setScale(2, RoundingMode.HALF_UP)
    } else {
      // AFN to USD
      amount.divide(exchangeRateUsdToAfn, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP)
    }
  }

  fun parseAmount(input: String): BigDecimal {
    val englishText = input
      .replace('۰', '0')
      .replace('۱', '1')
      .replace('۲', '2')
      .replace('۳', '3')
      .replace('۴', '4')
      .replace('۵', '5')
      .replace('۶', '6')
      .replace('۷', '7')
      .replace('۸', '8')
      .replace('۹', '9')
      .replace(",", "")
      .trim()

    return try {
      if (englishText.isEmpty()) BigDecimal.ZERO else BigDecimal(englishText)
    } catch (_: Exception) {
      BigDecimal.ZERO
    }
  }
}
