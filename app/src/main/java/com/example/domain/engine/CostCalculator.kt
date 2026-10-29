package com.example.domain.engine

import com.example.domain.model.BOMItem
import com.example.domain.model.CostEstimate
import com.example.domain.model.WindowDesign
import java.math.BigDecimal
import java.math.RoundingMode

object CostCalculator {

  fun estimateCost(
    design: WindowDesign,
    projectId: String = "",
    // Configurable default prices in AFN
    profilePricePerMeter: BigDecimal = BigDecimal("180.00"),
    glassPricePerM2: BigDecimal = BigDecimal("650.00"),
    reinforcementPricePerMeter: BigDecimal = BigDecimal("70.00"),
    gasketPricePerMeter: BigDecimal = BigDecimal("15.00"),
    handlePricePerPiece: BigDecimal = BigDecimal("220.00"),
    hingePricePerPiece: BigDecimal = BigDecimal("95.00"),
    espagnolettePrice: BigDecimal = BigDecimal("350.00"),
    laborRatePerWindow: BigDecimal = BigDecimal("500.00"),
    installationRatePerWindow: BigDecimal = BigDecimal("350.00"),
    transportCostPerWindow: BigDecimal = BigDecimal("150.00"),
    markupPercent: BigDecimal = BigDecimal("25.00"), // Default 25% profit markup
    currency: String = "AFN"
  ): CostEstimate {
    val mfg = ManufacturingCalculator.calculate(design)

    val bomItems = mutableListOf<BOMItem>()

    // 1. Profiles Cost
    val profileMeters = BigDecimal(mfg.totalProfileLengthMm)
      .divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)
    val profileTotal = profileMeters.multiply(profilePricePerMeter).setScale(2, RoundingMode.HALF_UP)
    bomItems.add(
      BOMItem(
        id = "bom_prof",
        category = "پروفیل",
        code = "PRF-01",
        name = "${design.profileSystem} (${design.frameFinish.titlePersian})",
        quantity = profileMeters,
        unit = "متر",
        unitPrice = profilePricePerMeter,
        lineTotal = profileTotal,
        currency = currency
      )
    )

    // 2. Reinforcement Steel
    val rfMeters = BigDecimal(mfg.totalReinforcementLengthMm)
      .divide(BigDecimal(1000), 2, RoundingMode.HALF_UP)
    val rfTotal = rfMeters.multiply(reinforcementPricePerMeter).setScale(2, RoundingMode.HALF_UP)
    if (rfMeters > BigDecimal.ZERO) {
      bomItems.add(
        BOMItem(
          id = "bom_rf",
          category = "گالوانیزه",
          code = "GALV-01",
          name = "گالوانیزه تقویتی ضخامت ۱.۲۵mm",
          quantity = rfMeters,
          unit = "متر",
          unitPrice = reinforcementPricePerMeter,
          lineTotal = rfTotal,
          currency = currency
        )
      )
    }

    // 3. Glass
    val glassTotal = mfg.totalGlassAreaM2.multiply(glassPricePerM2).setScale(2, RoundingMode.HALF_UP)
    bomItems.add(
      BOMItem(
        id = "bom_glass",
        category = "شیشه",
        code = "GLS-01",
        name = "شیشه دوجداره استاندارد با گاز آرگون",
        quantity = mfg.totalGlassAreaM2,
        unit = "مترمربع",
        unitPrice = glassPricePerM2,
        lineTotal = glassTotal,
        currency = currency
      )
    )

    // 4. Gasket
    val gasketTotal = mfg.totalGasketMeters.multiply(gasketPricePerMeter).setScale(2, RoundingMode.HALF_UP)
    bomItems.add(
      BOMItem(
        id = "bom_gasket",
        category = "لاستیک",
        code = "EPDM-01",
        name = "لاستیک درزبندی EPDM مقاوم در برابر UV",
        quantity = mfg.totalGasketMeters,
        unit = "متر",
        unitPrice = gasketPricePerMeter,
        lineTotal = gasketTotal,
        currency = currency
      )
    )

    // 5. Hardware
    var hardwareTotal = BigDecimal.ZERO
    mfg.hardwarePcs.forEach { (item, qty) ->
      val unitPrice = when {
        item.contains("دستگیره") -> handlePricePerPiece
        item.contains("لولا") -> hingePricePerPiece
        else -> espagnolettePrice
      }
      val lineTot = BigDecimal(qty).multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
      hardwareTotal = hardwareTotal.add(lineTot)
      bomItems.add(
        BOMItem(
          id = "bom_hw_${item.hashCode()}",
          category = "یراق‌آلات",
          code = "HDW-01",
          name = item,
          quantity = BigDecimal(qty),
          unit = "عدد",
          unitPrice = unitPrice,
          lineTotal = lineTot,
          currency = currency
        )
      )
    }

    val consumablesCost = BigDecimal("150.00") // چسب سیلیکون، پیچ، تاکوز
    bomItems.add(
      BOMItem(
        id = "bom_cons",
        category = "اتصالات",
        code = "CNS-01",
        name = "چسب ماستیک، پیچ سرمته و تاکوز زیرشیشه",
        quantity = BigDecimal.ONE,
        unit = "بسته",
        unitPrice = consumablesCost,
        lineTotal = consumablesCost,
        currency = currency
      )
    )

    val totalBaseCost = profileTotal
      .add(rfTotal)
      .add(glassTotal)
      .add(gasketTotal)
      .add(hardwareTotal)
      .add(consumablesCost)
      .add(laborRatePerWindow)
      .add(transportCostPerWindow)
      .add(installationRatePerWindow)

    // Markup: Price = Cost * (1 + Markup/100)
    val markupFactor = BigDecimal.ONE.add(
      markupPercent.divide(BigDecimal("100.00"), 4, RoundingMode.HALF_UP)
    )
    val quotationPrice = totalBaseCost.multiply(markupFactor).setScale(2, RoundingMode.HALF_UP)

    // Profit Margin % = ((Price - Cost) / Price) * 100
    val profit = quotationPrice.subtract(totalBaseCost)
    val marginPercent = if (quotationPrice > BigDecimal.ZERO) {
      profit.divide(quotationPrice, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal("100.00"))
        .setScale(2, RoundingMode.HALF_UP)
    } else BigDecimal.ZERO

    return CostEstimate(
      id = "est_${design.id}",
      projectId = projectId,
      designId = design.id,
      profileCost = profileTotal,
      glassCost = glassTotal,
      hardwareCost = hardwareTotal,
      reinforcementCost = rfTotal,
      gasketCost = gasketTotal,
      consumablesCost = consumablesCost,
      laborCost = laborRatePerWindow,
      transportCost = transportCostPerWindow,
      installationCost = installationRatePerWindow,
      totalBaseCost = totalBaseCost,
      markupPercent = markupPercent,
      profitMarginPercent = marginPercent,
      quotationPrice = quotationPrice,
      currency = currency,
      isProvisional = mfg.isProvisional,
      missingRulesNote = mfg.missingRulesDescription,
      bomItems = bomItems
    )
  }
}
