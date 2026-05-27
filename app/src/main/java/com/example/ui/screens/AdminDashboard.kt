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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun AdminDashboard(
    adminUser: User,
    repository: RefChainRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSideMenu by remember { mutableStateOf("Overview") }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Observe master Flows
    val usersList by repository.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val depositsList by repository.allDeposits.collectAsStateWithLifecycle(initialValue = emptyList())
    val withdrawalsList by repository.allWithdrawals.collectAsStateWithLifecycle(initialValue = emptyList())
    val transactionsList by repository.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val ticketsList by repository.allTickets.collectAsStateWithLifecycle(initialValue = emptyList())
    val logsList by repository.allAuditLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    val systemConfigState by repository.systemConfig.collectAsStateWithLifecycle(initialValue = SystemConfig())

    val config = systemConfigState ?: SystemConfig()
    val currentAdminLive = usersList.find { it.userId == adminUser.userId } ?: adminUser

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(EmeraldPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RefChain Admin Central Console", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "Admin Balance: NPR ${String.format("%.2f", currentAdminLive.walletBalance)}",
                            fontSize = 11.sp,
                            color = GoldSecondary,
                            fontWeight = FontWeight.SemiBold
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
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Menu for Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val menuItems = listOf("Overview", "Deposits", "Withdrawals", "Users", "Excel Export", "Tickets & Support", "Global Alert & Fraud", "Ecosystem Settings")
                items(menuItems) { item ->
                    val isSelected = selectedSideMenu == item
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.05f))
                            .clickable { selectedSideMenu = item }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedSideMenu) {
                    "Overview" -> AdminOverview(currentAdminLive, usersList, depositsList, withdrawalsList, transactionsList, logsList, repository)
                    "Deposits" -> AdminDepositsQueue(depositsList, repository)
                    "Withdrawals" -> AdminWithdrawalsQueue(withdrawalsList, repository)
                    "Users" -> AdminUsersManage(usersList, repository)
                    "Excel Export" -> AdminExcelExportCenter(usersList, depositsList, withdrawalsList, transactionsList, repository)
                    "Tickets & Support" -> AdminTicketsManage(ticketsList, repository)
                    "Global Alert & Fraud" -> AdminFraudAlerts(logsList, usersList, repository)
                    "Ecosystem Settings" -> AdminSystemConfig(config, repository)
                }
            }
        }
    }
}

