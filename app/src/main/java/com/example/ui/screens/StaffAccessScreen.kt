package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuthUiState
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary

private enum class AccessMode { LOGIN, REGISTER }

@Composable
fun StaffAccessScreen(
    state: AuthUiState,
    onCreateAdmin: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit
) {
    var mode by remember { mutableStateOf(AccessMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = NaomiOrange)
            return@Column
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (state.needsAdminSetup) Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                    contentDescription = null,
                    tint = NaomiOrange
                )
                Text(
                    text = if (state.needsAdminSetup) "Create Naomi Admin" else "Staff Access",
                    color = NaomiTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (state.needsAdminSetup) {
                        "First-time setup. Naomi will be the permanent local administrator for this PDA."
                    } else if (mode == AccessMode.LOGIN) {
                        "Sign in before using receipts, printing, shifts and staff tools."
                    } else {
                        "Register a staff account. New registrations are Staff accounts; only Naomi is Admin."
                    },
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )

                if (state.needsAdminSetup) {
                    OutlinedTextField(
                        value = "Naomi",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Administrator") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    CredentialFields(
                        credential = credential,
                        confirmation = confirmation,
                        onCredentialChange = { credential = it },
                        onConfirmationChange = { confirmation = it }
                    )
                    Button(
                        onClick = { onCreateAdmin(credential, confirmation) },
                        enabled = credential.length >= 4 && confirmation.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.padding(3.dp))
                        Text("Create Naomi Admin", fontWeight = FontWeight.Bold)
                    }
                } else if (mode == AccessMode.LOGIN) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = credential,
                        onValueChange = { credential = it },
                        label = { Text("PIN / password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onLogin(username, credential) },
                        enabled = username.isNotBlank() && credential.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            mode = AccessMode.REGISTER
                            credential = ""
                            confirmation = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Account", color = NaomiOrange)
                    }
                } else {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        supportingText = { Text("'Naomi' and 'admin' are reserved.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Staff display name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CredentialFields(
                        credential = credential,
                        confirmation = confirmation,
                        onCredentialChange = { credential = it },
                        onConfirmationChange = { confirmation = it }
                    )
                    Button(
                        onClick = { onRegister(username, displayName, credential, confirmation) },
                        enabled = username.length >= 3 && displayName.length >= 2 && credential.length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Staff Account", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            mode = AccessMode.LOGIN
                            credential = ""
                            confirmation = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Sign In", color = NaomiOrange)
                    }
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        color = NaomiOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialFields(
    credential: String,
    confirmation: String,
    onCredentialChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit
) {
    OutlinedTextField(
        value = credential,
        onValueChange = onCredentialChange,
        label = { Text("PIN / password") },
        supportingText = { Text("Minimum 4 characters. Stored as a salted hash.") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = confirmation,
        onValueChange = onConfirmationChange,
        label = { Text("Confirm PIN / password") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
