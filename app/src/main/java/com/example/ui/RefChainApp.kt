package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.RefChainRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.UserDashboard
import com.example.ui.screens.AdminDashboard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@Composable
fun RefChainApp(
    repository: RefChainRepository,
    modifier: Modifier = Modifier
) {
    var loggedInUser by remember { mutableStateOf<User?>(null) }
    var isDarkTheme by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Render either authentication or matching logged-in portal
            if (loggedInUser == null) {
                AuthScreen(
                    repository = repository,
                    onAuthSuccess = { user ->
                        loggedInUser = user
                    }
                )
            } else {
                val user = loggedInUser!!
                if (user.role == "ADMIN") {
                    AdminDashboard(
                        adminUser = user,
                        repository = repository,
                        onLogout = { loggedInUser = null }
                    )
                } else {
                    UserDashboard(
                        user = user,
                        repository = repository,
                        onLogout = { loggedInUser = null }
                    )
                }
            }

            // Top Quick Action floating bubble to toggle light/dark and switch demo roles
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(width = 1.dp, color = EmeraldPrimary.copy(alpha = 0.4f), shape = RoundedCornerShape(30.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Toggle
                IconButton(
                    onClick = { isDarkTheme = !isDarkTheme },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.Create else Icons.Default.Lock,
                        contentDescription = "Toggle Theme",
                        tint = GoldSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.height(16.dp).width(1.dp))

                // Role Simulation Swapper
                Row(
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val current = loggedInUser
                                if (current == null) {
                                    // Log in Sujan automatically
                                    val authResult = repository.authenticateUser("sujan@gmail.com", "user123")
                                    authResult.onSuccess { loggedInUser = it }
                                } else if (current.role == "USER") {
                                    // Log in Admin automatically
                                    val authResult = repository.authenticateUser("refchain.earn@gmail.com", "admin123")
                                    authResult.onSuccess { loggedInUser = it }
                                } else {
                                    // Switch back to Sujan
                                    val authResult = repository.authenticateUser("sujan@gmail.com", "user123")
                                    authResult.onSuccess { loggedInUser = it }
                                }
                            }
                        }
                        .testTag("role_exchange_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Simulate Swapping",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (loggedInUser == null) "DEMO USER" else if (loggedInUser!!.role == "USER") "SWAP TO ADMIN" else "SWAP TO USER",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
