package com.example.domain.engine

import com.example.domain.model.*
import java.util.UUID

enum class CadTemplateCategory {
  ALL,
  FIXED,
  SLIDING,
  CASEMENT,
  DOOR,
  COMBO,
  ARCHED
}

data class CadTemplate(
  val id: String,
  val nameKey: String,
  val category: CadTemplateCategory,
  val productType: ProductType,
  val defaultWidthMm: Int,
  val defaultHeightMm: Int,
  val hasTransom: Boolean = false,
  val defaultTransomHeightMm: Int = 400,
  val panelCount: Int,
  val createDesign: (
    id: String,
    name: String,
    widthMm: Int,
    heightMm: Int,
    profileSystem: String,
    frameFinish: FrameFinish,
    sashFinish: FrameFinish
  ) -> WindowDesign
)

object CadTemplates {

  val templates: List<CadTemplate> = listOf(
    // 1. Single Casement
    CadTemplate(
      id = "casement_1_sash",
      nameKey = "tmpl_casement_1_sash",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 800,
      defaultHeightMm = 1400,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = false,
          panels = listOf(PanelConfig(id = p1, openingType = OpeningType.CASEMENT_LEFT)),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 2. Fixed Picture Window
    CadTemplate(
      id = "fixed_picture_1",
      nameKey = "tmpl_fixed_1",
      category = CadTemplateCategory.FIXED,
      productType = ProductType.WINDOW,
      defaultWidthMm = 1200,
      defaultHeightMm = 1500,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = false,
          panels = listOf(PanelConfig(id = p1, openingType = OpeningType.FIXED)),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 3. 2-Panel Fixed + Casement
    CadTemplate(
      id = "casement_2_fixed_sash",
      nameKey = "tmpl_casement_2_fixed_sash",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 1400,
      defaultHeightMm = 1600,
      panelCount = 2,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.FIXED, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.CASEMENT_LEFT, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(panelId = p2)
          )
        )
      }
    ),

