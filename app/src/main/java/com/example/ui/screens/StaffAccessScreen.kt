package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Key
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

private enum class AccessMode { LOGIN, REGISTER, RECOVER }

@Composable
fun StaffAccessScreen(
    state: AuthUiState,
    onCreateAdmin: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onRecoverAdmin: (String, String, String) -> Unit
) {
    var mode by remember { mutableStateOf(AccessMode.LOGIN) }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NaomiDarkBg)
            .verticalScroll(rememberScrollState())
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
                    imageVector = if (state.needsAdminSetup) Icons.Default.AdminPanelSettings else if (mode == AccessMode.RECOVER) Icons.Default.Key else Icons.Default.Badge,
                    contentDescription = null,
                    tint = NaomiOrange
                )
                Text(
                    text = when {
                        state.needsAdminSetup -> "Create Naomi Admin"
                        mode == AccessMode.RECOVER -> "Recover Naomi Admin"
                        else -> "Staff Access"
                    },
                    color = NaomiTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = when {
                        state.needsAdminSetup -> "First-time setup. Naomi will be the permanent local administrator for this PDA."
                        mode == AccessMode.LOGIN -> "Sign in before using receipts, printing, shifts and staff tools."
                        mode == AccessMode.REGISTER -> "Register a Staff account. Naomi must approve it before it can sign in."
                        else -> "Use Naomi's one-time recovery code to replace the administrator PIN/password. The recovery code rotates after use."
                    },
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )

                when {
                    state.needsAdminSetup -> {
                        OutlinedTextField(
                            value = "Naomi",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Administrator") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        CredentialFields(credential, confirmation, { credential = it }, { confirmation = it })
                        Button(
                            onClick = { onCreateAdmin(credential, confirmation) },
                            enabled = credential.length >= 6 && confirmation.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text("Create Naomi Admin", fontWeight = FontWeight.Bold)
                        }
                    }
                    mode == AccessMode.LOGIN -> {
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
                        ) { Text("Sign In", fontWeight = FontWeight.Bold) }
                        TextButton(onClick = { mode = AccessMode.REGISTER; credential = ""; confirmation = "" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Register Account", color = NaomiOrange)
                        }
                        TextButton(onClick = { mode = AccessMode.RECOVER; credential = ""; confirmation = "" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Recover Naomi Admin", color = NaomiTextSecondary)
                        }
                    }
                    mode == AccessMode.REGISTER -> {
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
                        CredentialFields(credential, confirmation, { credential = it }, { confirmation = it })
                        Button(
                            onClick = { onRegister(username, displayName, credential, confirmation) },
                            enabled = username.length >= 3 && displayName.length >= 2 && credential.length >= 6,
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Register Staff Account", fontWeight = FontWeight.Bold) }
                        BackToLogin { mode = AccessMode.LOGIN; credential = ""; confirmation = "" }
                    }
                    else -> {
                        OutlinedTextField(
                            value = recoveryCode,
                            onValueChange = { recoveryCode = it.uppercase() },
                            label = { Text("Naomi recovery code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        CredentialFields(credential, confirmation, { credential = it }, { confirmation = it })
                        Button(
                            onClick = { onRecoverAdmin(recoveryCode, credential, confirmation) },
                            enabled = recoveryCode.length >= 12 && credential.length >= 6 && confirmation.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Recover & Set New Credential", fontWeight = FontWeight.Bold) }
                        BackToLogin { mode = AccessMode.LOGIN; credential = ""; confirmation = ""; recoveryCode = "" }
                    }
                }

                state.message?.let { message ->
                    Text(message, color = NaomiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecoveryCodeNoticeScreen(code: String, onAcknowledge: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(NaomiDarkBg).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NaomiOrange),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = NaomiOrange)
                Text("Save Naomi's Recovery Code", color = NaomiTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "This code is shown only now and is stored only as a one-way hash. Keep it somewhere safe away from the PDA. Using it rotates the code.",
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )
                Text(code, color = NaomiOrange, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Button(onClick = onAcknowledge, colors = ButtonDefaults.buttonColors(containerColor = NaomiRed), modifier = Modifier.fillMaxWidth()) {
                    Text("I Have Saved This Code", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ChangeCredentialScreen(displayName: String, onChange: (String, String) -> Unit) {
    var credential by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().background(NaomiDarkBg).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = NaomiSurface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NaomiOrange)
                Text("Create Your New Credential", color = NaomiTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("$displayName signed in with a temporary credential. It must be replaced before the PDA can be used.", color = NaomiTextSecondary, fontSize = 12.sp)
                CredentialFields(credential, confirmation, { credential = it }, { confirmation = it })
                Button(
                    onClick = { onChange(credential, confirmation) },
                    enabled = credential.length >= 6 && confirmation.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save New PIN / Password", fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun BackToLogin(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Back to Sign In", color = NaomiOrange) }
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
        supportingText = { Text("Use a 6–12 digit PIN, or a password of at least 8 characters.") },
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
