package com.example

import com.example.domain.engine.CuttingOptimizer
import com.example.domain.model.CutPiece
import org.junit.Assert.*
import org.junit.Test

class CuttingOptimizerTest {

  @Test
  fun testCuttingStockOptimizationWithKerfAndTrim() {
    val cuts = listOf(
      CutPiece("1", "فریم", "فریم", 1400, 2, 45, 45, "افقی"),
      CutPiece("2", "فریم", "فریم", 1600, 2, 45, 45, "عمودی")
    )

    val result = CuttingOptimizer.optimize(
      pieces = cuts,
      stockLengthMm = 6000,
      sawKerfMm = 4,
      endTrimMm = 20
    )

    // Total required: 1400*2 + 1600*2 = 2800 + 3200 = 6000mm.
    // Usable bar is 6000 - 20 = 5980mm, with kerf 3*4=12mm, total needed is 6012mm -> requires 2 bars!
    assertEquals(2, result.stockBars.size)
    assertEquals(4, result.totalPiecesCount)
    assertTrue(result.overallWastePercent > 0f)
  }

  @Test
  fun testReusableOffcutDetection() {
    val cuts = listOf(
      CutPiece("1", "فریم", "فریم", 2000, 1, 45, 45, "تست")
    )

    val result = CuttingOptimizer.optimize(
      pieces = cuts,
      stockLengthMm = 6000,
      sawKerfMm = 4,
      endTrimMm = 20
    )

    assertEquals(1, result.stockBars.size)
    val bar = result.stockBars[0]
    // Used: 20 + 2000 = 2020mm. Remainder = 3980mm >= 500mm -> Reusable!
    assertEquals(3980, bar.wasteLengthMm)
    assertTrue(bar.isReusableOffcut)
  }
}
