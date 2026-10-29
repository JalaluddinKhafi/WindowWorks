package com.example

import com.example.ui.Screen
import com.example.ui.ScreenPermissions
import com.example.ui.isScreenAllowed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenAccessTest {

  private val admin = ScreenPermissions(
    canManageProjects = true,
    canDesign = true,
    canCreateInvoices = true,
    canRecordPayments = true,
    canEditCatalog = true,
    canManageInventory = true,
    canManageUsers = true,
    canBackup = true
  )

  private val reader = ScreenPermissions(
    canManageProjects = false,
    canDesign = false,
    canCreateInvoices = false,
    canRecordPayments = false,
    canEditCatalog = false,
    canManageInventory = false,
    canManageUsers = false,
    canBackup = false
  )

  @Test
  fun adminCanOpenEveryRestrictedScreen() {
    listOf(
      Screen.USERS,
      Screen.CATALOG,
      Screen.MATERIALS_CATALOG,
      Screen.INVENTORY,
      Screen.PROJECT_CALCULATOR,
      Screen.QUOTATION,
      Screen.DESIGNER
    ).forEach { assertTrue("admin should open $it", isScreenAllowed(it, admin)) }
  }

  @Test
  fun readOnlyUserIsBlockedFromRestrictedScreens() {
    listOf(
      Screen.USERS,
      Screen.CATALOG,
      Screen.MATERIALS_CATALOG,
      Screen.INVENTORY,
      Screen.PROJECT_CALCULATOR,
      Screen.QUOTATION,
      Screen.DESIGNER
    ).forEach { assertFalse("reader must not open $it", isScreenAllowed(it, reader)) }
  }

  @Test
  fun readOnlyUserCanStillViewDashboardAndFinance() {
    assertTrue(isScreenAllowed(Screen.DASHBOARD, reader))
    assertTrue(isScreenAllowed(Screen.FINANCE, reader))
    assertTrue(isScreenAllowed(Screen.PROJECTS, reader))
  }
}
