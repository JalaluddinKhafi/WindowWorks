package com.example

import com.example.data.backup.BackupManager
import com.example.data.local.CustomerEntity
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric because [BackupManager] uses `org.json`, which is only a stub in plain
 * JVM unit tests (every method would throw "not mocked").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupManagerTest {

  private fun sampleBackupJson(): String = JSONObject().apply {
    put("app", "WindowWorkshop")
    put("schemaVersion", 2)
    put("exportedAt", 1700000000000L)
    put(
      "users",
      JSONArray().put(
        JSONObject().apply {
          put("id", "user_admin")
          put("username", "admin")
          put("fullName", "مدیر")
          put("passwordHash", "pbkdf2\u002412000\u0024deadbeef")
          put("salt", "abc")
          put("role", "OWNER_ADMIN")
          put("isActive", true)
          put("createdAt", 1L)
        }
      )
    )
    put(
      "customers",
      JSONArray().put(
        JSONObject().apply {
          put("id", "c1")
          put("code", "CST-101")
          put("fullName", "مشتری آزمایشی")
          put("phone", "0700000000")
          put("address", "")
          put("notes", "")
          put("isActive", true)
          put("createdAt", 1L)
          put("updatedAt", 1L)
        }
      )
    )
    put(
      "projects",
      JSONArray().put(
        JSONObject().apply {
          put("id", "p1")
          put("projectNumber", "PRJ-1")
          put("customerId", "c1")
          put("title", "پروژه")
          put("siteAddress", "")
          put("status", "DRAFT")
          put("contractCurrency", "USD")
          put("createdAt", 1L)
          put("notes", "")
          put("totalContractAmount", "1000.00")
          put("totalPaidAmount", "0.00")
          put("balanceDue", "1000.00")
        }
      )
    )
    put(
      "payments",
      JSONArray().put(
        JSONObject().apply {
          put("id", "pay1")
          put("customerId", "c1")
          put("projectId", "p1")
          put("amount", "100.00")
          put("currency", "USD")
          put("exchangeRateToAfn", "70.00")
          put("paymentMethod", "نقدی")
          put("referenceNumber", "")
          put("paymentDate", 1L)
          put("notes", "")
        }
      )
    )
    put(
      "settings",
      JSONArray().put(
        JSONObject().apply {
          put("key", "workshop_name")
          put("value", "کارگاه تست")
        }
      )
    )
  }.toString()

  @Test
  fun parsesCompleteBackup() {
    val snapshot = BackupManager.parse(sampleBackupJson())

    assertEquals(1, snapshot.users.size)
    assertEquals("OWNER_ADMIN", snapshot.users.first().role)
    assertEquals(1, snapshot.customers.size)
    assertEquals("USD", snapshot.projects.first().contractCurrency)
    assertEquals("70.00", snapshot.payments.first().exchangeRateToAfn)
    assertEquals("کارگاه تست", snapshot.settings.first().value)
  }

  @Test
  fun rejectsBackupWithoutUsers() {
    val json = JSONObject(sampleBackupJson()).put("users", JSONArray()).toString()
    val error = runCatching { BackupManager.parse(json) }.exceptionOrNull()
    assertNotNull(error)
    assertTrue(error is BackupManager.BackupException)
  }

  @Test
  fun rejectsForeignKeyApp() {
    val json = JSONObject(sampleBackupJson()).put("app", "OtherApp").toString()
    val error = runCatching { BackupManager.parse(json) }.exceptionOrNull()
    assertTrue(error is BackupManager.BackupException)
  }

  @Test
  fun rejectsNonJsonInput() {
    val error = runCatching { BackupManager.parse("این یک فایل json نیست") }.exceptionOrNull()
    assertTrue(error is BackupManager.BackupException)
  }

  @Test
  fun rejectsNewerSchemaVersion() {
    val json = JSONObject(sampleBackupJson()).put("schemaVersion", 99).toString()
    val error = runCatching { BackupManager.parse(json) }.exceptionOrNull()
    assertTrue(error is BackupManager.BackupException)
  }

  @Test
  fun customerFieldsRoundTrip() {
    val customer = CustomerEntity(
      id = "c9", code = "CST-9", fullName = "علی", phone = "0700",
      address = "کابل", notes = "یادداشت"
    )
    val snapshot = BackupManager.parse(
      JSONObject().apply {
        put("app", "WindowWorkshop")
        put("schemaVersion", 2)
        put("users", JSONArray().put(JSONObject().put("id", "u").put("username", "u")))
        put("customers", JSONArray().put(JSONObject().put("id", customer.id).put("code", customer.code).put("fullName", customer.fullName).put("phone", customer.phone).put("address", customer.address).put("notes", customer.notes)))
      }.toString()
    )
    val restored = snapshot.customers.first()
    assertEquals(customer.fullName, restored.fullName)
    assertEquals(customer.address, restored.address)
  }

  @Test
  fun missingOptionalCollectionsAreTreatedAsEmpty() {
    val json = JSONObject().apply {
      put("app", "WindowWorkshop")
      put("schemaVersion", 1)
      put("users", JSONArray().put(JSONObject().put("id", "u").put("username", "u")))
      put("customers", JSONArray().put(JSONObject().put("id", "c").put("fullName", "x")))
    }.toString()

    val snapshot = BackupManager.parse(json)
    assertTrue(snapshot.invoices.isEmpty())
    assertTrue(snapshot.payments.isEmpty())
    assertTrue(snapshot.settings.isEmpty())
  }
}
