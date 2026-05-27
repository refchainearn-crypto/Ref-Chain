package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.repository.RefChainRepository
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserDashboard(
    user: User,
    repository: RefChainRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe DB live updates! Real reactivity.
    val uList by repository.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val depList by repository.allDeposits.collectAsStateWithLifecycle(initialValue = emptyList())
    val withList by repository.allWithdrawals.collectAsStateWithLifecycle(initialValue = emptyList())
    val txList by repository.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val tktList by repository.allTickets.collectAsStateWithLifecycle(initialValue = emptyList())
    val systemConfigState by repository.systemConfig.collectAsStateWithLifecycle(initialValue = SystemConfig())

    val config = systemConfigState ?: SystemConfig()
    val currentUser = uList.find { it.userId == user.userId } ?: user

    // Filter list context
    val userDeposits = depList.filter { it.userId == currentUser.userId }
    val userWithdrawals = withList.filter { it.userId == currentUser.userId }
    val userTransactions = txList.filter { it.userId == currentUser.userId }
    val userTickets = tktList.filter { it.userId == currentUser.userId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RefChain Platform",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "User Code: ${currentUser.referralCode}",
                            fontSize = 12.sp,
                            color = GoldSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CoralDanger.copy(alpha = 0.15f))
                            .clickable { onLogout() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("LOGOUT", color = CoralDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Dashboard") },
                    label = { Text("Overview") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Finance") },
                    label = { Text("Finance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "My Network") },
                    label = { Text("Network") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Support") },
                    label = { Text("Support") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.12f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> UserOverviewTab(currentUser, userTransactions, repository)
                1 -> UserFinanceTab(currentUser, userDeposits, userWithdrawals, config, repository)
                2 -> UserNetworkTab(currentUser, uList, repository)
                3 -> UserSupportTab(currentUser, userTickets, config, repository)
            }
        }
    }
}

// =========================================================
// TAB 0: OVERVIEW & CANVA-BASED ACCENT CHARTS
// =========================================================
@Composable
fun UserOverviewTab(
    user: User,
    transactions: List<Transaction>,
    repository: RefChainRepository
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Legal Warning Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CoralDanger.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CoralDanger,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Legality disclaimer: We do NOT guarantee commissions or profits. Participations involve high structural risk factors.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Welcome Box
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Welcome,",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Text(
                            user.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Account Status: ACTIVE",
                            color = EmeraldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, tint = EmeraldPrimary, contentDescription = null)
                    }
                }
            }
        }

        // Balances Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "WALLET BALANCE",
                    value = "NPR ${String.format("%.2f", user.walletBalance)}",
                    subtitle = "Withdrawable resources",
                    trendUp = true,
                    icon = { Icon(Icons.Default.Home, tint = EmeraldPrimary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "COMMISSIONS EARNED",
                    value = "NPR ${String.format("%.2f", user.commissionsEarned)}",
                    subtitle = "Referral rewards",
                    trendUp = true,
                    icon = { Icon(Icons.Default.Info, tint = GoldSecondary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Chart Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Weekly Rewards Velocity (NPR Chart)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Historical distribution rate of multi-level networking",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Simulated chart points representing user deposit logs or simulated velocity
                val chartPoints = remember { listOf(200f, 600f, 400f, 1200f, 900f, 2100f, 1500f) }
                FinTechGrowthChart(
                    dataPoints = chartPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        // Transactions statement title
        item {
            Text(
                text = "Recent Secure Operations Log",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No past operations found. Deposit funds or refer friends to get started.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(transactions.take(5)) { tx ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (tx.amount >= 0) EmeraldPrimary.copy(alpha = 0.1f) else CoralDanger.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tx.amount >= 0) "+" else "-",
                                color = if (tx.amount >= 0) EmeraldPrimary else CoralDanger,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = tx.type.replace("_", " "),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tx.description,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = (if (tx.amount >= 0) "+" else "") + "${String.format("%.2f", tx.amount)} NPR",
                        color = if (tx.amount >= 0) EmeraldPrimary else CoralDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =========================================================
// TAB 1: FINANCE (DEPOSIT, WITHDRAW, QR, LOGS)
// =========================================================
@Composable
fun UserFinanceTab(
    user: User,
    deposits: List<Deposit>,
    withdrawals: List<Withdrawal>,
    config: SystemConfig,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isDepositForm by remember { mutableStateOf(true) }

    // Deposit fields
    var depositAmount by remember { mutableStateOf("") }
    var depMethod by remember { mutableStateOf("eSewa") }
    var transactionId by remember { mutableStateOf("") }

    // Withdrawal fields
    var withdrawAmount by remember { mutableStateOf("") }
    var withMethod by remember { mutableStateOf("eSewa") }
    var accountDetails by remember { mutableStateOf("") }

    var alertMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Selector row between Deposit and Withdraw
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDepositForm) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { isDepositForm = true; alertMsg = null; successMsg = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DEPOSIT", color = if (isDepositForm) EmeraldPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isDepositForm) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { isDepositForm = false; alertMsg = null; successMsg = null }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("WITHDRAW", color = if (!isDepositForm) EmeraldPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (isDepositForm) {
            // DEPOSIT SECTION
            item {
                GlassCard {
                    Text("Instant System Deposit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Min Deposit limit: NPR ${String.format("%.2f", config.minDepositAmount)}", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(10.dp))

                    // QR and Credentials based on payment method
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Column {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Official Scan Gateway", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Scan to settle payments securely", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.White.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Payment Gateway accounts:", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Bank of Nepal: Acct No: ${config.bankAccountNumber} | Name: ${config.bankAccountName}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                            Text("• eSewa ID: ${config.eSewaMobile} (Refchain Corporate)", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                            Text("• Khalti ID: ${config.khaltiMobile}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                            Text("• TRC20 Binance code: ${config.binanceAddress}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("Amount (NPR)") },
                        leadingIcon = { Icon(Icons.Default.Home, tint = EmeraldPrimary, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("dep_amount"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Method Row
                    Text("Gateway Method:", color = Color.White, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("eSewa", "Khalti", "Bank", "Binance").forEach { method ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (depMethod == method) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                    .border(width = 1.dp, color = if (depMethod == method) EmeraldPrimary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .clickable { depMethod = method }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(method, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = { Text("Transaction Reference ID") },
                        leadingIcon = { Icon(Icons.Default.Send, tint = EmeraldPrimary, contentDescription = null) },
                        placeholder = { Text("e.g. 543120938") },
                        modifier = Modifier.fillMaxWidth().testTag("dep_tx"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Note: Upload of transaction proof and valid Ref ID is mandatory. Admin manually verifies receipt within minutes.",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        lineHeight = 13.sp
                    )

                    if (alertMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(alertMsg ?: "", color = CoralDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg ?: "", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (loading) return@Button
                            alertMsg = null
                            successMsg = null
                            val amountParsed = depositAmount.toDoubleOrNull()
                            if (amountParsed == null || amountParsed <= 0) {
                                alertMsg = "Please input a valid positive amount."
                                return@Button
                            }
                            if (transactionId.isBlank()) {
                                alertMsg = "Unique Transaction Reference ID is required."
                                return@Button
                            }
                            loading = true
                            coroutineScope.launch {
                                val result = repository.submitDeposit(user.userId, amountParsed, depMethod, transactionId)
                                loading = false
                                result.onSuccess {
                                    successMsg = "Deposit request submitted! Awaiting manual verification from Board Admin."
                                    depositAmount = ""
                                    transactionId = ""
                                }.onFailure {
                                    alertMsg = it.message ?: "Failed submitting."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dep_submit_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SUBMIT DEPOSIT COMPREHENSIVE PROOF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // USER PAST DEPOSITS LIST
            item {
                Text("Your Deposit Operations", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (deposits.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No deposits submitted yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(deposits) { dep ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceCard)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ref ID: ${dep.transactionId}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Via: ${dep.paymentMethod} • NPR ${String.format("%.2f", dep.amount)}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }

                        StatusBadge(dep.status)
                    }
                }
            }
        } else {
            // WITHDRAWALS PAGE
            item {
                GlassCard {
                    Text("Secure Payout Withdrawal", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Wallet balance: NPR ${String.format("%.2f", user.walletBalance)}", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Amount (NPR)") },
                        leadingIcon = { Icon(Icons.Default.Home, tint = EmeraldPrimary, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("with_amount"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Method Selector
                    Text("Select Payout Channel:", color = Color.White, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("eSewa", "Khalti", "Bank", "Binance").forEach { method ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (withMethod == method) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                    .border(width = 1.dp, color = if (withMethod == method) EmeraldPrimary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .clickable { withMethod = method }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(method, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = accountDetails,
                        onValueChange = { accountDetails = it },
                        label = { Text("Wallet Account Details / Bank digits") },
                        leadingIcon = { Icon(Icons.Default.Edit, tint = EmeraldPrimary, contentDescription = null) },
                        placeholder = { Text("Mobile number or Wallet Address") },
                        modifier = Modifier.fillMaxWidth().testTag("with_details"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (alertMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(alertMsg ?: "", color = CoralDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(successMsg ?: "", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (loading) return@Button
                            alertMsg = null
                            successMsg = null
                            val amount = withdrawAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                alertMsg = "Please input a valid amount."
                                return@Button
                            }
                            if (accountDetails.isBlank()) {
                                alertMsg = "Destination payout account details are required."
                                return@Button
                            }
                            loading = true
                            coroutineScope.launch {
                                val result = repository.submitWithdrawal(user.userId, amount, withMethod, accountDetails)
                                loading = false
                                result.onSuccess {
                                    successMsg = "Withdrawal request logged! Funding approved queues usually verify within hours."
                                    withdrawAmount = ""
                                    accountDetails = ""
                                }.onFailure {
                                    alertMsg = it.message ?: "Failed submitting transaction."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("with_submit_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("REQUEST PAYOUT COMPLIANT TERMINATION", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            item {
                Text("Your Withdrawal Status", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (withdrawals.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No withdrawal requests logged yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(withdrawals) { with ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceCard)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("NPR ${String.format("%.2f", with.amount)} Payout Requested", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Method: ${with.paymentMethod} • Details: ${with.accountDetails}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }

                        StatusBadge(with.status)
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 2: NETWORK (REFERRAL DETAILS & INTERACTIVE HIERARCHY)
// =========================================================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserNetworkTab(
    user: User,
    usersList: List<User>,
    repository: RefChainRepository
) {
    val context = LocalContext.current

    // Recursive calculation of direct and indirect counts
    val directReferrals = usersList.filter { it.referredBy == user.referralCode }
    val directCodes = directReferrals.map { it.referralCode }
    // Indirect: referredBy is in direct codes
    val indirectReferrals = usersList.filter { it.referredBy != null && directCodes.contains(it.referredBy) }

    val totalDirect = directReferrals.size
    val totalIndirect = indirectReferrals.size

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Invite link card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Your Invites Ecosystem", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text("Invitational Code:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(user.referralCode, color = GoldSecondary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("RefChain Code", user.referralCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("copy_code_btn")
                    ) {
                        Text("Copy Code", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Invitational Link:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val url = "https://refchain.com/ref?code=${user.referralCode}"
                    Text(
                        url,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Copy Link",
                        tint = EmeraldPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                val clip = ClipData.newPlainText("RefChain Link", url)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }
        }

        // Stats row (direct vs indirect)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("DIRECT REFERRALS (LEVEL 1)", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$totalDirect registered", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Earn 40% on all deposit logs", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }

                GlassCard(modifier = Modifier.weight(1f)) {
                    Text("INDIRECT REFERRALS (LEVEL 2)", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$totalIndirect registered", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Earn 10% on nested trees", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }
            }
        }

        // Network Tree Visualization
        item {
            Text("Multi-Level Referral Graph (A → B → C)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Level 1: Root Node (Self)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Root User: ${user.name} (YOU)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (directReferrals.isEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(36.dp))
                        Text("Ecosystem graph currently empty.", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    } else {
                        directReferrals.forEach { direct ->
                            Spacer(modifier = Modifier.height(10.dp))
                            // Direction link arrow
                            Text("↓", color = GoldSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            // Level 2 Node direct referral
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldSecondary.copy(alpha = 0.15f))
                                    .border(1.dp, GoldSecondary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("L1 Invite: ${direct.name} (Earns you 40% Commission)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Level 3 nested nodes
                            val indirectlyMyReferred = indirectReferrals.filter { it.referredBy == direct.referralCode }
                            indirectlyMyReferred.forEach { indirect ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("   └── ↘", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier
                                        .padding(start = 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyanAccent.copy(alpha = 0.15f))
                                        .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("L2: ${indirect.name} (Earns you 10% via ${direct.name})", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // No self referrals warning
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CoralDanger.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compliance Integrity Policy:", color = CoralDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Self-referral spoof loops, fake credentials seeding, or duplicated IP setups will immediately freeze associated wallet resources.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// =========================================================
// TAB 3: HELP CENTER, CHAT WITH SUPPORT
// =========================================================
@Composable
fun UserSupportTab(
    user: User,
    tickets: List<SupportTicket>,
    config: SystemConfig,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var subject by remember { mutableStateOf("") }
    var issueType by remember { mutableStateOf("Financial") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var initMsg by remember { mutableStateOf("") }

    var alertMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Official Support Core", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (selectedTicket != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { selectedTicket = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("<- Back to Tickets", color = EmeraldPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Direct Quick channels
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { Toast.makeText(context, "Redirecting link: ${config.whatsappContactLink}", Toast.LENGTH_SHORT).show() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("WhatsApp Support", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(config.whatsappContactLink, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 8.sp)
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { Toast.makeText(context, "Contacting Email: ${config.supportEmail}", Toast.LENGTH_SHORT).show() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = GoldSecondary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Official Support Email", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(config.supportEmail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 8.sp)
                        }
                    }
                }
            }
        }

        if (selectedTicket != null) {
            val liveTicket = tickets.find { it.id == selectedTicket?.id } ?: selectedTicket!!

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Ticket Topic: ${liveTicket.subject}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        StatusBadge(liveTicket.status)
                        StatusBadge(liveTicket.priority)
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Parse simulated dialog from messagesStr
                    // Format: List structure parsed easily or falling back to simple line breaks
                    val parsedMessages = remember(liveTicket.messagesStr) {
                        try {
                            // Manual parser for simplified json arrays or string arrays
                            val entries = mutableListOf<Pair<String, String>>()
                            val items = liveTicket.messagesStr
                                .substringAfter("[").substringBeforeLast("]")
                                .split("},")
                            for (row in items) {
                                val sender = row.substringAfter("\"sender\":\"").substringBefore("\"")
                                val text = row.substringAfter("\"text\":\"").substringBefore("\"").replace("\\\"", "\"").replace("\\n", "\n")
                                if (sender.isNotBlank()) {
                                    entries.add(Pair(sender, text))
                                }
                            }
                            entries
                        } catch (e: Exception) {
                            listOf(Pair("SYSTEM", "Messages loaded as unified payload string: ${liveTicket.messagesStr}"))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        parsedMessages.forEach { msg ->
                            val isMe = msg.first == "USER"
                            val align = if (isMe) Alignment.End else Alignment.Start
                            val color = if (isMe) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                            val borderCol = if (isMe) EmeraldPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                            val label = if (isMe) "You" else "Director Support Support"

                            Column(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .align(align)
                                    .widthIn(max = 200.dp)
                            ) {
                                Text(label, color = GoldSecondary, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(0.5.dp, borderCol, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(msg.second, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (liveTicket.status != "RESOLVED") {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Write chat message reply...") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("reply_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (replyText.isBlank()) return@Button
                                    coroutineScope.launch {
                                        repository.replySupportTicket(liveTicket.id, "USER", replyText)
                                        replyText = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.testTag("reply_btn")
                            ) {
                                Text("Send", color = Color.Black)
                            }
                        }
                    } else {
                        Text("This ticket has been marked RESOLVED. Open a new ticket if issues persist.", color = EmeraldPrimary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // New ticket raising form
            item {
                GlassCard {
                    Text("Submit In-App Support Ticket", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Issue Subject") },
                        modifier = Modifier.fillMaxWidth().testTag("tkt_subject"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Financial", "Referral", "Technical", "Other").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (issueType == type) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                    .border(width = 1.dp, color = if (issueType == type) EmeraldPrimary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                    .clickable { issueType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = initMsg,
                        onValueChange = { initMsg = it },
                        label = { Text("Details or Transaction dispute message") },
                        modifier = Modifier.fillMaxWidth().testTag("tkt_msg"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (alertMsg != null) {
                        Text(alertMsg ?: "", color = CoralDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (successMsg != null) {
                        Text(successMsg ?: "", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (subject.isBlank() || initMsg.isBlank()) {
                                alertMsg = "All details are required."
                                return@Button
                            }
                            coroutineScope.launch {
                                repository.createSupportTicket(user.userId, subject, issueType, priority, initMsg)
                                successMsg = "Dispute Ticket logged successfully."
                                subject = ""
                                initMsg = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("tkt_submit_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SUBMIT TICKET QUERY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // PAST TICKETS
            item {
                Text("Your Past Ticket Logs", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            if (tickets.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No tickets generated yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(tickets) { tkt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceCard)
                            .clickable { selectedTicket = tkt }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(tkt.subject, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Category: ${tkt.issueType} • Priority: ${tkt.priority}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }

                        StatusBadge(tkt.status)
                    }
                }
            }
        }
    }
}
