package com.example.data.repository

import com.example.data.db.RefChainDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class RefChainRepository(private val dao: RefChainDao) {

    // --- USERS FLOWS & PERSISTENCE ---
    val allUsers: Flow<List<User>> = dao.getAllAsFlow()
    val allDeposits: Flow<List<Deposit>> = dao.getAllDepositsFlow()
    val allWithdrawals: Flow<List<Withdrawal>> = dao.getAllWithdrawalsFlow()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactionsFlow()
    val allTickets: Flow<List<SupportTicket>> = dao.getAllTicketsFlow()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogsFlow()
    val systemConfig: Flow<SystemConfig?> = dao.getConfigFlow()

    suspend fun getUser(userId: String): User? = dao.getUserById(userId)
    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)
    
    fun getTransactionsForUser(userId: String): Flow<List<Transaction>> = dao.getTransactionsByUserId(userId)
    fun getDepositsForUser(userId: String): Flow<List<Deposit>> = dao.getDepositsByUserId(userId)
    fun getWithdrawalsForUser(userId: String): Flow<List<Withdrawal>> = dao.getWithdrawalsByUserId(userId)
    fun getTicketsForUser(userId: String): Flow<List<SupportTicket>> = dao.getTicketsByUserId(userId)

    suspend fun insertAuditLog(userId: String, userName: String, actionType: String, details: String, isAlert: Boolean = false) {
        dao.insertAuditLog(AuditLog(
            userId = userId,
            userName = userName,
            actionType = actionType,
            details = details,
            isAlert = isAlert
        ))
    }

    // --- REGISTRATION / AUTHENTICATION LOGIC ---
    suspend fun registerUser(
        name: String,
        email: String,
        phone: String,
        whatsapp: String,
        passwordPlain: String,
        referredByCode: String?
    ): Result<User> {
        if (dao.getUserByEmail(email) != null) {
            return Result.failure(Exception("Email already registered."))
        }
        if (dao.getUserById(phone) != null) {
            return Result.failure(Exception("Phone number already registered."))
        }

        // Validate referral code if present
        var validReferredBy: String? = null
        if (!referredByCode.isNullOrBlank()) {
            val referrer = dao.getUserByReferralCode(referredByCode.trim().uppercase(Locale.ROOT))
            if (referrer != null) {
                validReferredBy = referrer.referralCode
                // Prevent self referral
                if (referrer.phone == phone || referrer.email == email) {
                    return Result.failure(Exception("Self-referrals are not allowed."))
                }
            } else {
                return Result.failure(Exception("Invalid referral code entered."))
            }
        }

        // Generate custom short referral code
        val cleanPhonePart = if (phone.length >= 4) phone.takeLast(4) else phone
        val cleanNamePart = name.take(3).uppercase(Locale.ROOT).replace(" ","G")
        val generatedCode = "${cleanNamePart}${cleanPhonePart}"

        val newUser = User(
            userId = phone, // use phone as unique user id
            name = name,
            email = email,
            phone = phone,
            whatsapp = whatsapp,
            passwordHash = passwordPlain, // plain for demo simplicity, but styled as hashed
            referralCode = generatedCode,
            referredBy = validReferredBy,
            walletBalance = 0.0,
            commissionsEarned = 0.0
        )

        dao.insertUser(newUser)
        insertAuditLog(newUser.userId, newUser.name, "SIGNUP", "User registered successfully using referral: ${validReferredBy ?: "None"}")
        
        return Result.success(newUser)
    }

    suspend fun authenticateUser(email: String, passwordPlain: String): Result<User> {
        val user = dao.getUserByEmail(email.trim()) ?: return Result.failure(Exception("No account linked with this email."))
        if (user.passwordHash != passwordPlain) {
            return Result.failure(Exception("Incorrect password string."))
        }
        if (user.status == "SUSPENDED") {
            return Result.failure(Exception("This account is suspended by Admin due to regulatory compliance or security review."))
        }

        insertAuditLog(user.userId, user.name, "LOGIN", "Successful secure session login")
        return Result.success(user)
    }

    // --- DEPOSIT PROCESSOR FLOW ---
    suspend fun submitDeposit(userId: String, amount: Double, method: String, transactionId: String): Result<Deposit> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val config = dao.getConfig() ?: SystemConfig()

        if (amount < config.minDepositAmount) {
            return Result.failure(Exception("Minimum deposit amount is NPR ${config.minDepositAmount}"))
        }

        val deposit = Deposit(
            userId = userId,
            userName = user.name,
            amount = amount,
            paymentMethod = method,
            transactionId = transactionId.trim(),
            screenshotUri = "payment_screenshot_placeholder"
        )
        dao.insertDeposit(deposit)
        insertAuditLog(userId, user.name, "DEPOSIT_PROOF_UPLOAD", "Uploaded deposit request $amount NPR via $method. TxId: $transactionId")
        return Result.success(deposit)
    }

    // --- WITHDRAWAL SUBMISSION ---
    suspend fun submitWithdrawal(userId: String, amount: Double, method: String, accountDetails: String): Result<Withdrawal> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val config = dao.getConfig() ?: SystemConfig()

        if (amount < config.minWithdrawalAmount) {
            return Result.failure(Exception("Minimum withdrawal is NPR ${config.minWithdrawalAmount}"))
        }
        if (user.walletBalance < amount) {
            return Result.failure(Exception("Insufficient wallet balance for this withdrawal request."))
        }

        // Anti-fraud: assess risk score based on rapid submissions or self checks
        var risk = 0
        if (amount > 10000) risk += 20
        if (user.commissionsEarned == 0.0) risk += 15

        // Deduct balance straight away as "locked" or "pending withdrawal"
        val updatedUser = user.copy(walletBalance = user.walletBalance - amount)
        dao.insertUser(updatedUser)

        val withdrawal = Withdrawal(
            userId = userId,
            userName = user.name,
            amount = amount,
            paymentMethod = method,
            accountDetails = accountDetails,
            riskScore = risk
        )
        dao.insertWithdrawal(withdrawal)
        
        dao.insertTransaction(Transaction(
            userId = userId,
            amount = -amount,
            type = "WITHDRAWAL",
            description = "Requested NPR $amount withdrawal via $method (Pending approval)"
        ))

        insertAuditLog(userId, user.name, "WITHDRAW_REQUEST", "Requested NPR $amount to $accountDetails (Risk score: $risk)")
        return Result.success(withdrawal)
    }

    // --- ADMIN ACTION: MANUAL DEPOSIT VERIFICATION ---
    suspend fun processDeposit(depositId: Int, approve: Boolean): Result<Boolean> {
        val deposit = dao.getDepositById(depositId) ?: return Result.failure(Exception("Deposit request not found"))
        if (deposit.status != "PENDING" && deposit.status != "UNDER_REVIEW") {
            return Result.failure(Exception("Deposit is already processed with state ${deposit.status}"))
        }

        if (approve) {
            val user = dao.getUserById(deposit.userId) ?: return Result.failure(Exception("Associated depositor user not found"))
            
            // 1. Update deposit status
            val approvedDeposit = deposit.copy(status = "APPROVED")
            dao.updateDeposit(approvedDeposit)

            // 2. Add deposit funds to user
            val updatedUser = user.copy(walletBalance = user.walletBalance + deposit.amount)
            dao.insertUser(updatedUser)

            // 3. Log transaction
            dao.insertTransaction(Transaction(
                userId = deposit.userId,
                amount = deposit.amount,
                type = "DEPOSIT",
                description = "NPR ${deposit.amount} e-Payment manually verified & approved (TxId: ${deposit.transactionId})"
            ))

            insertAuditLog(deposit.userId, user.name, "DEPOSIT_APPROVE", "Deposit approved for NPR ${deposit.amount} by Admin")

            // 4. MULTI-LEVEL REFERRAL DISTRIBUTION
            distributeReferralCommissions(user, deposit.amount)
        } else {
            val rejectedDeposit = deposit.copy(status = "REJECTED")
            dao.updateDeposit(rejectedDeposit)
            insertAuditLog(deposit.userId, deposit.userName, "DEPOSIT_REJECT", "Deposit rejected for NPR ${deposit.amount} by Admin")
        }

        return Result.success(true)
    }

    // Private helper for referral commission hierarchy distribution
    private suspend fun distributeReferralCommissions(depositor: User, depositAmount: Double) {
        val config = dao.getConfig() ?: SystemConfig()

        // LEVEL 1: Direct Referral Commission (e.g. 40%)
        val parentReferralCode = depositor.referredBy
        if (!parentReferralCode.isNullOrBlank()) {
            val parentUser = dao.getUserByReferralCode(parentReferralCode)
            if (parentUser != null) {
                val directCommissionAmount = depositAmount * (config.directReferralPercentage / 100.0)
                
                // Update parent wallet + total earned commission
                val updatedParent = parentUser.copy(
                    walletBalance = parentUser.walletBalance + directCommissionAmount,
                    commissionsEarned = parentUser.commissionsEarned + directCommissionAmount
                )
                dao.insertUser(updatedParent)

                // Log Parent's direct commission transaction
                dao.insertTransaction(Transaction(
                    userId = parentUser.userId,
                    amount = directCommissionAmount,
                    type = "DIRECT_COMMISSION",
                    description = "Direct Commission (${config.directReferralPercentage}%) from deposit of ${depositor.name}"
                ))
                
                insertAuditLog(
                    parentUser.userId, 
                    parentUser.name, 
                    "COMMISSION_PAID", 
                    "Earned direct commission NPR $directCommissionAmount from ${depositor.name}'s deposit"
                )

                // LEVEL 2: Indirect Referral Commission (e.g. 10%)
                val grandParentReferralCode = parentUser.referredBy
                if (!grandParentReferralCode.isNullOrBlank()) {
                    val grandParentUser = dao.getUserByReferralCode(grandParentReferralCode)
                    if (grandParentUser != null) {
                        val indirectCommissionAmount = depositAmount * (config.indirectReferralPercentage / 100.0)

                        // Update grand parent wallet + total earned commission
                        val updatedGrandParent = grandParentUser.copy(
                            walletBalance = grandParentUser.walletBalance + indirectCommissionAmount,
                            commissionsEarned = grandParentUser.commissionsEarned + indirectCommissionAmount
                        )
                        dao.insertUser(updatedGrandParent)

                        // Log GrandParent's indirect commission transaction
                        dao.insertTransaction(Transaction(
                            userId = grandParentUser.userId,
                            amount = indirectCommissionAmount,
                            type = "INDIRECT_COMMISSION",
                            description = "Indirect Commission (${config.indirectReferralPercentage}%) from deposit of ${depositor.name} via ${parentUser.name}"
                        ))
                        
                        insertAuditLog(
                            grandParentUser.userId, 
                            grandParentUser.name, 
                            "COMMISSION_PAID", 
                            "Earned indirect commission NPR $indirectCommissionAmount from ${depositor.name}'s activity via ${parentUser.name}"
                        )
                    }
                }
            }
        }
    }

    // --- ADMIN ACTION: PROCESS WITHDRAWAL ---
    suspend fun processWithdrawal(withdrawalId: Int, action: String): Result<Boolean> {
        val withdrawal = dao.getWithdrawalById(withdrawalId) ?: return Result.failure(Exception("Withdrawal request not found"))
        if (withdrawal.status != "PENDING" && withdrawal.status != "PROCESSING") {
            return Result.failure(Exception("Withdrawal is already in state: ${withdrawal.status}"))
        }

        val user = dao.getUserById(withdrawal.userId) ?: return Result.failure(Exception("Associated user not found"))

        when (action) {
            "APPROVE" -> {
                // Advance status to PROCESSING
                val updated = withdrawal.copy(status = "PROCESSING")
                dao.updateWithdrawal(updated)
                insertAuditLog(withdrawal.userId, withdrawal.userName, "WITHDRAW_PROCESS", "Withdrawal moved to processing queue")
            }
            "REJECT" -> {
                // Return locked balance to the user!
                val refundedUser = user.copy(walletBalance = user.walletBalance + withdrawal.amount)
                dao.insertUser(refundedUser)

                val updated = withdrawal.copy(status = "REJECTED")
                dao.updateWithdrawal(updated)

                dao.insertTransaction(Transaction(
                    userId = withdrawal.userId,
                    amount = withdrawal.amount,
                    type = "ADMIN_ADJUST",
                    description = "NPR ${withdrawal.amount} withdrawal rejection refunded to balance"
                ))
                insertAuditLog(withdrawal.userId, withdrawal.userName, "WITHDRAW_REJECTED", "Withdrawal of ${withdrawal.amount} NPR was rejected and refunded")
            }
            "COMPLETE" -> {
                // Confirm payout completed
                val updated = withdrawal.copy(status = "COMPLETED")
                dao.updateWithdrawal(updated)

                insertAuditLog(withdrawal.userId, withdrawal.userName, "WITHDRAW_COMPLETED", "Withdrawal of ${withdrawal.amount} NPR finalized and completed transfer")
            }
        }

        return Result.success(true)
    }

    // --- ADMIN ACTIONS: USER CONTROL ---
    suspend fun setUserStatus(userId: String, active: Boolean): Result<Boolean> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val finalStatus = if (active) "ACTIVE" else "SUSPENDED"
        val updated = user.copy(status = finalStatus)
        dao.insertUser(updated)
        
        insertAuditLog(userId, user.name, "SUSPENSION", "Account status manually set to $finalStatus by Administrator")
        return Result.success(true)
    }

    suspend fun manualWalletAdjustment(userId: String, adjustmentAmount: Double, reason: String): Result<Boolean> {
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found"))
        val updated = user.copy(walletBalance = user.walletBalance + adjustmentAmount)
        dao.insertUser(updated)

        dao.insertTransaction(Transaction(
            userId = userId,
            amount = adjustmentAmount,
            type = "ADMIN_ADJUST",
            description = "Admin Balance adjustment: $reason"
        ))

        insertAuditLog(userId, user.name, "ADMIN_ADJUST", "Manual balance adjustment: $adjustmentAmount NPR. Reason: $reason")
        return Result.success(true)
    }

    // --- SYSTEM CONFS UPDATE ---
    suspend fun updateSystemConfig(config: SystemConfig) {
        dao.insertConfig(config)
    }

    // --- TICKETS COMMUNICATOR ---
    suspend fun createSupportTicket(userId: String, subject: String, issueType: String, priority: String, initialMessage: String) {
        val user = dao.getUserById(userId) ?: return
        val messagesJson = """
            [
              {"sender":"USER", "text":"${initialMessage.replace("\"", "\\\"")}", "time": ${System.currentTimeMillis()}}
            ]
        """.trimIndent()

        val ticket = SupportTicket(
            userId = userId,
            userName = user.name,
            subject = subject,
            issueType = issueType,
            priority = priority,
            messagesStr = messagesJson
        )
        dao.insertTicket(ticket)
        insertAuditLog(userId, user.name, "SUPPORT_TICKET_RAISE", "Raised ticket ID regarding $issueType ($priority priority)")
    }

    suspend fun replySupportTicket(ticketId: Int, senderRole: String, text: String) {
        val ticket = dao.getTicketById(ticketId) ?: return
        val escapedText = text.replace("\"", "\\\"").replace("\n", "\\n")
        val timestamp = System.currentTimeMillis()
        
        // Strip ending bracket and append new JSON message entry
        val currentJson = ticket.messagesStr.trim()
        val updatedJson = if (currentJson.endsWith("]")) {
            val base = currentJson.substring(0, currentJson.length - 1)
            val comma = if (base.trim().endsWith("{")) "" else ","
            "$base$comma{\"sender\":\"$senderRole\", \"text\":\"$escapedText\", \"time\": $timestamp}]"
        } else {
            "[{\"sender\":\"$senderRole\", \"text\":\"$escapedText\", \"time\": $timestamp}]"
        }

        val updatedTicket = ticket.copy(
            messagesStr = updatedJson,
            status = if (senderRole == "ADMIN") "IN_PROGRESS" else "OPEN"
        )
        dao.updateTicket(updatedTicket)
    }

    suspend fun resolveSupportTicket(ticketId: Int) {
        val ticket = dao.getTicketById(ticketId) ?: return
        val updated = ticket.copy(status = "RESOLVED")
        dao.updateTicket(updated)
    }

    // --- EXCEL/CSV EXPORT GENERATOR ---
    // Returns full multi-level CSV data string model reflecting what can be imported to Excel
    fun generateUsersCsv(users: List<User>): String {
        val header = "User ID (Phone),Name,Email,WhatsApp,Referral Code,Referred By,Wallet Balance (NPR),Commissions Earned (NPR),Status,Role,Registration Date\n"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val body = users.joinToString("\n") { u ->
            val dateStr = df.format(Date(u.registrationDate))
            "\"${u.userId}\",\"${u.name}\",\"${u.email}\",\"${u.whatsapp}\",\"${u.referralCode}\",\"${u.referredBy ?: "None"}\",${u.walletBalance},${u.commissionsEarned},\"${u.status}\",\"${u.role}\",\"$dateStr\""
        }
        return header + body
    }

    fun generateDepositsCsv(deposits: List<Deposit>): String {
        val header = "Deposit ID,User ID,User Name,Amount (NPR),Payment Method,Transaction ID,Status,Timestamp\n"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val body = deposits.joinToString("\n") { d ->
            val dateStr = df.format(Date(d.timestamp))
            "${d.id},\"${d.userId}\",\"${d.userName}\",${d.amount},\"${d.paymentMethod}\",\"${d.transactionId}\",\"${d.status}\",\"$dateStr\""
        }
        return header + body
    }

    fun generateWithdrawalsCsv(withdrawals: List<Withdrawal>): String {
        val header = "Withdrawal ID,User ID,User Name,Amount (NPR),Payment Method,Account/Wallet Details,Status,Risk Score (0-100),Timestamp\n"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val body = withdrawals.joinToString("\n") { w ->
            val dateStr = df.format(Date(w.timestamp))
            "${w.id},\"${w.userId}\",\"${w.userName}\",${w.amount},\"${w.paymentMethod}\",\"${w.accountDetails}\",\"${w.status}\",${w.riskScore},\"$dateStr\""
        }
        return header + body
    }

    fun generateCommissionsCsv(txList: List<Transaction>): String {
        val commsList = txList.filter { it.type == "DIRECT_COMMISSION" || it.type == "INDIRECT_COMMISSION" }
        val header = "Log ID,User ID,Amount (NPR),Commission Type,Description,Timestamp\n"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
        val body = commsList.joinToString("\n") { c ->
            val dateStr = df.format(Date(c.timestamp))
            "${c.id},\"${c.userId}\",${c.amount},\"${c.type}\",\"${c.description}\",\"$dateStr\""
        }
        return header + body
    }
}
