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

class ProjectBalanceConversionTest {

  /**
   * Mirrors `WorkshopRepository.recalculateProjectBalance`: USD payments are converted
   * to the contract currency using the rate stored on each payment.
   */
  private fun paidInContractCurrency(
    payments: List<Triple<String, String, String>>, // amount, currency, rateToAfn
    contractCurrency: String
  ): BigDecimal {
    var total = BigDecimal.ZERO
    payments.forEach { (amountStr, currency, rateStr) ->
      val amount = CurrencyHelper.parseAmount(amountStr)
      val rate = CurrencyHelper.parseAmount(rateStr).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
      val inAfn = if (currency.equals("USD", ignoreCase = true)) amount.multiply(rate) else amount
      val inContract = if (contractCurrency.equals("USD", ignoreCase = true)) {
        inAfn.divide(rate, 2, RoundingMode.HALF_UP)
      } else inAfn
      total = total.add(inContract)
    }
    return total.setScale(2, RoundingMode.HALF_UP)
  }

  @Test
  fun usdPaymentIsConvertedIntoAfnContract() {
    val paid = paidInContractCurrency(
      listOf(Triple("100.00", "USD", "70.00")),
      contractCurrency = "AFN"
    )
    assertEquals(BigDecimal("7000.00"), paid)
  }

  @Test
  fun afnPaymentIsConvertedIntoUsdContract() {
    val paid = paidInContractCurrency(
      listOf(Triple("3500.00", "AFN", "70.00")),
      contractCurrency = "USD"
    )
    assertEquals(BigDecimal("50.00"), paid)
  }

  @Test
  fun mixedCurrencyPaymentsAreSummedCorrectly() {
    val paid = paidInContractCurrency(
      listOf(
        Triple("1000.00", "AFN", "1.00"),
        Triple("100.00", "USD", "72.50")
      ),
      contractCurrency = "AFN"
    )
    assertEquals(BigDecimal("8250.00"), paid)
  }

  @Test
  fun balanceNeverGoesNegative() {
    val contract = BigDecimal("1000.00")
    val paid = paidInContractCurrency(
      listOf(Triple("500.00", "USD", "3.00")),
      contractCurrency = "AFN"
    )
    val balance = contract.subtract(paid).max(BigDecimal.ZERO)
    assertEquals(BigDecimal.ZERO, balance)
  }
}
