package com.example

import com.example.core.SecurityHelper
import com.example.core.UserRole
import org.junit.Assert.*
import org.junit.Test

class UserRoleSecurityTest {

  @Test
  fun testPasswordHashingAndVerification() {
    val password = "workshopSecret@2026"
    val salt = SecurityHelper.generateSalt()
    val hash = SecurityHelper.hashPassword(password, salt)

    assertTrue(SecurityHelper.verifyPassword(password, salt, hash))
    assertFalse(SecurityHelper.verifyPassword("wrongPassword", salt, hash))
  }

  @Test
  fun testUserRolePermissions() {
    val admin = UserRole.OWNER_ADMIN
    assertTrue(admin.canManageUsers())
    assertTrue(admin.canEditCatalogAndPrices())
    assertTrue(admin.canApproveInvoices())
    assertTrue(admin.canExportOrBackup())

    val sales = UserRole.SALES
    assertFalse(sales.canManageUsers())
    assertTrue(sales.canCreateInvoices())
    assertFalse(sales.canApproveInvoices())

    val operator = UserRole.OPERATOR
    assertFalse(operator.canCreateInvoices())
    assertTrue(operator.canUpdateProduction())

    val readOnly = UserRole.READ_ONLY
    assertFalse(readOnly.canManageUsers())
    assertFalse(readOnly.canCreateInvoices())
    assertFalse(readOnly.canUpdateProduction())
  }
}
