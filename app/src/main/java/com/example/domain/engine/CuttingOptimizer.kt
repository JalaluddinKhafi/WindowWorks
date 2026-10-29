package com.example.domain.engine

import com.example.domain.model.CutPiece
import com.example.domain.model.CuttingOptimizationResult
import com.example.domain.model.StockBarPlan

object CuttingOptimizer {
  const val DEFAULT_STOCK_LENGTH_MM = 6000
  const val DEFAULT_SAW_KERF_MM = 4
  const val DEFAULT_END_TRIM_MM = 20
  const val REUSABLE_OFFCUT_THRESHOLD_MM = 500

  fun optimize(
    pieces: List<CutPiece>,
    profileName: String = "پروفیل فریم / لنگه",
    stockLengthMm: Int = DEFAULT_STOCK_LENGTH_MM,
    sawKerfMm: Int = DEFAULT_SAW_KERF_MM,
    endTrimMm: Int = DEFAULT_END_TRIM_MM
  ): CuttingOptimizationResult {
    // Flatten quantities into individual cut items
    val flatPieces = mutableListOf<CutPiece>()
    for (p in pieces) {
      repeat(p.quantity) {
        flatPieces.add(p)
      }
    }

    // Sort descending by length (Best-Fit Decreasing heuristic)
    flatPieces.sortByDescending { it.lengthMm }

    val usableStockPerBar = stockLengthMm - endTrimMm

    // Each bar tracks its remaining length and assigned cuts
    class BarState(val index: Int) {
      var remainingMm = usableStockPerBar
      val assignedCuts = mutableListOf<CutPiece>()
      var cutsCount = 0

      fun canFit(cutLengthMm: Int): Boolean {
        val needed = cutLengthMm + (if (cutsCount > 0) sawKerfMm else 0)
        return remainingMm >= needed
      }

      fun add(cut: CutPiece) {
        val needed = cut.lengthMm + (if (cutsCount > 0) sawKerfMm else 0)
        remainingMm -= needed
        assignedCuts.add(cut)
        cutsCount++
      }
    }

    val bars = mutableListOf<BarState>()

    for (cut in flatPieces) {
      if (cut.lengthMm > usableStockPerBar) {
        // Cut is longer than a single stock bar! Needs splicing or oversize bar
        val bar = BarState(bars.size + 1)
        bar.add(cut)
        bars.add(bar)
        continue
      }

      // Best-Fit: find the bar where remaining space after placing this cut is minimal
      var bestBar: BarState? = null
      var minRemainingAfter = Int.MAX_VALUE

      for (b in bars) {
        if (b.canFit(cut.lengthMm)) {
          val needed = cut.lengthMm + (if (b.cutsCount > 0) sawKerfMm else 0)
          val rem = b.remainingMm - needed
          if (rem < minRemainingAfter) {
            minRemainingAfter = rem
            bestBar = b
          }
        }
      }

      if (bestBar != null) {
        bestBar.add(cut)
      } else {
        // Open a new stock bar
        val newBar = BarState(bars.size + 1)
        newBar.add(cut)
        bars.add(newBar)
      }
    }

    val stockBarPlans = bars.map { b ->
      val totalCutLength = b.assignedCuts.sumOf { it.lengthMm }
      val totalKerf = if (b.cutsCount > 1) (b.cutsCount - 1) * sawKerfMm else 0
      val usedMm = endTrimMm + totalCutLength + totalKerf
      val wasteMm = stockLengthMm - usedMm
      val wastePercent = (wasteMm.toFloat() / stockLengthMm.toFloat()) * 100f
      val isReusable = wasteMm >= REUSABLE_OFFCUT_THRESHOLD_MM

      StockBarPlan(
        barIndex = b.index,
        profileName = profileName,
        stockLengthMm = stockLengthMm,
        cuts = b.assignedCuts,
        usedLengthMm = usedMm,
        wasteLengthMm = wasteMm,
        wastePercent = wastePercent,
        isReusableOffcut = isReusable
      )
    }

    val totalPieces = flatPieces.size
    val totalRequiredLen = flatPieces.sumOf { it.lengthMm }
    val totalStockUsed = stockBarPlans.size * stockLengthMm
    val totalWaste = stockBarPlans.sumOf { it.wasteLengthMm }
    val overallWastePct = if (totalStockUsed > 0) {
      (totalWaste.toFloat() / totalStockUsed.toFloat()) * 100f
    } else 0f

    return CuttingOptimizationResult(
      profileName = profileName,
      stockLengthMm = stockLengthMm,
      sawKerfMm = sawKerfMm,
      endTrimMm = endTrimMm,
      stockBars = stockBarPlans,
      totalPiecesCount = totalPieces,
      totalRequiredLengthMm = totalRequiredLen,
      totalStockUsedLengthMm = totalStockUsed,
      totalWasteLengthMm = totalWaste,
      overallWastePercent = overallWastePct
    )
  }
}
