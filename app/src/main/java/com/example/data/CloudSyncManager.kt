package com.example.data

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Result of a backup/restore operation, used for UI feedback
data class BackupSummary(
    val categories: Int,
    val transactions: Int,
    val recurringRules: Int
)

/**
 * Backs up the local Room database (categories, transactions, recurring rules)
 * to the signed-in user's private Firestore area, and restores it from there.
 *
 * Layout under Firestore:
 *   users/{uid}/backup/categories/{id}       one doc per category
 *   users/{uid}/backup/transactions/{id}     one doc per transaction
 *   users/{uid}/backup/recurringRules/{id}   one doc per recurring rule
 *   users/{uid}/backup/meta                  { lastBackupAt, counts }
 */
class CloudSyncManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()

    private fun backupRef(uid: String) =
        firestore.collection("users").document(uid).collection("backup")

    suspend fun backup(uid: String): BackupSummary = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val categories = db.categoryDao().getAllCategories().first()
        val transactions = db.transactionDao().getAllTransactions().first()
        val rules = db.recurringRuleDao().getAllRecurringRules().first()

        replaceCollection(backupRef(uid).collection("categories"), categories.map { it.id.toString() to it.toMap() })
        replaceCollection(backupRef(uid).collection("transactions"), transactions.map { it.id.toString() to it.toMap() })
        replaceCollection(backupRef(uid).collection("recurringRules"), rules.map { it.id.toString() to it.toMap() })

        backupRef(uid).document("meta").set(
            mapOf(
                "lastBackupAt" to System.currentTimeMillis(),
                "categories" to categories.size,
                "transactions" to transactions.size,
                "recurringRules" to rules.size
            )
        ).await()

        BackupSummary(categories.size, transactions.size, rules.size)
    }

    suspend fun restore(uid: String): BackupSummary? = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)

        val categories = readCollection(backupRef(uid).collection("categories")) { it.toCategory() }
        val transactions = readCollection(backupRef(uid).collection("transactions")) { it.toTransaction() }
        val rules = readCollection(backupRef(uid).collection("recurringRules")) { it.toRecurringRule() }

        if (categories.isEmpty() && transactions.isEmpty() && rules.isEmpty()) {
            return@withContext null
        }

        db.clearAllTables()
        categories.forEach { db.categoryDao().insertCategory(it) }
        transactions.forEach { db.transactionDao().insertTransaction(it) }
        rules.forEach { db.recurringRuleDao().insertRecurringRule(it) }

        BackupSummary(categories.size, transactions.size, rules.size)
    }

    // Replaces the contents of a collection: deletes old docs then writes new ones, in batches
    private suspend fun replaceCollection(
        ref: com.google.firebase.firestore.CollectionReference,
        docs: List<Pair<String, Map<String, Any>>>
    ) {
        val existing = ref.get().await()
        existing.documents.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        docs.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { (id, data) -> batch.set(ref.document(id), data) }
            batch.commit().await()
        }
    }

    private suspend fun <T> readCollection(
        ref: com.google.firebase.firestore.CollectionReference,
        convert: (DocumentSnapshot) -> T?
    ): List<T> {
        val snap = ref.get().await()
        return snap.documents.mapNotNull { convert(it) }
    }
}

// --- Entity <-> Firestore map conversions ---

private fun CategoryEntity.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "name" to name,
    "isSystem" to isSystem,
    "budgetLimit" to budgetLimit
)

private fun TransactionEntity.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "amount" to amount,
    "isExpense" to isExpense,
    "categoryName" to categoryName,
    "paymentType" to paymentType,
    "timestamp" to timestamp,
    "hebrewDay" to hebrewDay,
    "hebrewMonthIndex" to hebrewMonthIndex,
    "hebrewMonthName" to hebrewMonthName,
    "hebrewYear" to hebrewYear,
    "hebrewYearString" to hebrewYearString,
    "rawText" to (rawText ?: ""),
    "isAnomalous" to isAnomalous
)

private fun RecurringRuleEntity.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "title" to title,
    "amount" to amount,
    "isExpense" to isExpense,
    "categoryName" to categoryName,
    "paymentType" to paymentType,
    "dayOfMonth" to dayOfMonth,
    "isActive" to isActive,
    "lastGeneratedPeriodKey" to (lastGeneratedPeriodKey ?: ""),
    "reminderEnabled" to reminderEnabled
)

private fun DocumentSnapshot.toCategory(): CategoryEntity? {
    val id = getLong("id")?.toInt() ?: return null
    val name = getString("name") ?: return null
    return CategoryEntity(
        id = id,
        name = name,
        isSystem = getBoolean("isSystem") ?: false,
        budgetLimit = getDouble("budgetLimit") ?: 0.0
    )
}

private fun DocumentSnapshot.toTransaction(): TransactionEntity? {
    val id = getLong("id")?.toInt() ?: return null
    val title = getString("title") ?: return null
    return TransactionEntity(
        id = id,
        title = title,
        amount = getDouble("amount") ?: 0.0,
        isExpense = getBoolean("isExpense") ?: true,
        categoryName = getString("categoryName") ?: "",
        paymentType = getString("paymentType") ?: "CASH",
        timestamp = getLong("timestamp") ?: 0L,
        hebrewDay = (getLong("hebrewDay") ?: 0L).toInt(),
        hebrewMonthIndex = (getLong("hebrewMonthIndex") ?: 0L).toInt(),
        hebrewMonthName = getString("hebrewMonthName") ?: "",
        hebrewYear = (getLong("hebrewYear") ?: 0L).toInt(),
        hebrewYearString = getString("hebrewYearString") ?: "",
        rawText = getString("rawText"),
        isAnomalous = getBoolean("isAnomalous") ?: false
    )
}

private fun DocumentSnapshot.toRecurringRule(): RecurringRuleEntity? {
    val id = getLong("id")?.toInt() ?: return null
    return RecurringRuleEntity(
        id = id,
        title = getString("title") ?: "",
        amount = getDouble("amount") ?: 0.0,
        isExpense = getBoolean("isExpense") ?: true,
        categoryName = getString("categoryName") ?: "",
        paymentType = getString("paymentType") ?: "CASH",
        dayOfMonth = (getLong("dayOfMonth") ?: 1L).toInt(),
        isActive = getBoolean("isActive") ?: true,
        lastGeneratedPeriodKey = getString("lastGeneratedPeriodKey"),
        reminderEnabled = getBoolean("reminderEnabled") ?: true
    )
}
