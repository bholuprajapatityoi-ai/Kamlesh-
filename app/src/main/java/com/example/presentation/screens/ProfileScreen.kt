package com.example.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.AuthState
import com.example.domain.model.BackendDiagnostics
import com.example.presentation.components.BackendStatusBanner
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.viewmodel.AuthUiFeedback
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun ProfileScreen(
    authState: AuthState,
    feedback: AuthUiFeedback,
    diagnostics: BackendDiagnostics,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
    onRestoreSession: () -> Unit,
    onLogout: () -> Unit,
    onClearFeedback: () -> Unit,
    onRetryConnection: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Login, 1=Register, 2=Reset
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var resetToken by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Boss Account & Security",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                BackendStatusBanner(
                    diagnostics = diagnostics,
                    onActionClick = onRetryConnection,
                )

                if (!feedback.errorMessage.isNullOrBlank()) {
                    GoluGlassCard(accentBorder = GoluCoralRed) {
                        Text(
                            text = feedback.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = GoluCoralRed,
                        )
                    }
                }
                if (!feedback.successMessage.isNullOrBlank()) {
                    GoluGlassCard(accentBorder = GoluEmerald) {
                        Text(
                            text = feedback.successMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = GoluEmerald,
                        )
                    }
                }

                when (authState) {
                    is AuthState.Checking -> {
                        GoluGlassCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Restoring encrypted session with FastAPI...")
                            }
                        }
                    }

                    is AuthState.Authenticated -> {
                        val user = authState.user
                        GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.7f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AccountCircle,
                                    contentDescription = "User Profile",
                                    tint = GoluNeonCyan,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Boss ${user.fullName}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = GoluEmerald,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Tokens protected with Android Keystore AES-256-GCM.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onRestoreSession,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Verify Session")
                                }
                                Button(
                                    onClick = onLogout,
                                    modifier = Modifier.weight(1f).testTag("logout_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoluCoralRed),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Logout")
                                }
                            }
                        }
                    }

                    is AuthState.Unauthenticated -> {
                        GoluGlassCard {
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = {
                                        selectedTab = 0
                                        onClearFeedback()
                                    },
                                    text = { Text("Sign In") },
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = {
                                        selectedTab = 1
                                        onClearFeedback()
                                    },
                                    text = { Text("Register") },
                                )
                                Tab(
                                    selected = selectedTab == 2,
                                    onClick = {
                                        selectedTab = 2
                                        onClearFeedback()
                                    },
                                    text = { Text("Reset") },
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            if (selectedTab == 1) {
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                                )
                                Spacer(Modifier.height(10.dp))
                            }

                            if (selectedTab != 2) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                                )
                            }

                            if (selectedTab == 1) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("register_confirm_password_input"),
                                )
                            }

                            if (selectedTab == 2) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Account Email (to request reset)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onForgotPassword(email) },
                                    enabled = !feedback.isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_button"),
                                ) {
                                    Text("Request Password Reset")
                                }
                                Spacer(Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = resetToken,
                                    onValueChange = { resetToken = it },
                                    label = { Text("Reset Token") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { onResetPassword(resetToken, password, confirmPassword) },
                                    enabled = !feedback.isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("reset_password_submit_button"),
                                ) {
                                    Text("Confirm Password Reset")
                                }
                            } else {
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (selectedTab == 0) {
                                            onLogin(email, password)
                                        } else {
                                            onRegister(fullName, email, password, confirmPassword)
                                        }
                                    },
                                    enabled = !feedback.isLoading,
                                    modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    if (feedback.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (selectedTab == 0) "Sign In to GOLU" else "Create Boss Account")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