// =========================================================
// SECTION 1: OVERVIEW SCREEN WITH KPI CARDS & GROWTH CHARTS
// =========================================================
@Composable
fun AdminOverview(
    adminUser: User,
    users: List<User>,
    deposits: List<Deposit>,
    withdrawals: List<Withdrawal>,
    transactions: List<Transaction>,
    logs: List<AuditLog>,
    repository: RefChainRepository
) {
    val totalUsers = users.size
    val totalDepositsValue = deposits.filter { it.status == "APPROVED" }.sumOf { it.amount }
    val totalPayoutsValue = withdrawals.filter { it.status == "COMPLETED" }.sumOf { it.amount }
    val pendingDeposits = deposits.count { it.status == "PENDING" }
    val pendingWithdrawals = withdrawals.count { it.status == "PENDING" }
    val totalComms = transactions.filter { it.type == "DIRECT_COMMISSION" || it.type == "INDIRECT_COMMISSION" }.sumOf { it.amount }
    val alertCount = logs.count { it.isAlert }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Action Row
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Operational Quick Actions", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Approve All Pending Deposits
                                var approvedCount = 0
                                deposits.filter { it.status == "PENDING" }.forEach {
                                    repository.processDeposit(it.id, approve = true)
                                    approvedCount++
                                }
                                Toast.makeText(context, "$approvedCount Deposits approved securely!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Approve Pending Dep", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Clear Audit Alert Logs
                                var approvedCount = 0
                                withdrawals.filter { it.status == "PENDING" }.forEach {
                                    repository.processWithdrawal(it.id, action = "APPROVE")
                                    approvedCount++
                                }
                                Toast.makeText(context, "$approvedCount Withdrawals processed successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Approve Pending With", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        // Admin Self Account customization
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Admin / Board Self-Wallet Customizer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Customize your administrative account balance. Changes apply immediately in the database.", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(12.dp))

                var selfTargetBalance by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = selfTargetBalance,
                        onValueChange = { selfTargetBalance = it },
                        label = { Text("Input New Absolute Balance (NPR)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("self_adjust_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = EmeraldPrimary,
                            cursorColor = EmeraldPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val target = selfTargetBalance.toDoubleOrNull()
                            if (target == null || target < 0) {
                                Toast.makeText(context, "Please enter a valid positive balance", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                val diff = target - adminUser.walletBalance
                                repository.manualWalletAdjustment(adminUser.userId, diff, "CEO Self-Balance Override/Customization")
                                Toast.makeText(context, "Administrative account settled to NPR ${String.format("%.2f", target)} successfully!", Toast.LENGTH_SHORT).show()
                                selfTargetBalance = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp).testTag("self_adjust_submit_btn")
                    ) {
                        Text("SET BALANCE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fast Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Add 10k" to 10000.0,
                        "Add 100k" to 100000.0,
                        "Set to 1M" to 1000000.0,
                        "Reset to 0" to 0.0
                    )
                    presets.forEach { (label, value) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        if (label.startsWith("Add")) {
                                            repository.manualWalletAdjustment(adminUser.userId, value, "Quick Add Offset")
                                            Toast.makeText(context, "Added NPR ${String.format("%.2f", value)} to administrative ledger!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val diff = value - adminUser.walletBalance
                                            repository.manualWalletAdjustment(adminUser.userId, diff, "Quick Preset Override")
                                            Toast.makeText(context, "Ledger settled to NPR ${String.format("%.2f", value)}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Two-column KPI view
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "TOTAL REGISTERED USERS",
                    value = "$totalUsers Accounts",
                    subtitle = "All verified accounts",
                    trendUp = true,
                    icon = { Icon(Icons.Default.Person, tint = EmeraldPrimary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "TOTAL MANUAL DEPOSITS",
                    value = "NPR ${String.format("%.2f", totalDepositsValue)}",
                    subtitle = "Settled assets pool",
                    trendUp = true,
                    icon = { Icon(Icons.Default.Home, tint = EmeraldPrimary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "TOTAL DECLARED PAYOUTS",
                    value = "NPR ${String.format("%.2f", totalPayoutsValue)}",
                    subtitle = "Completed withdrawals queue",
                    trendUp = false,
                    icon = { Icon(Icons.Default.Send, tint = CoralDanger, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "NET NETWORK COMMISSIONS",
                    value = "NPR ${String.format("%.2f", totalComms)}",
                    subtitle = "L1 (40%) and L2 (10%) payouts",
                    trendUp = true,
                    icon = { Icon(Icons.Default.Info, tint = GoldSecondary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "PENDING DEPOSITS QUEUE",
                    value = "$pendingDeposits Pending",
                    subtitle = "Awaiting manual audit",
                    trendUp = false,
                    icon = { Icon(Icons.Default.Refresh, tint = GoldSecondary, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "FRAUD SYSTEM DISCOVERIES",
                    value = "$alertCount Compliance Flags",
                    subtitle = "Security policy alerts",
                    trendUp = false,
                    icon = { Icon(Icons.Default.Lock, tint = CoralDanger, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Animated Growth charts
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Corporate Financial Momentum (NPR Scale)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Real-time tracked deposits and system assets", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(10.dp))

                FinTechGrowthChart(
                    dataPoints = listOf(1000f, 3000f, 5000f, 8000f, 12000f, totalDepositsValue.toFloat().coerceAtLeast(14000f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}

// =========================================================
// SECTION 2: DEPOSITS MANUAL PROCESSING QUEUE
// =========================================================
@Composable
fun AdminDepositsQueue(
    deposits: List<Deposit>,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Pending Deposits Verification Queue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Verify transaction uploads manually. Balance and commissions dispatch reactively on approval.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        val pending = deposits.filter { it.status == "PENDING" || it.status == "UNDER_REVIEW" }

        if (pending.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No pending deposit verification claims found.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(pending) { dep ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("User: ${dep.userName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Mobile: ${dep.userId}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Claim: NPR ${String.format("%.2f", dep.amount)} via ${dep.paymentMethod}", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Tx ID Ref: ${dep.transactionId}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(dep.status)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.processDeposit(dep.id, approve = false)
                                            Toast.makeText(context, "Deposit Rejected", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reject", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.processDeposit(dep.id, approve = true)
                                            Toast.makeText(context, "Deposit Approved! Multi-level commissions dispatch done.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(width = 80.dp, height = 32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Approve", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SECTION 3: WITHDRAWALS QUEUE WITH MULTIPLE STAGES
// =========================================================
@Composable
fun AdminWithdrawalsQueue(
    withdrawals: List<Withdrawal>,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Payout Withdrawals Queue & Risk Board", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Approve requests to processing, and complete them upon verifying final local bank transfers.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (withdrawals.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No past or pending withdrawals requested.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(withdrawals) { with ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recipient: ${with.userName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Payout: NPR ${String.format("%.2f", with.amount)} via ${with.paymentMethod}", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Destination Account: ${with.accountDetails}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Fraud System Risk Score: ${with.riskScore}/100", color = if (with.riskScore > 15) CoralDanger else GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(with.status)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons depending on state
                            if (with.status == "PENDING") {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.processWithdrawal(with.id, "REJECT")
                                                Toast.makeText(context, "Withdrawal Rejected & Refunded", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Reject", color = Color.White, fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.processWithdrawal(with.id, "APPROVE")
                                                Toast.makeText(context, "Approved to processing queue", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                    ) {
                                        Text("Settle", color = Color.Black, fontSize = 10.sp)
                                    }
                                }
                            } else if (with.status == "PROCESSING") {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.processWithdrawal(with.id, "COMPLETE")
                                            Toast.makeText(context, "Marked COMPLETED successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text("Complete Payout", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SECTION 4: USER CONTROL (SUSPEND, ADJUST BALANCE)
// =========================================================
@Composable
fun AdminUsersManage(
    users: List<User>,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedUserForAdjust by remember { mutableStateOf<User?>(null) }
    var adjustAmount by remember { mutableStateOf("") }
    var adjustReason by remember { mutableStateOf("Commission loyalty reward") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedUserForAdjust != null) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Manual Adjustments Panel", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Target: ${selectedUserForAdjust?.name} (${selectedUserForAdjust?.userId})", color = GoldSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = adjustAmount,
                        onValueChange = { adjustAmount = it },
                        label = { Text("Amount adjustment (negative value to deduct)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("adjust_amount"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = adjustReason,
                        onValueChange = { adjustReason = it },
                        label = { Text("Adjustment Justification / Reason") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("adjust_reason"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { selectedUserForAdjust = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val amount = adjustAmount.toDoubleOrNull() ?: 0.0
                                if (amount == 0.0) return@Button
                                coroutineScope.launch {
                                    repository.manualWalletAdjustment(selectedUserForAdjust!!.userId, amount, adjustReason)
                                    Toast.makeText(context, "Balance updated successfully!", Toast.LENGTH_SHORT).show()
                                    selectedUserForAdjust = null
                                    adjustAmount = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("adjust_submit")
                        ) {
                            Text("Confirm Balance Mod", color = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            Text("Ecosystem User Directory (${users.size} Accounts)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        items(users) { u ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(u.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Email: ${u.email}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text("Role: ${u.role} | Referral Code: ${u.referralCode}", color = GoldSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        StatusBadge(u.status)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wallet Bal: NPR ${String.format("%.2f", u.walletBalance)}", color = EmeraldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Commissions: NPR ${String.format("%.2f", u.commissionsEarned)}", color = CyanAccent, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Suspend / Activate Toggle
                            val isActive = u.status == "ACTIVE"
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.setUserStatus(u.userId, !isActive)
                                        Toast.makeText(context, "User status adjusted!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (isActive) CoralDanger.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Close else Icons.Default.Check,
                                    tint = if (isActive) CoralDanger else EmeraldPrimary,
                                    contentDescription = "Status Adjust",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Manual Adjust Button
                            Button(
                                onClick = { selectedUserForAdjust = u },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("NPR Adjust", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SECTION 5: EXCEL/CSV EXPORT DIRECT VISUAL CENTER (IMPORTANT)
// =========================================================
@Composable
fun AdminExcelExportCenter(
    users: List<User>,
    deposits: List<Deposit>,
    withdrawals: List<Withdrawal>,
    transactions: List<Transaction>,
    repository: RefChainRepository
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var activeExportType by remember { mutableStateOf("Users") }
    var previewText by remember { mutableStateOf("") }

    // Recompute statement previews whenever context changes
    LaunchedEffect(activeExportType, users, deposits, withdrawals, transactions) {
        previewText = when (activeExportType) {
            "Users" -> repository.generateUsersCsv(users)
            "Deposits" -> repository.generateDepositsCsv(deposits)
            "Withdrawals" -> repository.generateWithdrawalsCsv(withdrawals)
            "Commissions" -> repository.generateCommissionsCsv(transactions)
            else -> "Select category to trigger spreadsheet creation."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Excel Reporting & Auditing Center", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Export compliant financial logs in CSV-Excel format to quickly settle claims, support users, and prove system solvency.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }

        item {
            // Excel Type selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Users", "Deposits", "Withdrawals", "Commissions").forEach { category ->
                    val isCat = activeExportType == category
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCat) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(width = if (isCat) 1.dp else 0.dp, color = if (isCat) EmeraldPrimary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                            .clickable { activeExportType = category }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(category, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$activeExportType Tabular Sheet Simulator", color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("XLS Report", previewText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "$activeExportType statement saved to clipboard. Ready to paste in Microsoft Excel!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("export_copy_btn"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export XLS", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Structured table list simulation of excel rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    val rows = previewText.split("\n").filter { it.isNotBlank() }
                    if (rows.isEmpty()) {
                        Text("No rows to display", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                    } else {
                        // Display headers
                        Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.08f)).padding(4.dp)) {
                            val cols = rows[0].split(",")
                            cols.take(3).forEach { header ->
                                Text(
                                    text = header.replace("\"", ""),
                                    modifier = Modifier.weight(1f),
                                    color = GoldSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            if (cols.size > 3) {
                                Text(text = cols[3].replace("\"", ""), modifier = Modifier.weight(1f), color = GoldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        // Display raw excel previews
                        rows.drop(1).take(12).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp)) {
                                val cols = row.split(",")
                                cols.take(3).forEach { colValue ->
                                    Text(
                                        text = colValue.replace("\"", ""),
                                        modifier = Modifier.weight(1f),
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 9.sp,
                                        maxLines = 2
                                    )
                                }
                                if (cols.size > 3) {
                                    Text(
                                        text = cols[3].replace("\"", ""),
                                        modifier = Modifier.weight(1f),
                                        color = EmeraldPrimary,
                                        fontSize = 9.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        if (rows.size > 13) {
                            Text(
                                text = "Showing 12 cells of ${rows.size - 1} rows. Click Export XLS to copy complete database.",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SECTION 6: SUPPORT TICKETS LISTS & RESOLUTION
// =========================================================
@Composable
fun AdminTicketsManage(
    tickets: List<SupportTicket>,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var replyingTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (replyingTicket != null) {
            val freshTicket = tickets.find { it.id == replyingTicket?.id } ?: replyingTicket!!

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chat Ticket: ${freshTicket.subject}", color = Color.White, fontSize = 14.sp)
                        StatusBadge(freshTicket.status)
                    }
                    Text("User ID: ${freshTicket.userId} [${freshTicket.userName}]", color = GoldSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Write automated or manual support dispatcher response...") },
                        modifier = Modifier.fillMaxWidth().testTag("admin_reply"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { replyingTicket = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back", color = Color.White)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.resolveSupportTicket(freshTicket.id)
                                    Toast.makeText(context, "Ticket Resolved", Toast.LENGTH_SHORT).show()
                                    replyingTicket = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                            modifier = Modifier.weight(1f).testTag("resolve_btn")
                        ) {
                            Text("Mark Resolved", color = Color.White)
                        }

                        Button(
                            onClick = {
                                if (replyText.isBlank()) return@Button
                                coroutineScope.launch {
                                    repository.replySupportTicket(freshTicket.id, "ADMIN", replyText)
                                    replyText = ""
                                    Toast.makeText(context, "Reply Dispatched", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("dispatch_reply")
                        ) {
                            Text("Dispatch Reply", color = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            Text("Open Customer Support Tickets Dashboard", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (tickets.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No customer support claims or disputes logged.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(tickets) { tkt ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Title: ${tkt.subject}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Reporter: ${tkt.userName} (${tkt.userId})", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text("Category: ${tkt.issueType} | Priority: ${tkt.priority}", color = GoldSecondary, fontSize = 11.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(tkt.status)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { replyingTicket = tkt; replyText = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Engage", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SECTION 7: FRAUD DETECTION & AUDIT LOGS ALERTS
// =========================================================
@Composable
fun AdminFraudAlerts(
    logs: List<AuditLog>,
    users: List<User>,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val alerts = logs.filter { it.isAlert }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Ecosystem Fraud Detection Board", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("AI self-referral blocks, multiple device registry filters, and suspicious compliance breaches.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }

        if (alerts.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No suspicious activities detected in this epoch.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            items(alerts) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CoralDanger.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, CoralDanger.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COMPLIANCE VIOLATION", color = CoralDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("HIGH RISK", color = CoralDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(alert.details, color = Color.White, fontSize = 12.sp)
                        Text("Linked ID: ${alert.userId} | Reporter: ${alert.userName}", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.setUserStatus(alert.userId, false)
                                        Toast.makeText(context, "Account Frozen successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralDanger),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Freeze Wallet", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("System Audit Logs", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Limit audit trail display for ease
        items(logs.take(15)) { log ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(log.actionType + ": " + log.details, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                    Text("By: ${log.userName} (${log.userId})", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                }

                if (log.isAlert) {
                    StatusBadge("ALERT")
                }
            }
        }
    }
}

// =========================================================
// SECTION 8: GLOBAL SETTINGS
// =========================================================
@Composable
fun AdminSystemConfig(
    config: SystemConfig,
    repository: RefChainRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var minDep by remember { mutableStateOf(config.minDepositAmount.toString()) }
    var minWith by remember { mutableStateOf(config.minWithdrawalAmount.toString()) }
    var l1Pct by remember { mutableStateOf(config.directReferralPercentage.toString()) }
    var l2Pct by remember { mutableStateOf(config.indirectReferralPercentage.toString()) }

    var eSewaId by remember { mutableStateOf(config.eSewaMobile) }
    var khaltiId by remember { mutableStateOf(config.khaltiMobile) }
    var binanceAddr by remember { mutableStateOf(config.binanceAddress) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("System Configuration Core", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Configure reward percentages, minimum limits and payout e-wallet details global variables instantly.", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Finance & Level Percentages Matrix", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = minDep,
                    onValueChange = { minDep = it },
                    label = { Text("Minimum deposit trigger limit (NPR)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("min_dep_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = minWith,
                    onValueChange = { minWith = it },
                    label = { Text("Minimum withdrawal payout trigger (NPR)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("min_with_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = l1Pct,
                    onValueChange = { l1Pct = it },
                    label = { Text("Direct Referral Level 1 Award (%)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("l1_pct_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = l2Pct,
                    onValueChange = { l2Pct = it },
                    label = { Text("Indirect Referral Level 2 Award (%)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("l2_pct_input"),
                    singleLine = true
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("QR & Gateway Coordinates", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = eSewaId,
                    onValueChange = { eSewaId = it },
                    label = { Text("eSewa Pay Id Number") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("esewa_id_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = khaltiId,
                    onValueChange = { khaltiId = it },
                    label = { Text("Khalti Wallet Identifier") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("khalti_id_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = binanceAddr,
                    onValueChange = { binanceAddr = it },
                    label = { Text("USDT Cryptography address") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("binance_addr_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val parsedMinDep = minDep.toDoubleOrNull() ?: 500.0
                        val parsedMinWith = minWith.toDoubleOrNull() ?: 200.0
                        val parsedL1 = l1Pct.toDoubleOrNull() ?: 40.0
                        val parsedL2 = l2Pct.toDoubleOrNull() ?: 10.0

                        coroutineScope.launch {
                            repository.updateSystemConfig(
                                config.copy(
                                    minDepositAmount = parsedMinDep,
                                    minWithdrawalAmount = parsedMinWith,
                                    directReferralPercentage = parsedL1,
                                    indirectReferralPercentage = parsedL2,
                                    eSewaMobile = eSewaId,
                                    khaltiMobile = khaltiId,
                                    binanceAddress = binanceAddr
                                )
                            )
                            Toast.makeText(context, "Ecosystem global configurations persisted!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_submit")
                ) {
                    Text("SAVE ALL PARAMETERS TO CORE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
