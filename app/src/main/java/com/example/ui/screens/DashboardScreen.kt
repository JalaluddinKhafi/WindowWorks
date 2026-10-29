package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.core.CurrencyHelper
import com.example.domain.model.ProjectModel
import com.example.domain.model.ProjectStatus
import com.example.ui.theme.*
import java.math.BigDecimal

@Composable
fun DashboardScreen(
  projects: List<ProjectModel>,
  onNavigateToProjects: () -> Unit,
  onNavigateToCustomers: () -> Unit,
  onNavigateToDesigner: () -> Unit,
  onNavigateToCutting: () -> Unit,
  onNavigateToFinance: () -> Unit,
  onNavigateToProjectCalculator: () -> Unit,
  onNavigateToMaterialsCatalog: () -> Unit,
  onSelectProject: (String) -> Unit
) {
  val totalContract = projects.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalContractAmount) }
  val totalPaid = projects.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalPaidAmount) }
  val totalBalance = projects.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.balanceDue) }
  val activeProjectsCount = projects.count { it.status != ProjectStatus.COMPLETED && it.status != ProjectStatus.CANCELLED }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    // Welcome Header
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "کارگاه تخصصی در و پنجره دوجداره",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "سیستم جامع محاسبات مهندسی، طراحی پارامتریک و حسابداری صنعتی",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Financial & Operational Metrics (4 summary cards)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "پروژه‌های فعال",
            value = CurrencyHelper.toPersianDigits(activeProjectsCount),
            subtitle = "از مجموع ${CurrencyHelper.toPersianDigits(projects.size)} پروژه",
            icon = Icons.Default.WorkOutline,
            containerColor = Color(0xFFEFF6FF),
            contentColor = Color(0xFF1E3A8A),
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "کل قراردادها",
            value = CurrencyHelper.formatMoney(totalContract, includeSymbol = false),
            subtitle = "افغانی (AFN)",
            icon = Icons.Default.ReceiptLong,
            containerColor = Color(0xFFF0FDF4),
            contentColor = Color(0xFF059669),
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "کل دریافتی‌ها",
            value = CurrencyHelper.formatMoney(totalPaid, includeSymbol = false),
            subtitle = "وصول شده",
            icon = Icons.Default.Payments,
            containerColor = Color(0xFFECFDF5),
            contentColor = Color(0xFF047857),
            modifier = Modifier.weight(1f)
          )
          StatCard(
            title = "مانده طلب کارگاه",
            value = CurrencyHelper.formatMoney(totalBalance, includeSymbol = false),
            subtitle = "در انتظار تسویه",
            icon = Icons.Default.AccountBalanceWallet,
            containerColor = Color(0xFFFEF3C7),
            contentColor = Color(0xFFB45309),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Quick Actions Bar
    item {
      Text(
        text = "دسترسی سریع به عملیات کارگاهی",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickActionButton(
            title = "طراحی پنجره",
            icon = Icons.Default.CropLandscape,
            color = Color(0xFF2563EB),
            onClick = onNavigateToDesigner,
            modifier = Modifier.weight(1f)
          )
          QuickActionButton(
            title = "محاسبه پروژه",
            icon = Icons.Default.Calculate,
            color = Color(0xFF059669),
            onClick = onNavigateToProjectCalculator,
            modifier = Modifier.weight(1f)
          )
          QuickActionButton(
            title = "بهینه‌سازی برش",
            icon = Icons.Default.ContentCut,
            color = Color(0xFFD97706),
            onClick = onNavigateToCutting,
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickActionButton(
            title = "اجناس و مواد",
            icon = Icons.Default.Category,
            color = Color(0xFF0284C7),
            onClick = onNavigateToMaterialsCatalog,
            modifier = Modifier.weight(1f)
          )
          QuickActionButton(
            title = "پروژه‌ها",
            icon = Icons.Default.FolderOpen,
            color = Color(0xFF4F46E5),
            onClick = onNavigateToProjects,
            modifier = Modifier.weight(1f)
          )
          QuickActionButton(
            title = "حسابداری",
            icon = Icons.Default.AttachMoney,
            color = Color(0xFF7C3AED),
            onClick = onNavigateToFinance,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Recent Projects Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "پروژه‌های اخیر کارگاه",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onNavigateToProjects) {
          Text("مشاهده همه (${CurrencyHelper.toPersianDigits(projects.size)})")
        }
      }
    }

    if (projects.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "هنوز پروژه‌ای ثبت نشده است. از دکمه پروژه‌ها یا طراحی جدید شروع کنید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    } else {
      items(projects.take(5)) { project ->
        ProjectCard(project = project, onClick = { onSelectProject(project.id) })
      }
    }
  }
}

@Composable
private fun StatCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.85f))
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = contentColor
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
private fun QuickActionButton(
  title: String,
  icon: ImageVector,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    modifier = modifier.clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1
      )
    }
  }
}

@Composable
fun ProjectCard(
  project: ProjectModel,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = project.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "شماره: ${project.projectNumber} | آدرس: ${project.siteAddress.ifBlank { "ثبت نشده" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        StatusBadge(status = project.status)
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "مبلغ کل: ${CurrencyHelper.formatMoney(project.totalContractAmount, project.contractCurrency)}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "مانده: ${CurrencyHelper.formatMoney(project.balanceDue, project.contractCurrency)}",
          style = MaterialTheme.typography.labelMedium,
          color = if (project.balanceDue > BigDecimal.ZERO) Color(0xFFDC2626) else Color(0xFF059669)
        )
      }
    }
  }
}

@Composable
fun StatusBadge(status: ProjectStatus) {
  val (bgColor, textColor) = when (status) {
    ProjectStatus.DRAFT -> Color(0xFFF1F5F9) to Color(0xFF475569)
    ProjectStatus.MEASUREMENT -> Color(0xFFEFF6FF) to Color(0xFF2563EB)
    ProjectStatus.DESIGN -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
    ProjectStatus.QUOTATION -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    ProjectStatus.APPROVED -> Color(0xFFECFDF5) to Color(0xFF047857)
    ProjectStatus.PRODUCTION -> Color(0xFFFFF7ED) to Color(0xFFC2410C)
    ProjectStatus.INSTALLATION -> Color(0xFFF0FDF4) to Color(0xFF15803D)
    ProjectStatus.COMPLETED -> Color(0xFFECFDF5) to Color(0xFF065F46)
    ProjectStatus.CANCELLED -> Color(0xFFFEF2F2) to Color(0xFFB91C1C)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = bgColor
  ) {
    Text(
      text = status.titlePersian,
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}
