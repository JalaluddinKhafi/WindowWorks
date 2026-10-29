package com.example

import com.example.core.CurrencyHelper
import com.example.domain.engine.CostCalculator
import com.example.domain.model.WindowDesign
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CurrencyAndFinanceTest {

  @Test
  fun testExactCurrencyConversion() {
    val rate = BigDecimal("72.50")
    val amountUsd = BigDecimal("100.00")
    val convertedAfn = CurrencyHelper.convertCurrency(amountUsd, "USD", "AFN", rate)
    assertEquals(BigDecimal("7250.00"), convertedAfn)

    val amountAfn = BigDecimal("7250.00")
    val convertedUsd = CurrencyHelper.convertCurrency(amountAfn, "AFN", "USD", rate)
    assertEquals(BigDecimal("100.00"), convertedUsd)
  }

  @Test
  fun testPersianNumeralConversion() {
    val englishNumber = "1234567890"
    val persian = CurrencyHelper.toPersianDigits(englishNumber)
    assertEquals("۱۲۳۴۵۶۷۸۹۰", persian)

    val parsed = CurrencyHelper.parseAmount("۱۲,۵۰۰.۵۰")
    assertEquals(BigDecimal("12500.50"), parsed)
  }

  @Test
  fun testMarkupVersusMarginCalculation() {
    val cost = BigDecimal("1000.00")
    val markup = BigDecimal("25.00") // 25% markup -> Price = 1250.00
    val factor = BigDecimal.ONE.add(markup.divide(BigDecimal("100.00"), 4, RoundingMode.HALF_UP))
    val price = cost.multiply(factor).setScale(2, RoundingMode.HALF_UP)
    assertEquals(BigDecimal("1250.00"), price)

    // Margin = (Price - Cost) / Price * 100 = (250 / 1250) * 100 = 20.00%
    val profit = price.subtract(cost)
    val margin = profit.divide(price, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP)
    assertEquals(BigDecimal("20.00"), margin)
  }

  @Test
  fun testCostEstimateCalculatesAccurately() {
    val design = WindowDesign(
      id = "test_w1",
      name = "پنجره تست",
      widthMm = 1400,
      heightMm = 1600
    )
    val estimate = CostCalculator.estimateCost(design)
    assertTrue(estimate.totalBaseCost > BigDecimal.ZERO)
    assertTrue(estimate.quotationPrice > estimate.totalBaseCost)
    assertEquals(BigDecimal("25.00"), estimate.markupPercent)
    assertEquals(BigDecimal("20.00"), estimate.profitMarginPercent)
  }
}