    // 4. 2-Panel Double Casement (French Window)
    CadTemplate(
      id = "casement_2_double_sash",
      nameKey = "tmpl_casement_2_double",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 1600,
      defaultHeightMm = 1600,
      panelCount = 2,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.CASEMENT_RIGHT, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.CASEMENT_LEFT, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(panelId = p2)
          )
        )
      }
    ),

    // 5. 3-Panel Fixed-Sash-Fixed (Villa Window)
    CadTemplate(
      id = "casement_3_fixed_sash_fixed",
      nameKey = "tmpl_casement_3_fixed_sash_fixed",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 2100,
      defaultHeightMm = 1600,
      panelCount = 3,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        val p3 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.FIXED, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.TILT_AND_TURN, widthRatio = 1.3f),
            PanelConfig(id = p3, openingType = OpeningType.FIXED, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.30f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(
              axis = SplitAxis.VERTICAL,
              fraction = 0.57f,
              first = LayoutNode(panelId = p2),
              second = LayoutNode(panelId = p3)
            )
          )
        )
      }
    ),

    // 6. 2-Track Sliding (2 Leaves)
    CadTemplate(
      id = "sliding_2_track_2_leaf",
      nameKey = "tmpl_sliding_2_leaf",
      category = CadTemplateCategory.SLIDING,
      productType = ProductType.WINDOW,
      defaultWidthMm = 1600,
      defaultHeightMm = 1400,
      panelCount = 2,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.SLIDING_RIGHT, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.SLIDING_LEFT, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(panelId = p2)
          )
        )
      }
    ),

    // 7. 2-Track Sliding (3 Leaves)
    CadTemplate(
      id = "sliding_2_track_3_leaf",
      nameKey = "tmpl_sliding_3_leaf",
      category = CadTemplateCategory.SLIDING,
      productType = ProductType.WINDOW,
      defaultWidthMm = 2400,
      defaultHeightMm = 1500,
      panelCount = 3,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        val p3 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.SLIDING_RIGHT, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.FIXED, widthRatio = 1f),
            PanelConfig(id = p3, openingType = OpeningType.SLIDING_LEFT, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.333f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(
              axis = SplitAxis.VERTICAL,
              fraction = 0.5f,
              first = LayoutNode(panelId = p2),
              second = LayoutNode(panelId = p3)
            )
          )
        )
      }
    ),

    // 8. 2-Track Sliding (4 Leaves Center Opening)
    CadTemplate(
      id = "sliding_2_track_4_leaf",
      nameKey = "tmpl_sliding_4_leaf",
      category = CadTemplateCategory.SLIDING,
      productType = ProductType.WINDOW,
      defaultWidthMm = 3200,
      defaultHeightMm = 1600,
      panelCount = 4,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        val p3 = "p_" + UUID.randomUUID().toString().take(6)
        val p4 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.FIXED, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.SLIDING_LEFT, widthRatio = 1f),
            PanelConfig(id = p3, openingType = OpeningType.SLIDING_RIGHT, widthRatio = 1f),
            PanelConfig(id = p4, openingType = OpeningType.FIXED, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(
              axis = SplitAxis.VERTICAL,
              fraction = 0.5f,
              first = LayoutNode(panelId = p1),
              second = LayoutNode(panelId = p2)
            ),
            second = LayoutNode(
              axis = SplitAxis.VERTICAL,
              fraction = 0.5f,
              first = LayoutNode(panelId = p3),
              second = LayoutNode(panelId = p4)
            )
          )
        )
      }
    ),

    // 9. Tilt & Turn Single
    CadTemplate(
      id = "tilt_and_turn_1",
      nameKey = "tmpl_tilt_turn_1",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 900,
      defaultHeightMm = 1400,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = false,
          panels = listOf(PanelConfig(id = p1, openingType = OpeningType.TILT_AND_TURN)),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 10. Top Hung Awning
    CadTemplate(
      id = "top_hung_1",
      nameKey = "tmpl_top_hung_1",
      category = CadTemplateCategory.CASEMENT,
      productType = ProductType.WINDOW,
      defaultWidthMm = 650,
      defaultHeightMm = 650,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = false,
          panels = listOf(PanelConfig(id = p1, openingType = OpeningType.TOP_HUNG)),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 11. Single Door with Top Transom
    CadTemplate(
      id = "door_single_transom",
      nameKey = "tmpl_door_single_transom",
      category = CadTemplateCategory.DOOR,
      productType = ProductType.DOOR,
      defaultWidthMm = 950,
      defaultHeightMm = 2400,
      hasTransom = true,
      defaultTransomHeightMm = 450,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.DOOR,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = true,
          transomHeightMm = 450,
          hasMullionDivision = false,
          panels = listOf(
            PanelConfig(
              id = p1,
              openingType = OpeningType.CASEMENT_LEFT,
              openingDirection = OpeningDirection.OUTWARD
            )
          ),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 12. French Double Door with Top Transom
    CadTemplate(
      id = "door_double_french_transom",
      nameKey = "tmpl_door_double_transom",
      category = CadTemplateCategory.DOOR,
      productType = ProductType.DOOR,
      defaultWidthMm = 1800,
      defaultHeightMm = 2400,
      hasTransom = true,
      defaultTransomHeightMm = 450,
      panelCount = 2,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.DOOR,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          widthMm = w,
          heightMm = h,
          hasTransom = true,
          transomHeightMm = 450,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(
              id = p1,
              openingType = OpeningType.CASEMENT_RIGHT,
              openingDirection = OpeningDirection.OUTWARD,
              widthRatio = 1f
            ),
            PanelConfig(
              id = p2,
              openingType = OpeningType.CASEMENT_LEFT,
              openingDirection = OpeningDirection.OUTWARD,
              widthRatio = 1f
            )
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(panelId = p2)
          )
        )
      }
    ),

    // 13. Arched Single Casement
    CadTemplate(
      id = "arched_single_casement",
      nameKey = "tmpl_arched_single",
      category = CadTemplateCategory.ARCHED,
      productType = ProductType.WINDOW,
      defaultWidthMm = 900,
      defaultHeightMm = 1600,
      panelCount = 1,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          windowShape = WindowShape.ARCHED,
          archHeightMm = 350,
          widthMm = w,
          heightMm = h,
          panels = listOf(PanelConfig(id = p1, openingType = OpeningType.CASEMENT_LEFT)),
          layoutRoot = LayoutNode(panelId = p1)
        )
      }
    ),

    // 14. Arched Double French Window / Door
    CadTemplate(
      id = "arched_double_french",
      nameKey = "tmpl_arched_double",
      category = CadTemplateCategory.ARCHED,
      productType = ProductType.WINDOW,
      defaultWidthMm = 1600,
      defaultHeightMm = 2100,
      panelCount = 2,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.WINDOW,
          profileSystem = sys,
          frameFinish = frame,
          sashFinish = sash,
          windowShape = WindowShape.ARCHED,
          archHeightMm = 400,
          widthMm = w,
          heightMm = h,
          hasTransom = false,
          hasMullionDivision = true,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.CASEMENT_RIGHT, widthRatio = 1f),
            PanelConfig(id = p2, openingType = OpeningType.CASEMENT_LEFT, widthRatio = 1f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.5f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(panelId = p2)
          )
        )
      }
    ),

    // 15. Window and Door Combo (Grand Villa Facade) - Matching Mockup
    CadTemplate(
      id = "combo_grand_villa_facade",
      nameKey = "tmpl_combo_facade",
      category = CadTemplateCategory.COMBO,
      productType = ProductType.DOOR,
      defaultWidthMm = 2600,
      defaultHeightMm = 2400,
      hasTransom = true,
      defaultTransomHeightMm = 600,
      panelCount = 4,
      createDesign = { id, name, w, h, sys, frame, sash ->
        val p1 = "p_" + UUID.randomUUID().toString().take(6)
        val p2 = "p_" + UUID.randomUUID().toString().take(6)
        val p3 = "p_" + UUID.randomUUID().toString().take(6)
        val p4 = "p_" + UUID.randomUUID().toString().take(6)
        WindowDesign(
          id = id,
          name = name,
          productType = ProductType.DOOR,
          profileSystem = sys,
          frameFinish = FrameFinish.ANTHRACITE,
          sashFinish = FrameFinish.ANTHRACITE,
          widthMm = w,
          heightMm = h,
          hasTransom = true,
          transomHeightMm = 600,
          hasGrids = true,
          gridRows = 2,
          gridCols = 2,
          panels = listOf(
            PanelConfig(id = p1, openingType = OpeningType.FIXED, widthRatio = 0.75f),
            PanelConfig(id = p2, openingType = OpeningType.CASEMENT_RIGHT, widthRatio = 1.1f),
            PanelConfig(id = p3, openingType = OpeningType.CASEMENT_LEFT, widthRatio = 1.1f),
            PanelConfig(id = p4, openingType = OpeningType.FIXED, widthRatio = 0.75f)
          ),
          layoutRoot = LayoutNode(
            axis = SplitAxis.VERTICAL,
            fraction = 0.25f,
            first = LayoutNode(panelId = p1),
            second = LayoutNode(
              axis = SplitAxis.VERTICAL,
              fraction = 0.66f,
              first = LayoutNode(
                axis = SplitAxis.VERTICAL,
                fraction = 0.5f,
                first = LayoutNode(panelId = p2),
                second = LayoutNode(panelId = p3)
              ),
              second = LayoutNode(panelId = p4)
            )
          )
        )
      }
    )
  )

  fun findById(id: String): CadTemplate? = templates.find { it.id == id }

  fun applyTemplate(current: WindowDesign, template: CadTemplate): WindowDesign {
    return template.createDesign(
      current.id,
      current.name,
      template.defaultWidthMm,
      template.defaultHeightMm,
      current.profileSystem,
      current.frameFinish,
      current.sashFinish
    ).copy(
      notes = current.notes,
      outerGlassTint = current.outerGlassTint,
      innerGlassTint = current.innerGlassTint,
      // A template only changes the layout; the chosen material family and system stay intact.
      materialFamily = current.materialFamily,
      profileFaceWidthMm = current.profileFaceWidthMm,
      hasThermalBreak = current.hasThermalBreak
    )
  }
}
