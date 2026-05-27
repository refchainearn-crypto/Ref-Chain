package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String, // e.g. Phone number or uuid
    val name: String,
    val email: String,
    val phone: String,
    val whatsapp: String,
    val passwordHash: String,
    val referralCode: String,
    val referredBy: String?, // The referral code of the inviter
    val walletBalance: Double = 0.0,
    val commissionsEarned: Double = 0.0,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val role: String = "USER", // USER, ADMIN
    val registrationDate: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "deposits")
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val amount: Double,
    val paymentMethod: String, // Bank, eSewa, Khalti, Binance
    val transactionId: String,
    val screenshotUri: String?, // path or placeholder
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "withdrawals")
data class Withdrawal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val amount: Double,
    val paymentMethod: String, // Bank, eSewa, Khalti, Binance
    val accountDetails: String, // destination number/address
    val status: String = "PENDING", // PENDING, PROCESSING, APPROVED, REJECTED, COMPLETED
    val riskScore: Int = 0, // 0 to 100 based on suspicious checks
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val amount: Double,
    val type: String, // DEPOSIT, WITHDRAWAL, DIRECT_COMMISSION, INDIRECT_COMMISSION, ADMIN_ADJUST
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val subject: String,
    val issueType: String, // Financial, Tech, Referral, Other
    val priority: String, // HIGH, MEDIUM, LOW
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED
    val messagesStr: String, // JSON or formatted text representation of chat
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "system_configs")
data class SystemConfig(
    @PrimaryKey val id: String = "DEFAULT_CONFIG",
    val minDepositAmount: Double = 500.0, // NPR 500 default
    val minWithdrawalAmount: Double = 200.0,
    val directReferralPercentage: Double = 40.0, // 40%
    val indirectReferralPercentage: Double = 10.0, // 10%
    val bankAccountName: String = "RefChain FinTech Corp",
    val bankAccountNumber: String = "982301823901",
    val bankName: String = "Nepal Rastra Bank",
    val eSewaMobile: String = "9801234567",
    val khaltiMobile: String = "9812345678",
    val binanceAddress: String = "0x7aB1...fDe9",
    val whatsappContactLink: String = "https://wa.me/9779801234567",
    val supportEmail: String = "refchain.earn@gmail.com",
    val supportPhone: String = "+977-9801234567"
) : Serializable

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val userName: String,
    val actionType: String, // SIGNUP, LOGIN, FRAUD_ALERT, DEPOSIT_PROOF_UPLOAD, SUSPENSION
    val details: String,
    val isAlert: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
