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

  // State flows from Room
  val projects by repository.getAllProjects().collectAsState(initial = emptyList())
  val customers by repository.getAllCustomers().collectAsState(initial = emptyList())
  val profiles by repository.getAllProfiles().collectAsState(initial = emptyList())
  val glasses by repository.getAllGlasses().collectAsState(initial = emptyList())
  val accessories by repository.getAllAccessories().collectAsState(initial = emptyList())
  val materialProducts by repository.getAllMaterialProducts().collectAsState(initial = emptyList())

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
  val projectItems by if (selectedProjectId != null) {
    repository.getProjectItems(selectedProjectId!!).collectAsState(initial = emptyList())
  } else remember { mutableStateOf(emptyList()) }

  val projectInvoices by if (selectedProjectId != null) {
    repository.getInvoicesByProject(selectedProjectId!!).collectAsState(initial = emptyList())
  } else remember { mutableStateOf(emptyList()) }

  val projectPayments by if (selectedProjectId != null) {
    repository.getPaymentsByProject(selectedProjectId!!).collectAsState(initial = emptyList())
  } else remember { mutableStateOf(emptyList()) }

  // BackHandler to handle system and navigation back gesture
  BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
    currentScreen = when (currentScreen) {
      Screen.PROJECT_DETAIL -> Screen.PROJECTS
      Screen.CUTTING_PLAN, Screen.QUOTATION, Screen.MANUFACTURING_INSTRUCTIONS -> Screen.DESIGNER
      Screen.USERS, Screen.CATALOG, Screen.INVENTORY, Screen.MATERIALS_CATALOG -> Screen.MORE
      Screen.PROJECT_CALCULATOR -> Screen.DASHBOARD
      else -> Screen.DASHBOARD
    }
  }

  // Force Right-to-Left (RTL) Layout for Persian UI
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      topBar = {
        if (currentScreen in listOf(Screen.DASHBOARD, Screen.PROJECTS, Screen.FINANCE, Screen.CUSTOMERS, Screen.MORE)) {
          TopAppBar(
            title = {
              Text(
                text = currentScreen.titlePersian,
                style = MaterialTheme.typography.titleMedium
              )
            },
            actions = {
              IconButton(onClick = { currentScreen = Screen.PROJECT_CALCULATOR }) {
                Icon(Icons.Default.Calculate, contentDescription = "محاسبه پروژه")
              }
              IconButton(onClick = { currentScreen = Screen.MATERIALS_CATALOG }) {
                Icon(Icons.Default.Category, contentDescription = "اجناس و مواد")
              }
              if (currentScreen == Screen.MORE) {
                IconButton(onClick = { currentScreen = Screen.CATALOG }) {
                  Icon(Icons.Default.MenuBook, contentDescription = "کاتالوگ")
                }
                IconButton(onClick = { currentScreen = Screen.INVENTORY }) {
                  Icon(Icons.Default.Inventory2, contentDescription = "انبار")
                }
              }
            }
          )
        }
      },
      bottomBar = {
        NavigationBar {
          val navItems = listOf(
            Screen.DASHBOARD,
            Screen.PROJECTS,
            Screen.DESIGNER,
            Screen.PROJECT_CALCULATOR,
            Screen.FINANCE,
            Screen.MORE
          )
          navItems.forEach { screen ->
            val isSelected = currentScreen == screen ||
                (screen == Screen.PROJECTS && currentScreen == Screen.PROJECT_DETAIL) ||
                (screen == Screen.DESIGNER && (currentScreen == Screen.CUTTING_PLAN || currentScreen == Screen.QUOTATION || currentScreen == Screen.MANUFACTURING_INSTRUCTIONS)) ||
                (screen == Screen.MORE && (currentScreen in listOf(Screen.USERS, Screen.CATALOG, Screen.INVENTORY, Screen.MATERIALS_CATALOG)))

            NavigationBarItem(
              selected = isSelected,
              onClick = { currentScreen = screen },
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
              onNavigateToProjects = { currentScreen = Screen.PROJECTS },
              onNavigateToCustomers = { currentScreen = Screen.CUSTOMERS },
              onNavigateToDesigner = { currentScreen = Screen.DESIGNER },
              onNavigateToCutting = { currentScreen = Screen.CUTTING_PLAN },
              onNavigateToFinance = { currentScreen = Screen.FINANCE },
              onNavigateToProjectCalculator = { currentScreen = Screen.PROJECT_CALCULATOR },
              onNavigateToMaterialsCatalog = { currentScreen = Screen.MATERIALS_CATALOG },
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
                  activeDesign = WindowDesign(
                    id = "des_${item.id}",
                    name = item.description,
                    productType = item.productType,
                    materialFamily = item.materialFamily,
                    widthMm = item.widthMm,
                    heightMm = item.heightMm
                  )
                  currentScreen = Screen.DESIGNER
                },
                onOpenCuttingPlan = { currentScreen = Screen.CUTTING_PLAN },
                onOpenQuotation = { currentScreen = Screen.QUOTATION },
                onOpenPaymentDialog = { currentScreen = Screen.FINANCE },
                onBack = { currentScreen = Screen.PROJECTS }
              )
            } else {
              LaunchedEffect(Unit) { currentScreen = Screen.PROJECTS }
            }
          }

          Screen.DESIGNER -> {
            WindowDesignerScreen(
              initialDesign = activeDesign,
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
                currentScreen = Screen.QUOTATION
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
              onNavigateToDesigner = { design ->
                activeDesign = design
                currentScreen = Screen.DESIGNER
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
              onBack = { currentScreen = Screen.DESIGNER }
            )
          }

          Screen.QUOTATION -> {
            QuotationScreen(
              design = activeDesign,
              projectId = selectedProjectId ?: "",
              onSaveInvoice = { inv ->
                scope.launch { repository.saveInvoice(inv) }
              },
              onBack = { currentScreen = Screen.DESIGNER }
            )
          }

          Screen.FINANCE -> {
            FinanceScreen(
              invoices = projectInvoices,
              payments = projectPayments,
              expenses = emptyList(),
              projects = projects,
              customers = customers,
              onRecordPayment = { p ->
                scope.launch { repository.savePayment(p) }
              },
              onRecordExpense = { _ -> }
            )
          }

          Screen.CUSTOMERS -> {
            CustomerListScreen(
              customers = customers,
              onSaveCustomer = { c ->
                scope.launch { repository.saveCustomer(c) }
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
              onAddProfile = { _ -> scope.launch { repository.getAllProfiles() } },
              onAddGlass = { _ -> },
              onAddAccessory = { _ -> }
            )
          }

          Screen.INVENTORY -> {
            InventoryScreen(
              movements = emptyList(),
              onRecordMovement = { _ -> }
            )
          }

          Screen.USERS -> {
            UsersScreen(
              users = emptyList(),
              auditLogs = emptyList(),
              onBack = { currentScreen = Screen.MORE }
            )
          }

          Screen.MORE -> {
            SettingsScreen(
              workshopName = "کارگاه تخصصی در و پنجره دوجداره",
              baseCurrency = "AFN",
              exchangeRateUsdToAfn = "72.50",
              defaultMarkup = "25.00",
              sawKerfMm = "4",
              onSaveSettings = { _, _, _, _, _ -> },
              onExportBackup = {
                """
                {
                  "app": "WindowWorkshop",
                  "version": "1.0.0",
                  "projectsCount": ${projects.size},
                  "customersCount": ${customers.size},
                  "timestamp": ${System.currentTimeMillis()}
                }
                """.trimIndent()
              },
              onNavigateToUsers = { currentScreen = Screen.USERS },
              onNavigateToMaterialsCatalog = { currentScreen = Screen.MATERIALS_CATALOG },
              onNavigateToProjectCalculator = { currentScreen = Screen.PROJECT_CALCULATOR }
            )
          }
        }
      }
    }
  }
}
