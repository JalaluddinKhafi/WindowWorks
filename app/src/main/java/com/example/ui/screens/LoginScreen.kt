package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Local login screen. Authentication is fully offline against the `users` table
 * (PBKDF2 password hashes). No network / Firebase dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  workshopName: String,
  isFirstRun: Boolean,
  onLogin: suspend (username: String, password: String) -> String?,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var username by remember { mutableStateOf(if (isFirstRun) "admin" else "") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var isBusy by remember { mutableStateOf(false) }

  fun submit() {
    if (isBusy) return
    if (username.isBlank() || password.isBlank()) {
      error = "نام کاربری و رمز عبور را وارد کنید."
      return
    }
    isBusy = true
    error = null
    scope.launch {
      val result = onLogin(username.trim(), password)
      isBusy = false
      if (result != null) {
        error = result
        password = ""
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surface
          )
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      modifier = Modifier
        .widthIn(max = 420.dp)
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(44.dp)
        )
        Text(
          text = "WindowWorkshop",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = workshopName.ifBlank { "کارگاه تخصصی در و پنجره" },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        if (isFirstRun) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                "ورود اولیه راه‌اندازی",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                "نام کاربری: admin  |  رمز عبور موقت: admin123",
                style = MaterialTheme.typography.bodySmall
              )
              Text(
                "پس از اولین ورود، رمز عبور را از بخش تنظیمات تغییر دهید.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        OutlinedTextField(
          value = username,
          onValueChange = {
            username = it
            error = null
          },
          label = { Text("نام کاربری") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            error = null
          },
          label = { Text("رمز عبور") },
          leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (passwordVisible) "پنهان کردن رمز" else "نمایش رمز"
              )
            }
          },
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
          ),
          singleLine = true,
          isError = error != null,
          modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
          Text(
            text = error!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Button(
          onClick = { submit() },
          enabled = !isBusy,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          if (isBusy) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
          } else {
            Text("ورود به برنامه", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Password change dialog. When [forceChange] is true (first admin login) the dialog cannot be
 * dismissed and only allows changing the password or signing out.
 */
@Composable
fun ChangePasswordDialog(
  forceChange: Boolean,
  onChangePassword: suspend (oldPassword: String, newPassword: String) -> String?,
  onDismiss: () -> Unit,
  onLogout: (() -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  var oldPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }
  var isBusy by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!forceChange) onDismiss() },
    title = { Text(if (forceChange) "تغییر رمز عبور اجباری" else "تغییر رمز عبور") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (forceChange) {
          Text(
            "برای حفظ امنیت اطلاعات کارگاه، رمز عبور پیش‌فرض باید تغییر کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        OutlinedTextField(
          value = oldPassword,
          onValueChange = { oldPassword = it; error = null },
          label = { Text("رمز عبور فعلی") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = newPassword,
          onValueChange = { newPassword = it; error = null },
          label = { Text("رمز عبور جدید (حداقل ۶ کاراکتر)") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = confirmPassword,
          onValueChange = { confirmPassword = it; error = null },
          label = { Text("تکرار رمز عبور جدید") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          modifier = Modifier.fillMaxWidth()
        )
        error?.let {
          Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        enabled = !isBusy,
        onClick = {
          if (newPassword != confirmPassword) {
            error = "رمز جدید و تکرار آن یکسان نیست."
            return@Button
          }
          isBusy = true
          scope.launch {
            val result = onChangePassword(oldPassword, newPassword)
            isBusy = false
            if (result == null) {
              onDismiss()
            } else {
              error = result
            }
          }
        }
      ) { Text("ذخیره رمز جدید") }
    },
    dismissButton = {
      if (forceChange) {
        onLogout?.let { logout ->
          TextButton(onClick = logout) { Text("خروج") }
        }
      } else {
        TextButton(onClick = onDismiss) { Text("انصراف") }
      }
    }
  )
}
