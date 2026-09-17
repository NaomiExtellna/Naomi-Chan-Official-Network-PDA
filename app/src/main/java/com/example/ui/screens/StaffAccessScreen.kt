package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AuthUiState
import com.example.ui.theme.NaomiBorder
import com.example.ui.theme.NaomiDarkBg
import com.example.ui.theme.NaomiOrange
import com.example.ui.theme.NaomiRed
import com.example.ui.theme.NaomiSurface
import com.example.ui.theme.NaomiSurfaceVariant
import com.example.ui.theme.NaomiTextPrimary
import com.example.ui.theme.NaomiTextSecondary
import com.example.ui.theme.NaomiTextTertiary

private enum class AccessMode { LOGIN, REGISTER, RECOVER }

@Composable
fun StaffAccessScreen(
    state: AuthUiState,
    onCreateAdmin: (String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onRecoverAdmin: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("naomi_staff_access", android.content.Context.MODE_PRIVATE)
    }

    var mode by remember { mutableStateOf(AccessMode.LOGIN) }
    var username by remember {
        mutableStateOf(preferences.getString("last_username", "").orEmpty())
    }
    var displayName by remember { mutableStateOf("") }
    var credential by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var recoveryCode by remember { mutableStateOf("") }
    var usePinPad by remember { mutableStateOf(true) }

    fun submitLogin(loginCredential: String = credential) {
        if (username.isBlank() || loginCredential.isBlank() || state.isAuthenticating) return
        preferences.edit().putString("last_username", username.trim()).apply()
        onLogin(username, loginCredential)
    }

    val canSubmitLogin = username.isNotBlank() &&
        credential.isNotBlank() &&
        (!usePinPad || credential.length == 6) &&
        !state.isAuthenticating

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NaomiDarkBg,
                        Color(0xFFF8F9FA),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = NaomiSurface,
                border = BorderStroke(1.dp, NaomiBorder)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_naomi_logo),
                    contentDescription = "Naomi-Chan logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Naomi-Chan™ POS",
                color = NaomiTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Secure staff access · SUNMI V2",
                color = NaomiTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.isLoading) {
                CircularProgressIndicator(color = NaomiRed)
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = NaomiSurface),
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    AccessHeading(state = state, mode = mode, usePinPad = usePinPad)
                    HorizontalDivider(color = NaomiBorder)

                    when {
                        state.needsAdminSetup -> {
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
                            PrimaryActionButton(
                                text = "Create administrator",
                                enabled = credential.length >= 6 && confirmation.isNotBlank(),
                                loading = false,
                                icon = Icons.Default.Lock,
                                onClick = { onCreateAdmin(credential, confirmation) }
                            )
                        }

                        mode == AccessMode.LOGIN -> {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                supportingText = {
                                    if (preferences.getString("last_username", "").orEmpty().isNotBlank()) {
                                        Text("Last staff username remembered on this terminal.")
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (usePinPad) {
                                PinEntryDisplay(pinLength = credential.length)
                                PinKeypad(
                                    enabled = !state.isAuthenticating,
                                    onDigit = { digit ->
                                        if (credential.length < 6) {
                                            val updated = credential + digit
                                            credential = updated
                                            if (updated.length == 6 && username.isNotBlank()) {
                                                submitLogin(updated)
                                            }
                                        }
                                    },
                                    onClear = { credential = "" },
                                    onBackspace = {
                                        if (credential.isNotEmpty()) credential = credential.dropLast(1)
                                    }
                                )
                            } else {
                                OutlinedTextField(
                                    value = credential,
                                    onValueChange = { credential = it },
                                    label = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { if (canSubmitLogin) submitLogin() }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            PrimaryActionButton(
                                text = if (state.isAuthenticating) "Signing in…" else "Sign in",
                                enabled = canSubmitLogin,
                                loading = state.isAuthenticating,
                                icon = Icons.Default.Badge,
                                onClick = { submitLogin() }
                            )

                            TextButton(
                                onClick = {
                                    credential = ""
                                    usePinPad = !usePinPad
                                },
                                enabled = !state.isAuthenticating,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (usePinPad) "Use password instead" else "Use 6-digit PIN instead",
                                    color = NaomiOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        mode = AccessMode.REGISTER
                                        credential = ""
                                        confirmation = ""
                                    }
                                ) {
                                    Text("Register staff", color = NaomiOrange, fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = {
                                        mode = AccessMode.RECOVER
                                        credential = ""
                                        confirmation = ""
                                    }
                                ) {
                                    Text("Admin recovery", color = NaomiTextSecondary, fontSize = 11.sp)
                                }
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
                            CredentialFields(
                                credential = credential,
                                confirmation = confirmation,
                                onCredentialChange = { credential = it },
                                onConfirmationChange = { confirmation = it }
                            )
                            PrimaryActionButton(
                                text = "Submit registration",
                                enabled = username.length >= 3 && displayName.length >= 2 && credential.length >= 6,
                                loading = false,
                                icon = Icons.Default.Badge,
                                onClick = { onRegister(username, displayName, credential, confirmation) }
                            )
                            BackToLogin {
                                mode = AccessMode.LOGIN
                                credential = ""
                                confirmation = ""
                            }
                        }

                        else -> {
                            OutlinedTextField(
                                value = recoveryCode,
                                onValueChange = { recoveryCode = it.uppercase() },
                                label = { Text("Recovery code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            CredentialFields(
                                credential = credential,
                                confirmation = confirmation,
                                onCredentialChange = { credential = it },
                                onConfirmationChange = { confirmation = it }
                            )
                            PrimaryActionButton(
                                text = "Recover access",
                                enabled = recoveryCode.length >= 12 && credential.length >= 6 && confirmation.isNotBlank(),
                                loading = false,
                                icon = Icons.Default.Key,
                                onClick = { onRecoverAdmin(recoveryCode, credential, confirmation) }
                            )
                            BackToLogin {
                                mode = AccessMode.LOGIN
                                credential = ""
                                confirmation = ""
                                recoveryCode = ""
                            }
                        }
                    }

                    state.message?.let { message ->
                        Surface(
                            color = NaomiSurfaceVariant,
                            border = BorderStroke(1.dp, NaomiBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                color = NaomiTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Naomi-Chan Official Network · Local secure access",
                color = NaomiTextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccessHeading(state: AuthUiState, mode: AccessMode, usePinPad: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NaomiRed.copy(alpha = 0.08f)
        ) {
            Icon(
                imageVector = when {
                    state.needsAdminSetup -> Icons.Default.AdminPanelSettings
                    mode == AccessMode.RECOVER -> Icons.Default.Key
                    else -> Icons.Default.Security
                },
                contentDescription = null,
                tint = NaomiRed,
                modifier = Modifier.padding(9.dp).size(21.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    state.needsAdminSetup -> "Administrator setup"
                    mode == AccessMode.RECOVER -> "Recover administrator"
                    mode == AccessMode.REGISTER -> "Register staff account"
                    else -> "Staff sign in"
                },
                color = NaomiTextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    state.needsAdminSetup -> "Secure this terminal before first use."
                    mode == AccessMode.LOGIN -> if (usePinPad) "Enter your 6-digit staff PIN." else "Authenticate with your staff password."
                    mode == AccessMode.REGISTER -> "New accounts require administrator approval."
                    else -> "Use the one-time recovery code issued for Naomi."
                },
                color = NaomiTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PinEntryDisplay(pinLength: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "6-DIGIT PIN",
            color = NaomiTextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(6) { index ->
                val filled = index < pinLength
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = if (filled) NaomiRed.copy(alpha = 0.08f) else NaomiSurfaceVariant,
                    border = BorderStroke(1.dp, if (filled) NaomiRed.copy(alpha = 0.55f) else NaomiBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (filled) "●" else "",
                            color = NaomiRed,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    PinDigitButton(
                        label = digit,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClear,
                enabled = enabled,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            PinDigitButton(
                label = "0",
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onClick = { onDigit("0") }
            )
            OutlinedButton(
                onClick = onBackspace,
                enabled = enabled,
                border = BorderStroke(1.dp, NaomiBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("⌫", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = NaomiSurfaceVariant,
            contentColor = NaomiTextPrimary
        ),
        border = BorderStroke(1.dp, NaomiBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
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
            border = BorderStroke(1.dp, NaomiOrange),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = NaomiOrange)
                Text("Save the administrator recovery code", color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "This code is shown once and stored only as a one-way hash. Keep it somewhere secure and separate from the PDA.",
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )
                Surface(
                    color = NaomiSurfaceVariant,
                    border = BorderStroke(1.dp, NaomiBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(code, color = NaomiOrange, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(14.dp))
                }
                Button(
                    onClick = onAcknowledge,
                    colors = ButtonDefaults.buttonColors(containerColor = NaomiRed),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("I have saved this code", fontWeight = FontWeight.Bold)
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
        Card(
            colors = CardDefaults.cardColors(containerColor = NaomiSurface),
            border = BorderStroke(1.dp, NaomiBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = NaomiOrange)
                Text("Create a new credential", color = NaomiTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$displayName signed in with a temporary credential. Replace it before continuing.",
                    color = NaomiTextSecondary,
                    fontSize = 12.sp
                )
                CredentialFields(
                    credential = credential,
                    confirmation = confirmation,
                    onCredentialChange = { credential = it },
                    onConfirmationChange = { confirmation = it }
                )
                PrimaryActionButton(
                    text = "Save new credential",
                    enabled = credential.length >= 6 && confirmation.isNotBlank(),
                    loading = false,
                    icon = Icons.Default.Lock,
                    onClick = { onChange(credential, confirmation) }
                )
            }
        }
    }
}

@Composable
private fun BackToLogin(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Back to sign in", color = NaomiOrange)
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
        label = { Text("PIN or password") },
        supportingText = { Text("6–12 digit PIN, or password with at least 8 characters.") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = confirmation,
        onValueChange = onConfirmationChange,
        label = { Text("Confirm credential") },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )
}
