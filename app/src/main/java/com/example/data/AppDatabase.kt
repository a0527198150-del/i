package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isSystem: Boolean = false,
    val budgetLimit: Double = 0.0 // 0.0 means no budget is defined
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean = true,
    val categoryName: String,
    val paymentType: String, // "CASH" or "CREDIT"
    val timestamp: Long,
    val hebrewDay: Int,
    val hebrewMonthIndex: Int,
    val hebrewMonthName: String,
    val hebrewYear: Int,
    val hebrewYearString: String,
    val rawText: String? = null
)

// --- DAOs (Data Access Objects) ---

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("UPDATE categories SET budgetLimit = :limit WHERE id = :id")
    suspend fun updateCategoryBudget(id: Int, limit: Double)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE hebrewYear = :year AND hebrewMonthIndex = :monthIndex ORDER BY timestamp DESC")
    fun getTransactionsForHebrewMonth(year: Int, monthIndex: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}

// --- Room Database ---

@Database(entities = [CategoryEntity::class, TransactionEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hebrew_budget_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository ---

class BudgetRepository(private val db: AppDatabase) {
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategories()
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()

    fun getTransactionsForHebrewMonth(year: Int, monthIndex: Int): Flow<List<TransactionEntity>> {
        return db.transactionDao().getTransactionsForHebrewMonth(year, monthIndex)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        db.transactionDao().insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        db.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        db.transactionDao().deleteTransactionById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Boolean {
        val rowId = db.categoryDao().insertCategory(category)
        return rowId != -1L
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        db.categoryDao().deleteCategory(category)
    }

    suspend fun updateCategoryBudget(id: Int, limit: Double) {
        db.categoryDao().updateCategoryBudget(id, limit)
    }

    suspend fun checkAndPrepopulateCategories() {
        val defaultCategories = listOf(
            CategoryEntity(name = "אוכל קנוי בברכל", isSystem = true),
            CategoryEntity(name = "אוכל מוכן", isSystem = true),
            CategoryEntity(name = "סלולר", isSystem = true),
            CategoryEntity(name = "ביגוד", isSystem = true),
            CategoryEntity(name = "מגורים", isSystem = true),
            CategoryEntity(name = "בריאות", isSystem = true),
            CategoryEntity(name = "תחבורה", isSystem = true),
            CategoryEntity(name = "הכנסות", isSystem = true),
            CategoryEntity(name = "אחר", isSystem = true)
        )
        for (category in defaultCategories) {
            db.categoryDao().insertCategory(category)
        }
    }
}
