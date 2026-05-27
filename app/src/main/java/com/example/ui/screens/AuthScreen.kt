package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.RefChainRepository
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    repository: RefChainRepository,
    onAuthSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLogin by remember { mutableStateOf(true) }

    // Fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralInput by remember { mutableStateOf("") }
    var termsAgree by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Brand Title
            Spacer(modifier = Modifier.height(20.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RefChain",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "FINANCIAL TRUST NETWORK",
                color = GoldSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Security Compliance Card containing Tabs & Forms
            GlassCard {
                // Regulatory disclaimer banner at top of form
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CoralDanger.copy(alpha = 0.08f))
                        .border(1.dp, CoralDanger.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Regulatory Alert",
                            tint = CoralDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Legal Disclaimer & Regulatory Risk Notice:",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "This platform does not guarantee profits. Referral structures involve structural risk. Users participate at their own explicit financial risk.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLogin) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (isLogin) 1.dp else 0.dp,
                                color = if (isLogin) EmeraldPrimary.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                isLogin = true
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SIGN IN",
                            color = if (isLogin) EmeraldPrimary else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isLogin) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (!isLogin) 1.dp else 0.dp,
                                color = if (!isLogin) EmeraldPrimary.copy(alpha = 0.3f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                isLogin = false
                                errorMessage = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CREATE WALLET",
                            color = if (!isLogin) EmeraldPrimary else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inputs
                if (!isLogin) {
                    // Sign up specific fields
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Legal Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("email_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (!isLogin) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (with Country Code)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary) },
                        placeholder = { Text("+977-98...") },
                        modifier = Modifier.fillMaxWidth().testTag("phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Contact Link") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldPrimary) },
                        placeholder = { Text("https://wa.me/...") },
                        modifier = Modifier.fillMaxWidth().testTag("whatsapp_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = referralInput,
                        onValueChange = { referralInput = it },
                        label = { Text("Invitation Link or Referral Code (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldSecondary) },
                        placeholder = { Text("e.g. SUJAN40") },
                        modifier = Modifier.fillMaxWidth().testTag("referral_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Secure Pin / Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showPassword = !showPassword }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (showPassword) "HIDE" else "SHOW",
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (!isLogin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { termsAgree = !termsAgree }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAgree,
                            onCheckedChange = { termsAgree = it },
                            modifier = Modifier.testTag("terms_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agree to high-risk terms, privacy laws and declare no secondary self-referrals will be staged.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                }

                // Messages UI
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = CoralDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (infoMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = infoMessage ?: "",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (loading) return@Button
                        errorMessage = null
                        infoMessage = null

                        if (isLogin) {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both Email and Password fields."
                                return@Button
                            }
                            loading = true
                            coroutineScope.launch {
                                val result = repository.authenticateUser(email, password)
                                loading = false
                                result.onSuccess {
                                    onAuthSuccess(it)
                                }.onFailure {
                                    errorMessage = it.message ?: "Authentication failed."
                                }
                            }
                        } else {
                            if (name.isBlank() || email.isBlank() || phone.isBlank() || whatsapp.isBlank() || password.isBlank()) {
                                errorMessage = "Please completely fill in all signup details."
                                return@Button
                            }
                            if (!termsAgree) {
                                errorMessage = "You must review and explicitly agree to the Terms & Conditions."
                                return@Button
                            }
                            loading = true
                            coroutineScope.launch {
                                val referral = if (referralInput.isNotBlank()) referralInput else null
                                val result = repository.registerUser(
                                    name = name,
                                    email = email,
                                    phone = phone,
                                    whatsapp = whatsapp,
                                    passwordPlain = password,
                                    referredByCode = referral
                                )
                                loading = false
                                result.onSuccess {
                                    infoMessage = "Account created! Logging in..."
                                    onAuthSuccess(it)
                                }.onFailure {
                                    errorMessage = it.message ?: "Registration failed."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isLogin) "SECURE LOG IN" else "PROCEED & CREATE WALLET",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Demo Credentials Guidance Panel
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Demo Admin Verification Mode",
                    color = GoldSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To test as System CEO, sign in with:\n• Email: refchain.earn@gmail.com\n• Password: admin123\n\nTo test Sujan (First Referrer):\n• Email: sujan@gmail.com | Password: user123",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
