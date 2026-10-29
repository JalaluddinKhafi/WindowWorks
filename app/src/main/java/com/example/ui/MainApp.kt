package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.AppSettings
import com.example.core.UserRole
import com.example.data.repository.WorkshopRepository
import com.example.domain.model.*
import com.example.ui.screens.*
import kotlinx.coroutines.launch

enum class Screen(val titlePersian: String, val icon: ImageVector) {
  DASHBOARD("داشبورد", Icons.Default.Dashboard),
  PROJECTS("پروژه‌ها", Icons.Default.FolderOpen),
  DESIGNER("طراحی ۲D/3D", Icons.Default.CropLandscape),
  PROJECT_CALCULATOR("محاسبه پروژه", Icons.Default.Calculate),
  FINANCE("حسابداری", Icons.Default.AccountBalanceWallet),
  CUSTOMERS("مشتریان", Icons.Default.People),
  MATERIALS_CATALOG("اجناس و مواد", Icons.Default.Category),
  MORE("تنظیمات", Icons.Default.Settings),
  PROJECT_DETAIL("جزئیات پروژه", Icons.Default.Folder),
  CUTTING_PLAN("نقشه برش", Icons.Default.ContentCut),
  QUOTATION("پیش‌فاکتور", Icons.Default.ReceiptLong),
  MANUFACTURING_INSTRUCTIONS("نقشه کارگاهی", Icons.Default.Engineering),
  USERS("کاربران", Icons.Default.ManageAccounts),
  CATALOG("کاتالوگ متریال", Icons.Default.ListAlt),
  INVENTORY("انبارداری", Icons.Default.Inventory2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(repository: WorkshopRepository) {
  val scope = rememberCoroutineScope()

  // -------------------------------------------------------------------------------------------
  // Data flows from Room
  // -------------------------------------------------------------------------------------------
  val projects by repository.getAllProjects().collectAsState(initial = emptyList())
  val customers by repository.getAllCustomers().collectAsState(initial = emptyList())
  val profiles by repository.getAllProfiles().collectAsState(initial = emptyList())
  val glasses by repository.getAllGlasses().collectAsState(initial = emptyList())
  val accessories by repository.getAllAccessories().collectAsState(initial = emptyList())
  val materialProducts by repository.getAllMaterialProducts().collectAsState(initial = emptyList())
  val allInvoices by repository.getAllInvoices().collectAsState(initial = emptyList())
  val allPayments by repository.getAllPayments().collectAsState(initial = emptyList())
  val allExpenses by repository.getAllExpenses().collectAsState(initial = emptyList())
  val inventoryMovements by repository.getInventoryMovements().collectAsState(initial = emptyList())
  val users by repository.getAllUsers().collectAsState(initial = emptyList())
  val auditLogs by repository.getRecentAuditLogs().collectAsState(initial = emptyList())
  val settings by repository.getSettings().collectAsState(initial = AppSettings())

  // A brand new installation has exactly one user and nobody has changed the bootstrap password.
  val isFirstRun = users.size <= 1 && !repository.isSignedIn

  // -------------------------------------------------------------------------------------------
  // Session state mirrors the repository session (see the `sessionState` collector below).
  // -------------------------------------------------------------------------------------------
  var isSignedIn by remember { mutableStateOf(repository.isSignedIn) }
  var currentRole by remember { mutableStateOf(repository.currentRole) }
  var currentFullName by remember { mutableStateOf(repository.currentFullName) }
  var mustChangePassword by remember { mutableStateOf(repository.mustChangePassword) }
  var sessionUserId by remember { mutableStateOf(repository.currentUserId) }
  var showChangePasswordDialog by remember { mutableStateOf(false) }
  var accessDeniedMessage by remember { mutableStateOf<String?>(null) }

  // Navigation state
  var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
  var selectedProjectId by remember { mutableStateOf<String?>(null) }

  // Design studio state
  var activeDesign by remember {
    mutableStateOf(
      WindowDesign(
        id = "des_default",
        name = "پنجره دوجداره پذیرایی",
        widthMm = 1500,
        heightMm = 1700,
        profileSystem = "Wintech 6000",
        frameFinish = FrameFinish.WHITE,
        sashFinish = FrameFinish.WHITE,
        hasTransom = false,
        panels = listOf(
          PanelConfig("p1", OpeningType.FIXED),
          PanelConfig("p2", OpeningType.CASEMENT_LEFT)
        )
      )
    )
  }

  // Selected project items & finances
  val selectedProject = projects.find { it.id == selectedProjectId }
  val projectItems by remember(selectedProjectId) {
    val id = selectedProjectId
    if (id != null) repository.getProjectItems(id)
    else kotlinx.coroutines.flow.flowOf(emptyList<ProjectItemModel>())
  }.collectAsState(initial = emptyList())

  // Restore the previous session after an app restart, then keep Compose mirrors in sync with the
  // repository session (login, logout, role change, backup restore, ...).
  LaunchedEffect(true) {
    if (!repository.isSignedIn) repository.restoreSession()
    repository.sessionState.collect {
      isSignedIn = repository.isSignedIn
      currentRole = repository.currentRole
      currentFullName = repository.currentFullName
      mustChangePassword = repository.mustChangePassword
      sessionUserId = repository.currentUserId
    }
  }



  if (!isSignedIn) {
    LoginScreen(
      workshopName = settings.workshopName,
      isFirstRun = isFirstRun,
      onLogin = { username, password ->
        val error = repository.login(username, password)
        if (error == null) {
          isSignedIn = true
          currentRole = repository.currentRole
          currentFullName = repository.currentFullName
          mustChangePassword = repository.mustChangePassword
          currentScreen = Screen.DASHBOARD
        }
        error
      }
    )
    return
  }

  val can = remember(currentRole) {
    ScreenPermissions(
      canManageProjects = currentRole.canManageProjects(),
      canDesign = currentRole.canDesignWindows(),
      canCreateInvoices = currentRole.canCreateInvoices(),
      canRecordPayments = currentRole.canRecordPayments(),
      canEditCatalog = currentRole.canEditCatalogAndPrices(),
      canManageInventory = currentRole.canManageInventory(),
      canManageUsers = currentRole.canManageUsers(),
      canBackup = currentRole.canExportOrBackup()
    )
  }

  fun navigate(target: Screen) {
    if (isScreenAllowed(target, can)) {
      currentScreen = target
    } else {
      accessDeniedMessage = target.titlePersian
    }
  }

  BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
    currentScreen = when (currentScreen) {
      Screen.PROJECT_DETAIL -> Screen.PROJECTS
      Screen.CUTTING_PLAN, Screen.QUOTATION, Screen.MANUFACTURING_INSTRUCTIONS -> Screen.DESIGNER
      Screen.USERS, Screen.CATALOG, Screen.INVENTORY, Screen.MATERIALS_CATALOG -> Screen.MORE
      Screen.PROJECT_CALCULATOR -> Screen.DASHBOARD
      else -> Screen.DASHBOARD
    }
  }

  if (mustChangePassword) {
    ChangePasswordDialog(
      forceChange = true,
      onChangePassword = { old, new -> repository.changePassword(old, new) },
      onDismiss = {},
      onLogout = { scope.launch { repository.logout() } }
    )
  } else if (showChangePasswordDialog) {
    ChangePasswordDialog(
      forceChange = false,
      onChangePassword = { old, new -> repository.changePassword(old, new) },
      onDismiss = { showChangePasswordDialog = false },
      onLogout = null
    )
  }

  accessDeniedMessage?.let { screenTitle ->
    AlertDialog(
      onDismissRequest = { accessDeniedMessage = null },
      title = { Text("عدم دسترسی") },
      text = {
        Text(
          "نقش «${currentRole.titlePersian}» اجازه دسترسی به بخش «$screenTitle» را ندارد."
        )
      },
      confirmButton = {
        TextButton(onClick = { accessDeniedMessage = null }) { Text("متوجه شدم") }
      }
    )
  }

  // Force Right-to-Left (RTL) Layout for Persian UI
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      topBar = {
        if (currentScreen in listOf(Screen.DASHBOARD, Screen.PROJECTS, Screen.FINANCE, Screen.CUSTOMERS, Screen.MORE)) {
          TopAppBar(
            title = {
              Column {
                Text(
                  text = currentScreen.titlePersian,
                  style = MaterialTheme.typography.titleMedium
                )
                Text(
                  text = "${currentFullName} • ${currentRole.titlePersian}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            },
            actions = {
              if (can.canCreateInvoices) {
                IconButton(onClick = { navigate(Screen.PROJECT_CALCULATOR) }) {
                  Icon(Icons.Default.Calculate, contentDescription = "محاسبه پروژه")
                }
              }
              if (can.canEditCatalog) {
                IconButton(onClick = { navigate(Screen.MATERIALS_CATALOG) }) {
                  Icon(Icons.Default.Category, contentDescription = "اجناس و مواد")
                }
                IconButton(onClick = { navigate(Screen.CATALOG) }) {
                  Icon(Icons.Default.MenuBook, contentDescription = "کاتالوگ")
                }
              }
              if (can.canManageInventory) {
                IconButton(onClick = { navigate(Screen.INVENTORY) }) {
                  Icon(Icons.Default.Inventory2, contentDescription = "انبار")
                }
              }
              IconButton(onClick = { showChangePasswordDialog = true }) {
                Icon(Icons.Default.Key, contentDescription = "تغییر رمز عبور")
              }
              IconButton(
                onClick = {
                  scope.launch { repository.logout() }
                  currentScreen = Screen.DASHBOARD
                  selectedProjectId = null
                }
              ) {
                Icon(Icons.Default.Logout, contentDescription = "خروج از حساب")
              }
            }
          )
        }
      },
      bottomBar = {
        NavigationBar {
          val navItems = mutableListOf(
            Screen.DASHBOARD,
            Screen.PROJECTS,
            Screen.DESIGNER,
            Screen.FINANCE,
            Screen.CUSTOMERS
          )
          if (can.canCreateInvoices) navItems.add(2, Screen.PROJECT_CALCULATOR)
          navItems.add(Screen.MORE)

          navItems.forEach { screen ->
            val isSelected = currentScreen == screen ||
                (screen == Screen.PROJECTS && currentScreen == Screen.PROJECT_DETAIL) ||
                (screen == Screen.DESIGNER && (currentScreen == Screen.CUTTING_PLAN || currentScreen == Screen.QUOTATION || currentScreen == Screen.MANUFACTURING_INSTRUCTIONS)) ||
                (screen == Screen.MORE && (currentScreen in listOf(Screen.USERS, Screen.CATALOG, Screen.INVENTORY, Screen.MATERIALS_CATALOG)))

            NavigationBarItem(
              selected = isSelected,
              onClick = { navigate(screen) },
              icon = { Icon(screen.icon, contentDescription = screen.titlePersian) },
              label = { Text(screen.titlePersian, style = MaterialTheme.typography.labelSmall) }
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentScreen) {
          Screen.DASHBOARD -> {
            DashboardScreen(
              projects = projects,
              onNavigateToProjects = { navigate(Screen.PROJECTS) },
              onNavigateToCustomers = { navigate(Screen.CUSTOMERS) },
              onNavigateToDesigner = { navigate(Screen.DESIGNER) },
              onNavigateToCutting = { navigate(Screen.CUTTING_PLAN) },
              onNavigateToFinance = { navigate(Screen.FINANCE) },
              onNavigateToProjectCalculator = { navigate(Screen.PROJECT_CALCULATOR) },
              onNavigateToMaterialsCatalog = { navigate(Screen.MATERIALS_CATALOG) },
              onSelectProject = { projId ->
                selectedProjectId = projId
                currentScreen = Screen.PROJECT_DETAIL
              }
            )
          }

          Screen.PROJECTS -> {
            ProjectListScreen(
              projects = projects,
              customers = customers,
              onSelectProject = { projId ->
                selectedProjectId = projId
                currentScreen = Screen.PROJECT_DETAIL
              },
              onSaveProject = { newP ->
                scope.launch { repository.saveProject(newP) }
              }
            )
          }

          Screen.PROJECT_DETAIL -> {
            if (selectedProject != null) {
              val customer = customers.find { it.id == selectedProject.customerId }
              ProjectDetailScreen(
                project = selectedProject,
                items = projectItems,
                customer = customer,
                onStatusChange = { newSt ->
                  scope.launch { repository.saveProject(selectedProject.copy(status = newSt)) }
                },
                onAddItem = { newItem ->
                  scope.launch { repository.saveProjectItem(newItem) }
                },
                onOpenDesignerForItem = { item ->
                  // Load the saved design when it exists, otherwise derive it from the item.
                  scope.launch {
                    val saved = item.designId?.let { repository.getDesign(it) }
                    activeDesign = saved ?: WindowDesign(
                      id = "des_${item.id}",
                      name = item.description,
                      productType = item.productType,
                      materialFamily = item.materialFamily,
                      widthMm = item.widthMm,
                      heightMm = item.heightMm,
                      panels = listOf(
                        PanelConfig("p1", OpeningType.CASEMENT_RIGHT),
                        PanelConfig("p2", OpeningType.FIXED)
                      )
                    )
                    navigate(Screen.DESIGNER)
                  }
                },
                onOpenCuttingPlan = { navigate(Screen.CUTTING_PLAN) },
                onOpenQuotation = { navigate(Screen.QUOTATION) },
                onOpenPaymentDialog = { navigate(Screen.FINANCE) },
                onBack = { currentScreen = Screen.PROJECTS }
              )
            } else {
              LaunchedEffect(Unit) { currentScreen = Screen.PROJECTS }
            }
          }

          Screen.DESIGNER -> {
            WindowDesignerScreen(
              initialDesign = activeDesign,
              defaultMarkupPercent = settings.defaultMarkupPercent,
              exchangeRateUsdToAfn = settings.exchangeRateUsdToAfn,
              canSave = can.canDesign,
              onSaveDesign = { d ->
                activeDesign = d
                scope.launch { repository.saveDesign(d) }
              },
              onNavigateToCutting = { d ->
                activeDesign = d
                currentScreen = Screen.CUTTING_PLAN
              },
              onNavigateToQuotation = { d ->
                activeDesign = d
                if (can.canCreateInvoices) currentScreen = Screen.QUOTATION
                else accessDeniedMessage = Screen.QUOTATION.titlePersian
              },
              onNavigateToManufacturingInstructions = { d ->
                activeDesign = d
                currentScreen = Screen.MANUFACTURING_INSTRUCTIONS
              },
              onBack = { currentScreen = Screen.DASHBOARD }
            )
          }

          Screen.PROJECT_CALCULATOR -> {
            val userProfileSystems = remember(profiles, materialProducts) {
              val fromCatalog = profiles.map { it.brandSystem.trim() }
              val fromMaterials = materialProducts
                .filter { it.category.contains("پروفیل") || it.materialFamily in listOf("UPVC", "ALUMINUM") }
                .map { if (it.brandModel.isNotBlank()) it.brandModel.trim() else it.name.trim() }
              (fromCatalog + fromMaterials).filter { it.isNotBlank() }.distinct()
            }
            ProjectCalculatorScreen(
              availableProfiles = userProfileSystems,
              defaultExchangeRate = settings.exchangeRateUsdToAfn,
              defaultMarkupPercent = settings.defaultMarkupPercent,
              defaultCurrency = settings.baseCurrency,
              onNavigateToDesigner = { design ->
                activeDesign = design
                navigate(Screen.DESIGNER)
              },
              onSaveSession = { session ->
                scope.launch { repository.saveCalculationSession(session) }
              },
              onBack = { currentScreen = Screen.DASHBOARD }
            )
          }

          Screen.MANUFACTURING_INSTRUCTIONS -> {
            ManufacturingInstructionsScreen(
              design = activeDesign,
              onBack = { currentScreen = Screen.DESIGNER }
            )
          }

          Screen.MATERIALS_CATALOG -> {
            MaterialsCatalogScreen(
              products = materialProducts,
              onSaveProduct = { p -> scope.launch { repository.saveMaterialProduct(p) } },
              onDeleteProduct = { id -> scope.launch { repository.deleteMaterialProduct(id) } },
              onBack = { currentScreen = Screen.DASHBOARD }
            )
          }

          Screen.CUTTING_PLAN -> {
            CuttingPlanScreen(
              design = activeDesign,
              sawKerfMm = settings.sawKerfMm,
              endTrimMm = settings.endTrimMm,
              onBack = { currentScreen = Screen.DESIGNER }
            )
          }

          Screen.QUOTATION -> {
            QuotationScreen(
              design = activeDesign,
              projectId = selectedProjectId ?: "",
              exchangeRateUsdToAfn = settings.exchangeRateUsdToAfn,
              paymentTerms = settings.quotationPaymentTerms,
              onSaveInvoice = { inv ->
                scope.launch { repository.saveInvoice(inv) }
              },
              onBack = { currentScreen = Screen.DESIGNER }
            )
          }

          Screen.FINANCE -> {
            FinanceScreen(
              invoices = allInvoices,
              payments = allPayments,
              expenses = allExpenses,
              projects = projects,
              customers = customers,
              baseCurrency = settings.baseCurrency,
              exchangeRateUsdToAfn = settings.exchangeRateUsdToAfn,
              canRecordPayments = can.canRecordPayments,
              onRecordPayment = { p -> scope.launch { repository.savePayment(p) } },
              onRecordExpense = { e -> scope.launch { repository.saveExpense(e) } }
            )
          }

          Screen.CUSTOMERS -> {
            CustomerListScreen(
              customers = customers,
              canEdit = currentRole != UserRole.READ_ONLY,
              onSaveCustomer = { c ->
                scope.launch { repository.createCustomerWithNextCode(c) }
              },
              onSelectCustomer = { c ->
                val customerProjects = projects.filter { it.customerId == c.id }
                if (customerProjects.isNotEmpty()) {
                  selectedProjectId = customerProjects.first().id
                  currentScreen = Screen.PROJECT_DETAIL
                }
              }
            )
          }

          Screen.CATALOG -> {
            CatalogScreen(
              profiles = profiles,
              glasses = glasses,
              accessories = accessories,
              canEdit = can.canEditCatalog,
              onAddProfile = { p -> scope.launch { repository.saveProfile(p) } },
              onAddGlass = { g -> scope.launch { repository.saveGlass(g) } },
              onAddAccessory = { a -> scope.launch { repository.saveAccessory(a) } }
            )
          }

          Screen.INVENTORY -> {
            InventoryScreen(
              movements = inventoryMovements,
              products = materialProducts,
              projects = projects,
              canRecord = can.canManageInventory,
              onRecordMovement = { m -> scope.launch { repository.recordInventoryMovement(m) } }
            )
          }

          Screen.USERS -> {
            UsersScreen(
              users = users,
              auditLogs = auditLogs,
              currentUserId = sessionUserId,
              onSaveUser = { user, password -> repository.saveUser(user, password) },
              onDeleteUser = { id -> repository.deleteUser(id) },
              onBack = { currentScreen = Screen.MORE }
            )
          }

          Screen.MORE -> {
            SettingsScreen(
              settings = settings,
              currentRole = currentRole,
              onSaveSettings = { newSettings ->
                val error = repository.saveSettings(newSettings)
                error
              },
              onExportBackup = { repository.exportBackupJson() },
              onRestoreBackup = { json -> repository.restoreBackupJson(json) },
              canBackup = can.canBackup,
              onChangePassword = { showChangePasswordDialog = true },
              onLogout = {
                scope.launch { repository.logout() }
                currentScreen = Screen.DASHBOARD
              },
              onNavigateToUsers = { navigate(Screen.USERS) },
              onNavigateToMaterialsCatalog = { navigate(Screen.MATERIALS_CATALOG) },
              onNavigateToProjectCalculator = { navigate(Screen.PROJECT_CALCULATOR) }
            )
          }
        }
      }
    }
  }
}

/** Single source of truth for role-based screen access (unit tested). */
fun isScreenAllowed(target: Screen, permissions: ScreenPermissions): Boolean = when (target) {
  Screen.USERS -> permissions.canManageUsers
  Screen.MATERIALS_CATALOG, Screen.CATALOG -> permissions.canEditCatalog
  Screen.INVENTORY -> permissions.canManageInventory
  Screen.PROJECT_CALCULATOR, Screen.QUOTATION -> permissions.canCreateInvoices
  Screen.FINANCE -> true // viewing is allowed; writing is gated per-action
  Screen.DESIGNER -> permissions.canDesign
  else -> true
}

data class ScreenPermissions(
  val canManageProjects: Boolean,
  val canDesign: Boolean,
  val canCreateInvoices: Boolean,
  val canRecordPayments: Boolean,
  val canEditCatalog: Boolean,
  val canManageInventory: Boolean,
  val canManageUsers: Boolean,
  val canBackup: Boolean
)
