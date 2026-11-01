package com.example

import com.example.data.local.AppDao
import com.example.data.local.DesignEntity
import com.example.data.repository.WorkshopRepository
import com.example.domain.engine.DesignEditor
import com.example.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DesignPersistenceTest {
  private val dao = Proxy.newProxyInstance(AppDao::class.java.classLoader, arrayOf(AppDao::class.java)) { _, _, _ -> error("No database calls expected") } as AppDao
  private val repository = WorkshopRepository(dao)
  private fun serialize(design: WindowDesign): DesignEntity {
    val method = WorkshopRepository::class.java.getDeclaredMethod("serializeDesign", WindowDesign::class.java).apply { isAccessible = true }
    return method.invoke(repository, design) as DesignEntity
  }
  private fun deserialize(entity: DesignEntity): WindowDesign {
    val method = WorkshopRepository::class.java.getDeclaredMethod("deserializeDesign", DesignEntity::class.java).apply { isAccessible = true }
    return method.invoke(repository, entity) as WindowDesign
  }
  @Test fun savesAndReopensNestedLayoutAndCustomHardware() {
    val initial = WindowDesign("p", "door", productType = ProductType.DOOR, widthMm = 2400, heightMm = 2200,
      openingDirection = OpeningDirection.OUTWARD,
      panels = listOf(PanelConfig("a", OpeningType.CASEMENT_LEFT, handleX = 0.8f, handleY = 0.7f, hasHandle = false, lockPoints = 3)))
    val design = DesignEditor.split(DesignEditor.split(initial, 0, SplitAxis.VERTICAL), 1, SplitAxis.HORIZONTAL)
    assertEquals(design, deserialize(serialize(design)))
  }
  @Test fun oldPanelJsonLoadsWithCompatibleDefaults() {
    val entity = serialize(WindowDesign("p", "old")).copy(panelsJson = """[{"id":"old","openingType":"FIXED"}]""")
    val design = deserialize(entity)
    assertNull(design.layoutRoot)
    assertEquals(1, design.panels.size)
    assertTrue(design.panels.first().hasHandle)
    assertEquals(0.5f, design.panels.first().handleY, 0f)
  }
}
