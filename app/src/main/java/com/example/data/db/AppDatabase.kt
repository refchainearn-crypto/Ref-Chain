package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RefChainDao {
    // --- USERS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): User?

    @Query("SELECT * FROM users WHERE referredBy = :referredByCode")
    suspend fun getUsersByReferredBy(referredByCode: String): List<User>

    @Query("SELECT * FROM users ORDER BY registrationDate DESC")
    fun getAllAsFlow(): Flow<List<User>>

    @Query("SELECT * FROM users ORDER BY registrationDate DESC")
    suspend fun getAllUsers(): List<User>


    // --- CONFIGS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SystemConfig)

    @Query("SELECT * FROM system_configs WHERE id = 'DEFAULT_CONFIG' LIMIT 1")
    fun getConfigFlow(): Flow<SystemConfig?>

    @Query("SELECT * FROM system_configs WHERE id = 'DEFAULT_CONFIG' LIMIT 1")
    suspend fun getConfig(): SystemConfig?


    // --- DEPOSITS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: Deposit)

    @Update
    suspend fun updateDeposit(deposit: Deposit)

    @Query("SELECT * FROM deposits ORDER BY timestamp DESC")
    fun getAllDepositsFlow(): Flow<List<Deposit>>

    @Query("SELECT * FROM deposits ORDER BY timestamp DESC")
    suspend fun getAllDeposits(): List<Deposit>

    @Query("SELECT * FROM deposits WHERE id = :id LIMIT 1")
    suspend fun getDepositById(id: Int): Deposit?

    @Query("SELECT * FROM deposits WHERE userId = :userId ORDER BY timestamp DESC")
    fun getDepositsByUserId(userId: String): Flow<List<Deposit>>


    // --- WITHDRAWALS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: Withdrawal)

    @Update
    suspend fun updateWithdrawal(withdrawal: Withdrawal)

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    fun getAllWithdrawalsFlow(): Flow<List<Withdrawal>>

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    suspend fun getAllWithdrawals(): List<Withdrawal>

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: Int): Withdrawal?

    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWithdrawalsByUserId(userId: String): Flow<List<Withdrawal>>


    // --- TRANSACTIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUserId(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<Transaction>


    // --- TICKETS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket)

    @Update
    suspend fun updateTicket(ticket: SupportTicket)

    @Query("SELECT * FROM support_tickets ORDER BY timestamp DESC")
    fun getAllTicketsFlow(): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTicketsByUserId(userId: String): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: Int): SupportTicket?


    // --- AUDIT LOGS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<AuditLog>
}

@Database(
    entities = [
        User::class,
        Deposit::class,
        Withdrawal::class,
        Transaction::class,
        SupportTicket::class,
        SystemConfig::class,
        AuditLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RefChainDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refchain_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Precompile default setup inside background thread
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaults(database.dao())
                    }
                }
            }
        }

        private suspend fun populateDefaults(dao: RefChainDao) {
            // 1. Initial configurations
            val config = SystemConfig()
            dao.insertConfig(config)

            // 2. Insert Admin User
            // Pre-register administrative credentials specified in environment (refchain.earn@gmail.com)
            val adminUser = User(
                userId = "rc_admin",
                name = "Fintech Board Controller",
                email = "refchain.earn@gmail.com",
                phone = "+977-9800000000",
                whatsapp = "+977-9800000000",
                passwordHash = "admin123", // Simple hash
                referralCode = "ADMIN_REFC",
                referredBy = null,
                walletBalance = 0.0,
                commissionsEarned = 0.0,
                status = "ACTIVE",
                role = "ADMIN"
            )
            dao.insertUser(adminUser)

            // 3. Referral Multi-level Chain setup
            // User A (Sujan)
            val userSujan = User(
                userId = "user_sujan",
                name = "Sujan Thapa",
                email = "sujan@gmail.com",
                phone = "+977-9841234567",
                whatsapp = "+977-9841234567",
                passwordHash = "user123",
                referralCode = "SUJAN40",
                referredBy = null,
                walletBalance = 0.0,
                commissionsEarned = 0.0,
                status = "ACTIVE",
                role = "USER"
            )
            dao.insertUser(userSujan)

            // User B (Pooja - referred by Sujan)
            val userPooja = User(
                userId = "user_pooja",
                name = "Pooja Shrestha",
                email = "pooja@gmail.com",
                phone = "+977-9851234567",
                whatsapp = "+977-9851234567",
                passwordHash = "user123",
                referralCode = "POOJA10",
                referredBy = "SUJAN40",
                walletBalance = 0.0,
                commissionsEarned = 0.0,
                status = "ACTIVE",
                role = "USER"
            )
            dao.insertUser(userPooja)

            // User C (Rohan - referred by Pooja)
            val userRohan = User(
                userId = "user_rohan",
                name = "Rohan Adhikari",
                email = "rohan@gmail.com",
                phone = "+977-9861234568",
                whatsapp = "+977-9861234568",
                passwordHash = "user123",
                referralCode = "ROHANFF",
                referredBy = "POOJA10",
                walletBalance = 0.0,
                commissionsEarned = 0.0,
                status = "ACTIVE",
                role = "USER"
            )
            dao.insertUser(userRohan)

            // 4. No historical approved deposits or commissions in starting state
            // Everything begins completely clean.

            // Add an audit log alert
            dao.insertAuditLog(AuditLog(
                userId = "user_rohan",
                userName = "Rohan Adhikari",
                actionType = "LOGIN",
                details = "Simulated registration complete. System set to zero starting balance.",
                isAlert = false
            ))

            dao.insertAuditLog(AuditLog(
                userId = "unknown_suspicious",
                userName = "Guest Target",
                actionType = "FRAUD_ALERT",
                details = "Multi-device registration detected under identical IP hash (Potential Self-Referral Hook)",
                isAlert = true
            ))

            // Create default support ticket
            val messages = """
                [
                  {"sender":"USER", "text":"Hi, I uploaded my eSewa receipt but my balance is not premium updated yet. Please approve quickly.", "time": 1779951000000},
                  {"sender":"ADMIN", "text":"We are reviewing your transaction ID TX-8921-SUJAN now. One moment.", "time": 1779951500000}
                ]
            """.trimIndent()
            dao.insertTicket(SupportTicket(
                userId = "user_sujan",
                userName = "Sujan Thapa",
                subject = "Deposit verification lag",
                issueType = "Financial",
                priority = "HIGH",
                status = "IN_PROGRESS",
                messagesStr = messages
            ))
        }
    }
}
