package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(database)

    // Preferences store for app-wide settings that aren't per-category (e.g. overall monthly budget cap)
    private val appPrefs = application.getSharedPreferences("hebrew_budget_prefs", Context.MODE_PRIVATE)

    // Overall monthly budget cap (0.0 means no cap is defined), independent of per-category limits
    private val _monthlyBudgetLimit = MutableStateFlow(appPrefs.getFloat(KEY_MONTHLY_BUDGET_LIMIT, 0f).toDouble())
    val monthlyBudgetLimit = _monthlyBudgetLimit.asStateFlow()

    // Exposed lists
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected Hebrew month and year for filtering
    private val _selectedHebrewMonthIndex = MutableStateFlow(0)
    val selectedHebrewMonthIndex = _selectedHebrewMonthIndex.asStateFlow()

    private val _selectedHebrewMonthName = MutableStateFlow("")
    val selectedHebrewMonthName = _selectedHebrewMonthName.asStateFlow()

    private val _selectedHebrewYear = MutableStateFlow(0)
    val selectedHebrewYear = _selectedHebrewYear.asStateFlow()

    private val _selectedHebrewYearString = MutableStateFlow("")
    val selectedHebrewYearString = _selectedHebrewYearString.asStateFlow()

    // Gemini Parsing State
    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _parseError = MutableStateFlow<String?>(null)
    val parseError = _parseError.asStateFlow()

    private val _parsedDraft = MutableStateFlow<ParsedTransaction?>(null)
    val parsedDraft = _parsedDraft.asStateFlow()

    init {
        // Run pre-population of default categories
        viewModelScope.launch {
            repository.checkAndPrepopulateCategories()
            
            // Set default selected month to current Hebrew month
            val currentHebrew = HebrewCalendarHelper.getHebrewDateInfo(System.currentTimeMillis())
            _selectedHebrewMonthIndex.value = currentHebrew.monthIndex
            _selectedHebrewMonthName.value = currentHebrew.monthName
            _selectedHebrewYear.value = currentHebrew.year
            _selectedHebrewYearString.value = currentHebrew.yearHebrewString
        }
    }

    // Filtered transactions for the selected Hebrew month & year
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        _selectedHebrewMonthIndex,
        _selectedHebrewYear
    ) { transactions, monthIndex, year ->
        transactions.filter {
            it.hebrewMonthIndex == monthIndex && it.hebrewYear == year
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of available months (Hebrew month + year) across all transactions, plus current month
    val availableMonths: StateFlow<List<HebrewMonthYearOption>> = allTransactions.map { list ->
        val current = HebrewCalendarHelper.getHebrewDateInfo(System.currentTimeMillis())
        val currentOption = HebrewMonthYearOption(
            monthIndex = current.monthIndex,
            monthName = current.monthName,
            year = current.year,
            yearString = current.yearHebrewString
        )
        
        val optionsFromTx = list.map {
            HebrewMonthYearOption(
                monthIndex = it.hebrewMonthIndex,
                monthName = it.hebrewMonthName,
                year = it.hebrewYear,
                yearString = it.hebrewYearString
            )
        }.distinct()

        (optionsFromTx + currentOption).distinctBy { "${it.year}_${it.monthIndex}" }
            .sortedWith(compareBy<HebrewMonthYearOption> { it.year }.thenBy { it.monthIndex })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations for the current selected month
    val monthlyStats: StateFlow<MonthlyStats> = filteredTransactions.map { list ->
        var totalIncome = 0.0
        var totalExpense = 0.0
        var cashExpense = 0.0
        var creditExpense = 0.0
        var cashIncome = 0.0
        var creditIncome = 0.0

        for (tx in list) {
            val amount = tx.amount
            if (tx.isExpense) {
                totalExpense += amount
                if (tx.paymentType == "CASH") cashExpense += amount else creditExpense += amount
            } else {
                totalIncome += amount
                if (tx.paymentType == "CASH") cashIncome += amount else creditIncome += amount
            }
        }

        MonthlyStats(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            cashExpense = cashExpense,
            creditExpense = creditExpense,
            cashIncome = cashIncome,
            creditIncome = creditIncome,
            netBalance = totalIncome - totalExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats())

    // Select different month
    fun selectMonth(option: HebrewMonthYearOption) {
        _selectedHebrewMonthIndex.value = option.monthIndex
        _selectedHebrewMonthName.value = option.monthName
        _selectedHebrewYear.value = option.year
        _selectedHebrewYearString.value = option.yearString
    }

    // Add category
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name.trim()))
        }
    }

    // Remove category
    fun removeCategory(category: CategoryEntity) {
        if (category.isSystem) return // system categories cannot be deleted
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Update category budget
    fun updateCategoryBudget(category: CategoryEntity, limit: Double) {
        viewModelScope.launch {
            repository.updateCategoryBudget(category.id, limit)
        }
    }

    // Update the overall monthly budget cap (applies across all categories combined)
    fun setMonthlyBudgetLimit(limit: Double) {
        val safeLimit = if (limit < 0.0) 0.0 else limit
        _monthlyBudgetLimit.value = safeLimit
        appPrefs.edit().putFloat(KEY_MONTHLY_BUDGET_LIMIT, safeLimit.toFloat()).apply()
    }

    // Insert transaction
    fun addManualTransaction(
        title: String,
        amount: Double,
        isExpense: Boolean,
        categoryName: String,
        paymentType: String, // "CASH" or "CREDIT"
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            val hebrewInfo = HebrewCalendarHelper.getHebrewDateInfo(timestamp)

            // Check budget limits before saving the transaction
            if (isExpense) {
                val category = categories.value.find { it.name == categoryName }
                val limit = category?.budgetLimit ?: 0.0
                if (limit > 0.0) {
                    val currentCategoryTotal = allTransactions.value
                        .filter { it.isExpense && it.categoryName == categoryName && it.hebrewMonthIndex == hebrewInfo.monthIndex && it.hebrewYear == hebrewInfo.year }
                        .sumOf { it.amount }
                    val newTotal = currentCategoryTotal + amount
                    val pBefore = currentCategoryTotal / limit
                    val pAfter = newTotal / limit

                    if (pBefore < 0.8 && pAfter >= 0.8 && pAfter < 1.0) {
                        sendLocalNotification(
                            "התראת תקציב - 80%",
                            "ההוצאות בקטגוריה '$categoryName' הגיעו ל-80% מהתקציב (₪${String.format("%.2f", newTotal)} מתוך ₪${String.format("%.2f", limit)})"
                        )
                    } else if (pBefore < 1.0 && pAfter >= 1.0) {
                        sendLocalNotification(
                            "חריגה מהתקציב!",
                            "ההוצאות בקטגוריה '$categoryName' עברו את התקציב שהוגדר! (₪${String.format("%.2f", newTotal)} מתוך ₪${String.format("%.2f", limit)})"
                        )
                    }
                }

                // Check overall monthly budget cap (across all categories combined)
                val overallLimit = _monthlyBudgetLimit.value
                if (overallLimit > 0.0) {
                    val currentMonthTotal = allTransactions.value
                        .filter { it.isExpense && it.hebrewMonthIndex == hebrewInfo.monthIndex && it.hebrewYear == hebrewInfo.year }
                        .sumOf { it.amount }
                    val newMonthTotal = currentMonthTotal + amount
                    val pBeforeOverall = currentMonthTotal / overallLimit
                    val pAfterOverall = newMonthTotal / overallLimit

                    if (pBeforeOverall < 0.8 && pAfterOverall >= 0.8 && pAfterOverall < 1.0) {
                        sendLocalNotification(
                            "התראת תקציב חודשי כולל - 80%",
                            "סך כל ההוצאות החודש הגיעו ל-80% מהתקציב הכולל (₪${String.format("%.2f", newMonthTotal)} מתוך ₪${String.format("%.2f", overallLimit)})"
                        )
                    } else if (pBeforeOverall < 1.0 && pAfterOverall >= 1.0) {
                        sendLocalNotification(
                            "חריגה מהתקציב החודשי הכולל!",
                            "סך כל ההוצאות החודש עברו את התקציב הכולל שהוגדר! (₪${String.format("%.2f", newMonthTotal)} מתוך ₪${String.format("%.2f", overallLimit)})"
                        )
                    }
                }
            }

            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                isExpense = isExpense,
                categoryName = categoryName,
                paymentType = paymentType,
                timestamp = timestamp,
                hebrewDay = hebrewInfo.day,
                hebrewMonthIndex = hebrewInfo.monthIndex,
                hebrewMonthName = hebrewInfo.monthName,
                hebrewYear = hebrewInfo.year,
                hebrewYearString = hebrewInfo.yearHebrewString
            )
            repository.insertTransaction(entity)
        }
    }

    private fun sendLocalNotification(title: String, message: String) {
        try {
            val context = getApplication<Application>().applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channelId = "budget_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "התראות תקציב",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "התראות כאשר ההוצאות מגיעות ל-80% או עוברות את התקציב החודשי"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Delete transaction
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Ask Gemini to parse expense description
    fun parseDescriptionWithGemini(description: String) {
        if (description.isBlank()) return
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            _parseError.value = "מפתח ה-API של Gemini חסר. אנא הגדר אותו בפאנל ה-Secrets ב-AI Studio."
            return
        }

        viewModelScope.launch {
            _isParsing.value = true
            _parseError.value = null
            _parsedDraft.value = null

            val currentCategoryNames = categories.value.map { it.name }
            val parsedResult = GeminiExpenseParser.parseExpense(
                prompt = description,
                categories = currentCategoryNames,
                apiKey = apiKey
            )

            _isParsing.value = false
            if (parsedResult != null) {
                _parsedDraft.value = parsedResult
            } else {
                _parseError.value = "לא הצלחנו לפענח את המשפט. נסה לנסח אחרת או להוסיף ידנית."
            }
        }
    }

    // Confirm and save Gemini drafted transaction
    fun confirmDraftTransaction(categoryName: String? = null) {
        val draft = _parsedDraft.value ?: return
        addManualTransaction(
            title = draft.title,
            amount = draft.amount,
            isExpense = draft.isExpense,
            categoryName = categoryName ?: draft.categoryName,
            paymentType = draft.paymentType,
            timestamp = System.currentTimeMillis()
        )
        _parsedDraft.value = null
    }

    // Clear Gemini states
    fun clearDraft() {
        _parsedDraft.value = null
        _parseError.value = null
    }

    companion object {
        private const val KEY_MONTHLY_BUDGET_LIMIT = "monthly_budget_limit"
    }
}

// Helper structures
data class HebrewMonthYearOption(
    val monthIndex: Int,
    val monthName: String,
    val year: Int,
    val yearString: String
) {
    fun getDisplayName(): String = "$monthName $yearString"
}

data class MonthlyStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val cashExpense: Double = 0.0,
    val creditExpense: Double = 0.0,
    val cashIncome: Double = 0.0,
    val creditIncome: Double = 0.0,
    val netBalance: Double = 0.0
)
