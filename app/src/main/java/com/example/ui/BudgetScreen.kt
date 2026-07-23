package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.ParsedTransaction
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    // Force RTL direction for Hebrew app
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val categories by viewModel.categories.collectAsStateWithLifecycle()
        val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
        val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()
        val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
        val monthlyBudgetLimit by viewModel.monthlyBudgetLimit.collectAsStateWithLifecycle()
        val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

        val selectedMonthIndex by viewModel.selectedHebrewMonthIndex.collectAsStateWithLifecycle()
        val selectedMonthName by viewModel.selectedHebrewMonthName.collectAsStateWithLifecycle()
        val selectedYear by viewModel.selectedHebrewYear.collectAsStateWithLifecycle()
        val selectedYearString by viewModel.selectedHebrewYearString.collectAsStateWithLifecycle()

        val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
        val parseError by viewModel.parseError.collectAsStateWithLifecycle()
        val parsedDraft by viewModel.parsedDraft.collectAsStateWithLifecycle()

        // UI states
        var currentTab by remember { mutableStateOf(0) } // 0 = Detailed List, 1 = By Categories
        var showManualAddDialog by remember { mutableStateOf(false) }
        var showCategoryManagerDialog by remember { mutableStateOf(false) }
        var showMonthSelector by remember { mutableStateOf(false) }
        var geminiInputText by remember { mutableStateOf("") }
        var budgetToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
        var showMonthlyBudgetDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "תקציב עברי חכם",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF001E30)
                            )
                            Text(
                                text = "$selectedMonthName $selectedYearString",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF006494)
                            )
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showMonthlyBudgetDialog = true },
                                modifier = Modifier.testTag("monthly_budget_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = "הגדרת תקציב חודשי כולל",
                                    tint = Color(0xFF001E30)
                                )
                            }
                            IconButton(
                                onClick = { showCategoryManagerDialog = true },
                                modifier = Modifier.testTag("manage_categories_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "ניהול קטגוריות",
                                    tint = Color(0xFF001E30)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFD1E4FF), shape = RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "יה",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001D36),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDFBFF)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showManualAddDialog = true },
                    containerColor = Color(0xFF001D36),
                    contentColor = Color.White,
                    modifier = Modifier
                        .testTag("add_transaction_fab")
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "הוספת תנועה")
                        Text("הוספה ידנית", fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = Color(0xFFFDFBFF),
            modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFFDFBFF)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hebrew Month Selector Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEEF0F8)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "חודש פעיל (לפי הלוח העברי)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF44474E)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { showMonthSelector = true }
                                        .testTag("month_select_trigger")
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$selectedMonthName $selectedYearString",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF001D36)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "בחר חודש",
                                        tint = Color(0xFF001D36)
                                    )
                                }
                            }

                            // Quick current date info
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE1E2EC))
                            ) {
                                Text(
                                    text = "מתחיל בא'",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001D36)
                                )
                            }
                        }
                    }
                }

                // 2. Budget Statistics Dashboard
                item {
                    BudgetDashboardCard(stats = monthlyStats, monthlyBudgetLimit = monthlyBudgetLimit)
                }

                // 3. Gemini Input Section
                item {
                    GeminiInputCard(
                        inputText = geminiInputText,
                        onInputTextChange = { geminiInputText = it },
                        isParsing = isParsing,
                        parseError = parseError,
                        onParseClick = {
                            viewModel.parseDescriptionWithGemini(geminiInputText)
                            geminiInputText = "" // clear input on send
                        }
                    )
                }

                // 4. Tab Navigation (Detailed vs Category Split vs Charts)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tab 0 pill ("פירוט הכל")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .background(
                                    color = if (currentTab == 0) Color(0xFFE1E2EC) else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .border(
                                    width = if (currentTab == 0) 0.dp else 1.dp,
                                    color = if (currentTab == 0) Color.Transparent else Color(0xFF74777F),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { currentTab = 0 }
                                .testTag("tab_all_detailed"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "תנועות",
                                fontWeight = if (currentTab == 0) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (currentTab == 0) Color(0xFF191C1E) else Color(0xFF44474E),
                                fontSize = 13.sp
                            )
                        }

                        // Tab 1 pill ("תקציב וקטגוריות")
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(42.dp)
                                .background(
                                    color = if (currentTab == 1) Color(0xFFE1E2EC) else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .border(
                                    width = if (currentTab == 1) 0.dp else 1.dp,
                                    color = if (currentTab == 1) Color.Transparent else Color(0xFF74777F),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { currentTab = 1 }
                                .testTag("tab_by_categories"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "תקציב וקטגוריות",
                                fontWeight = if (currentTab == 1) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (currentTab == 1) Color(0xFF191C1E) else Color(0xFF44474E),
                                fontSize = 13.sp
                            )
                        }

                        // Tab 2 pill ("תרשימים")
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .background(
                                    color = if (currentTab == 2) Color(0xFFE1E2EC) else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .border(
                                    width = if (currentTab == 2) 0.dp else 1.dp,
                                    color = if (currentTab == 2) Color.Transparent else Color(0xFF74777F),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { currentTab = 2 }
                                .testTag("tab_charts"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "תרשימים",
                                fontWeight = if (currentTab == 2) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (currentTab == 2) Color(0xFF191C1E) else Color(0xFF44474E),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // 5. Dynamic Tab Content
                if (currentTab == 0) {
                    // TAB 0: Detailed List
                    if (filteredTransactions.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(text = "אין תנועות בחודש זה עדיין. תאר לג'מיני מה קנית או הוסף ידנית!")
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                onDeleteClick = { viewModel.deleteTransaction(tx) }
                            )
                        }
                    }
                } else if (currentTab == 1) {
                    // TAB 1: Grouped By Categories & Budgets
                    val grouped = filteredTransactions.groupBy { it.categoryName }
                    
                    if (categories.isEmpty()) {
                        item {
                            EmptyStatePlaceholder(text = "אין קטגוריות מוגדרות.")
                        }
                    } else {
                        items(categories, key = { it.id }) { category ->
                            val categoryTransactions = grouped[category.name] ?: emptyList()
                            val totalCategoryExpense = categoryTransactions.filter { it.isExpense }.sumOf { it.amount }
                            val totalCategoryIncome = categoryTransactions.filter { !it.isExpense }.sumOf { it.amount }

                            CategoryGroupCard(
                                category = category,
                                transactions = categoryTransactions,
                                totalExpense = totalCategoryExpense,
                                totalIncome = totalCategoryIncome,
                                onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                onEditBudget = { budgetToEdit = it }
                            )
                        }
                    }
                } else {
                    // TAB 2: Graphical Analysis & Charts
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DonutChartCard(
                                categories = categories,
                                transactions = filteredTransactions
                            )
                            
                            BarChartCard(
                                allTransactions = allTransactions
                            )
                        }
                    }
                }
                
                // Add a spacer at the end for floating button overlap
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }

        // --- Dialogs ---

        // A. Month Selector Dialog
        if (showMonthSelector) {
            MonthSelectorDialog(
                options = availableMonths,
                selectedMonthIndex = selectedMonthIndex,
                selectedYear = selectedYear,
                onSelect = { option ->
                    viewModel.selectMonth(option)
                    showMonthSelector = false
                },
                onDismiss = { showMonthSelector = false }
            )
        }

        // B. Gemini Draft Confirmation Dialog
        parsedDraft?.let { draft ->
            GeminiDraftConfirmDialog(
                draft = draft,
                availableCategories = categories,
                onConfirm = { finalCategory ->
                    viewModel.confirmDraftTransaction(finalCategory)
                },
                onDismiss = {
                    viewModel.clearDraft()
                }
            )
        }

        // C. Manual Add Dialog
        if (showManualAddDialog) {
            ManualAddTransactionDialog(
                categories = categories,
                onAdd = { title, amount, isExpense, category, paymentType, timestamp ->
                    viewModel.addManualTransaction(
                        title = title,
                        amount = amount,
                        isExpense = isExpense,
                        categoryName = category,
                        paymentType = paymentType,
                        timestamp = timestamp
                    )
                    showManualAddDialog = false
                },
                onDismiss = { showManualAddDialog = false }
            )
        }

        // D. Category Manager Dialog
        if (showCategoryManagerDialog) {
            CategoryManagerDialog(
                categories = categories,
                onAdd = { viewModel.addCategory(it) },
                onDelete = { viewModel.removeCategory(it) },
                onDismiss = { showCategoryManagerDialog = false }
            )
        }

        // E. Edit Category Budget Dialog
        budgetToEdit?.let { category ->
            EditCategoryBudgetDialog(
                category = category,
                onDismiss = { budgetToEdit = null },
                onSave = { limit ->
                    viewModel.updateCategoryBudget(category, limit)
                    budgetToEdit = null
                }
            )
        }

        // F. Edit Overall Monthly Budget Dialog (cap across all categories combined)
        if (showMonthlyBudgetDialog) {
            EditMonthlyBudgetDialog(
                currentLimit = monthlyBudgetLimit,
                onDismiss = { showMonthlyBudgetDialog = false },
                onSave = { limit ->
                    viewModel.setMonthlyBudgetLimit(limit)
                    showMonthlyBudgetDialog = false
                }
            )
        }
    }
}

// --- Sub-Composables ---

@Composable
fun BudgetDashboardCard(
    stats: MonthlyStats,
    monthlyBudgetLimit: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val decFormat = remember { DecimalFormat("#,##0.00") }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E4FF)),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Balance row header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "יתרה חודשית עברית",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF001D36)
                )
                Surface(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "חודש עברי",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )
                }
            }

            // Big balance display
            val balanceSign = if (stats.netBalance >= 0) "" else "-"
            val absoluteBalance = kotlin.math.abs(stats.netBalance)
            Text(
                text = "${balanceSign}₪${decFormat.format(absoluteBalance)}",
                fontSize = 38.sp,
                fontWeight = FontWeight.Light,
                color = Color(0xFF001D36),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Total Income / Expense side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Income Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFE8F5E9).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "סה\"כ הכנסות 📈",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₪${decFormat.format(stats.totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                // Total Expense Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFFFEBEE).copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "סה\"כ הוצאות 📉",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₪${decFormat.format(stats.totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF001D36).copy(alpha = 0.1f))

            // Income / Expense summary split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cash details
                val netCash = stats.cashIncome - stats.cashExpense
                val cashSign = if (netCash >= 0) "" else "-"
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "מזומן",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36).copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${cashSign}₪${decFormat.format(kotlin.math.abs(netCash))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )
                }

                // Vertical border divider
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0xFF001D36).copy(alpha = 0.1f))
                        .align(Alignment.CenterVertically)
                )

                // Credit details
                val netCredit = stats.creditIncome - stats.creditExpense
                val creditSign = if (netCredit >= 0) "" else "-"
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = "אשראי",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36).copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${creditSign}₪${decFormat.format(kotlin.math.abs(netCredit))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )
                }
            }

            // Overall monthly budget cap progress (across all categories combined)
            if (monthlyBudgetLimit > 0.0) {
                HorizontalDivider(color = Color(0xFF001D36).copy(alpha = 0.1f))

                val usageFraction = (stats.totalExpense / monthlyBudgetLimit).toFloat().coerceIn(0f, 1f)
                val isOverBudget = stats.totalExpense >= monthlyBudgetLimit
                val isNearBudget = stats.totalExpense >= monthlyBudgetLimit * 0.8

                val progressColor = when {
                    isOverBudget -> Color(0xFFBA1A1A)
                    isNearBudget -> Color(0xFFE65100)
                    else -> Color(0xFF006494)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "תקציב חודשי כולל",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36).copy(alpha = 0.7f)
                        )
                        Text(
                            text = "₪${decFormat.format(stats.totalExpense)} מתוך ₪${decFormat.format(monthlyBudgetLimit)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { usageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = progressColor,
                        trackColor = Color.White.copy(alpha = 0.6f)
                    )
                    if (isOverBudget) {
                        Text(
                            text = "חריגה מהתקציב החודשי הכולל",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA1A1A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiInputCard(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isParsing: Boolean,
    parseError: String?,
    onParseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF0F8)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "ג'מיני פיענוח חכם",
                    tint = Color(0xFF001D36)
                )
                Text(
                    text = "דבר אל ג'מיני בחופשיות ✦",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF001D36)
                )
            }

            Text(
                text = "ספר לג'מיני מה קנית או קיבלת, והוא ימיין זאת מיד לקטגוריה, לאמצעי תשלום, ולסכום הנכון.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF44474E)
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        "לדוגמה: 'קניתי אוכל בחומוס אליהו ב-45 שקלים באשראי' או 'קיבלתי משכורת 5500 שקלים במזומן'",
                        fontSize = 12.sp,
                        color = Color(0xFF74777F)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("gemini_input_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color(0xFF191C1E),
                    unfocusedTextColor = Color(0xFF191C1E)
                )
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = onParseClick,
                    enabled = inputText.isNotBlank() && !isParsing,
                    modifier = Modifier.testTag("parse_gemini_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF001D36),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF001D36).copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("מפענח...")
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("שלח לג'מיני", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isParsing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF001D36)
                )
            }

            parseError?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decFormat = remember { DecimalFormat("#,##0.00") }

    // Map categories to emojis and background colors
    val (emoji, bg) = when (transaction.categoryName) {
        "אוכל מוכן" -> Pair("🥘", Color(0xFFFFDBCB))
        "אוכל קנוי בברכל" -> Pair("🛒", Color(0xFFE0E2EC))
        "סלולר" -> Pair("📱", Color(0xFFD8E2FF))
        "ביגוד" -> Pair("👔", Color(0xFFF6EDFF))
        "מגורים" -> Pair("🏠", Color(0xFFE0F2F1))
        "בריאות" -> Pair("💊", Color(0xFFFFCDD2))
        "תחבורה" -> Pair("🚗", Color(0xFFFFF9C4))
        "הכנסות" -> Pair("📈", Color(0xFFC8E6C9))
        else -> Pair("💰", Color(0xFFE8EAF6))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Emoji badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(bg, shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }

                Column {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                    
                    Text(
                        text = "${transaction.categoryName} • ${if (transaction.paymentType == "CASH") "מזומן" else "אשראי"} • יום ${transaction.hebrewDay} ב${transaction.hebrewMonthName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF44474E)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val amountColor = if (transaction.isExpense) Color(0xFFBA1A1A) else Color(0xFF2E7D32)
                val amountPrefix = if (transaction.isExpense) "-" else "+"
                
                Text(
                    text = "$amountPrefix₪${decFormat.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "מחק תנועה",
                        tint = Color(0xFFBA1A1A).copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGroupCard(
    category: CategoryEntity,
    transactions: List<TransactionEntity>,
    totalExpense: Double,
    totalIncome: Double,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditBudget: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val decFormat = remember { DecimalFormat("#,##0.00") }

    val emoji = when (category.name) {
        "אוכל מוכן" -> "🥘"
        "אוכל קנוי בברכל" -> "🛒"
        "סלולר" -> "📱"
        "ביגוד" -> "👔"
        "מגורים" -> "🏠"
        "בריאות" -> "💊"
        "תחבורה" -> "🚗"
        "הכנסות" -> "📈"
        else -> "💰"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEEF0F8), shape = RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 18.sp)
                    }

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                        Text(
                            text = "תנועות: ${transactions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF44474E)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (totalExpense > 0.0) {
                            Text(
                                text = "הוצאות: ₪${decFormat.format(totalExpense)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBA1A1A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (totalIncome > 0.0) {
                            Text(
                                text = "הכנסות: ₪${decFormat.format(totalIncome)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (totalExpense == 0.0 && totalIncome == 0.0) {
                            Text(
                                text = "₪0.00",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF44474E).copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF001D36)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FF))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Budget limit row
                    val budget = category.budgetLimit
                    if (budget > 0.0) {
                        val progress = (totalExpense / budget).coerceIn(0.0, 1.0)
                        val isOver80 = totalExpense >= budget * 0.8
                        val isOver100 = totalExpense >= budget
                        
                        val progressColor = when {
                            isOver100 -> Color(0xFFBA1A1A) // Red
                            isOver80 -> Color(0xFFD68A00) // Amber/Orange
                            else -> Color(0xFF006494) // Blue
                        }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFE1E2EC)), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (isOver100) Icons.Default.Warning else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = progressColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "תקציב חודשי: ₪${decFormat.format(budget)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF001D36)
                                    )
                                }
                                
                                Text(
                                    text = "${(progress * 100).toInt()}% נוצל",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = progressColor
                                )
                            }
                            
                            LinearProgressIndicator(
                                progress = progress.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = progressColor,
                                trackColor = Color(0xFFEEF0F8)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        isOver100 -> "חרגת מהתקציב ב-₪${decFormat.format(totalExpense - budget)}!"
                                        isOver80 -> "שים לב! הגעת ל-80% מהתקציב בקטגוריה זו."
                                        else -> "נשאר תקציב פנוי של ₪${decFormat.format(budget - totalExpense)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = progressColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                
                                Row(
                                    modifier = Modifier.clickable { onEditBudget(category) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "ערוך תקציב",
                                        tint = Color(0xFF006494),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "ערוך",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF006494),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Category has no budget set
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFE1E2EC)), RoundedCornerShape(12.dp))
                                .clickable { onEditBudget(category) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF006494),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "לא הוגדר תקציב חודשי לקטגוריה זו",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF44474E)
                                )
                            }
                            Text(
                                text = "הגדר תקציב",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF006494),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (transactions.isEmpty()) {
                        Text(
                            text = "אין הוצאות או הכנסות בקטגוריה זו בחודש הנבחר.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF44474E).copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        transactions.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFE1E2EC)),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Payment badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (tx.paymentType == "CASH") Color(0xFFE0F2F1) else Color(0xFFE8EAF6),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (tx.paymentType == "CASH") "מזומן" else "אשראי",
                                            fontSize = 10.sp,
                                            color = if (tx.paymentType == "CASH") Color(0xFF00796B) else Color(0xFF3F51B5),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF191C1E)
                                        )
                                        Text(
                                            text = "יום ${tx.hebrewDay} ב${tx.hebrewMonthName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF44474E)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val amountColor = if (tx.isExpense) Color(0xFFBA1A1A) else Color(0xFF2E7D32)
                                    Text(
                                        text = "${if (tx.isExpense) "-" else "+"}₪${decFormat.format(tx.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
                                    )

                                    IconButton(
                                        onClick = { onDeleteTransaction(tx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "מחק תנועה",
                                            tint = Color(0xFFBA1A1A).copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// --- Dialog Sub-Composables ---

@Composable
fun MonthSelectorDialog(
    options: List<HebrewMonthYearOption>,
    selectedMonthIndex: Int,
    selectedYear: Int,
    onSelect: (HebrewMonthYearOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "בחר חודש עברי",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFFE1E2EC))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    items(options) { option ->
                        val isSelected = option.monthIndex == selectedMonthIndex && option.year == selectedYear
                        val cardColors = if (isSelected) {
                            CardDefaults.cardColors(containerColor = Color(0xFFEEF0F8))
                        } else {
                            CardDefaults.cardColors(containerColor = Color.White)
                        }

                        Card(
                            colors = cardColors,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF001D36) else Color(0xFFE1E2EC)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.getDisplayName(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF001D36) else Color(0xFF191C1E)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "נבחר",
                                        tint = Color(0xFF001D36),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E))
                    ) {
                        Text("ביטול", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiDraftConfirmDialog(
    draft: ParsedTransaction,
    availableCategories: List<CategoryEntity>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(draft.categoryName) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val decFormat = remember { DecimalFormat("#,##0.00") }

    // Check if parsed category actually exists in database; if not, default to "אחר" or draft's proposed name
    LaunchedEffect(availableCategories, draft) {
        val matchesExisting = availableCategories.any { it.name == draft.categoryName }
        if (!matchesExisting && availableCategories.isNotEmpty()) {
            selectedCategory = availableCategories.firstOrNull { it.name == "אחר" }?.name ?: draft.categoryName
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF001D36)
                    )
                    Text(
                        text = "פיענוח ג'מיני מוכן! ✦",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF001D36)
                    )
                }

                HorizontalDivider(color = Color(0xFFE1E2EC))

                // Information Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Title
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("תיאור שנמצא:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF44474E))
                        Text(draft.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF191C1E))
                    }

                    // Amount
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("סכום:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF44474E))
                        Text("₪${decFormat.format(draft.amount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = if (draft.isExpense) Color(0xFFBA1A1A) else Color(0xFF2E7D32))
                    }

                    // Transaction Type
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("סוג פעולה:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF44474E))
                        Text(if (draft.isExpense) "הוצאה" else "הכנסה", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF191C1E))
                    }

                    // Payment Type
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("אמצעי תשלום:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF44474E))
                        Text(if (draft.paymentType == "CASH") "מזומן" else "אשראי", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF191C1E))
                    }

                    // Category Selection Grid
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("קטגוריה:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF44474E), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8F9FF), RoundedCornerShape(12.dp))
                                .padding(6.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableCategories) { category ->
                                    val isSelected = category.name == selectedCategory
                                    val emoji = when (category.name) {
                                        "אוכל מוכן" -> "🥘"
                                        "אוכל קנוי בברכל" -> "🛒"
                                        "סלולר" -> "📱"
                                        "ביגוד" -> "👔"
                                        "מגורים" -> "🏠"
                                        "בריאות" -> "💊"
                                        "תחבורה" -> "🚗"
                                        "הכנסות" -> "📈"
                                        else -> "💰"
                                    }
                                    val backgroundColor = if (isSelected) Color(0xFF001D36) else Color.White
                                    val contentColor = if (isSelected) Color.White else Color(0xFF001D36)
                                    
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCategory = category.name },
                                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE1E2EC))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 12.sp)
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = contentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E)),
                        modifier = Modifier.testTag("dismiss_draft_btn")
                    ) {
                        Text("ביטול", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedCategory)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001D36), contentColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("confirm_draft_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("אשר ושמור", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddTransactionDialog(
    categories: List<CategoryEntity>,
    onAdd: (title: String, amount: Double, isExpense: Boolean, category: String, paymentType: String, timestamp: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var paymentType by remember { mutableStateOf("CREDIT") } // "CREDIT" or "CASH"
    var selectedCategoryName by remember { mutableStateOf(categories.firstOrNull()?.name ?: "אחר") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "הוספת תנועה ידנית",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFFE1E2EC))

                // Custom Segmented Toggle for Expense/Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEF0F8), shape = RoundedCornerShape(50))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(
                                color = if (isExpense) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                isExpense = true
                                if (selectedCategoryName == "הכנסות") {
                                    selectedCategoryName = categories.firstOrNull { it.name != "הכנסות" }?.name ?: "אחר"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "הוצאה",
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) Color(0xFFBA1A1A) else Color(0xFF44474E),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(
                                color = if (!isExpense) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                isExpense = false
                                if (categories.any { it.name == "הכנסות" }) {
                                    selectedCategoryName = "הכנסות"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "הכנסה",
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) Color(0xFF2E7D32) else Color(0xFF44474E),
                            fontSize = 13.sp
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("תיאור (לדוגמה: קניות בברכל)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001D36),
                        unfocusedBorderColor = Color(0xFFC4C6D0)
                    )
                )

                // Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("סכום בשקלים") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_amount_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF001D36),
                        unfocusedBorderColor = Color(0xFFC4C6D0)
                    )
                )

                // Cash / Credit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("אמצעי תשלום:", fontWeight = FontWeight.SemiBold, color = Color(0xFF001D36))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = paymentType == "CREDIT",
                            onClick = { paymentType = "CREDIT" },
                            label = { Text("אשראי") },
                            leadingIcon = { if (paymentType == "CREDIT") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = paymentType == "CASH",
                            onClick = { paymentType = "CASH" },
                            label = { Text("מזומן") },
                            leadingIcon = { if (paymentType == "CASH") Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                // Category Selection Grid
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("בחר קטגוריה:", fontWeight = FontWeight.SemiBold, color = Color(0xFF001D36))
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8F9FF), RoundedCornerShape(12.dp))
                            .padding(6.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                val isSelected = category.name == selectedCategoryName
                                val emoji = when (category.name) {
                                    "אוכל מוכן" -> "🥘"
                                    "אוכל קנוי בברכל" -> "🛒"
                                    "סלולר" -> "📱"
                                    "ביגוד" -> "👔"
                                    "מגורים" -> "🏠"
                                    "בריאות" -> "💊"
                                    "תחבורה" -> "🚗"
                                    "הכנסות" -> "📈"
                                    else -> "💰"
                                }
                                val backgroundColor = if (isSelected) Color(0xFF001D36) else Color.White
                                val contentColor = if (isSelected) Color.White else Color(0xFF001D36)
                                
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategoryName = category.name },
                                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE1E2EC))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = emoji, fontSize = 12.sp)
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E)),
                        modifier = Modifier.testTag("manual_dismiss_btn")
                    ) {
                        Text("ביטול", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0.0) {
                                onAdd(title, amt, isExpense, selectedCategoryName, paymentType, System.currentTimeMillis())
                            }
                        },
                        enabled = title.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001D36), contentColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("manual_save_btn")
                    ) {
                        Text("שמור", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val UnspecifiedTypeColor = Color(0xFF555555)

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onAdd: (String) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ניהול קטגוריות",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFFE1E2EC))

                // Add Category Input Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("קטגוריה חדשה...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF001D36),
                            unfocusedBorderColor = Color(0xFFC4C6D0)
                        )
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAdd(newCategoryName)
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF001D36), contentColor = Color.White),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Text("הוסף", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "קטגוריות קיימות:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36)
                )

                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val emoji = when (category.name) {
                            "אוכל מוכן" -> "🥘"
                            "אוכל קנוי בברכל" -> "🛒"
                            "סלולר" -> "📱"
                            "ביגוד" -> "👔"
                            "מגורים" -> "🏠"
                            "בריאות" -> "💊"
                            "תחבורה" -> "🚗"
                            "הכנסות" -> "📈"
                            else -> "💰"
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF191C1E)
                                    )
                                    if (category.isSystem) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0xFFEEF0F8),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "מובנה",
                                                fontSize = 9.sp,
                                                color = Color(0xFF001D36),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (!category.isSystem) {
                                    IconButton(
                                        onClick = { onDelete(category) },
                                        modifier = Modifier.testTag("delete_category_button_${category.name}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "מחק קטגוריה",
                                            tint = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E))
                    ) {
                        Text("סגור", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryBudgetDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetValue by remember { mutableStateOf(if (category.budgetLimit > 0.0) category.budgetLimit.toString() else "") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "הגדרת תקציב ל-${category.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36)
                )

                OutlinedTextField(
                    value = budgetValue,
                    onValueChange = {
                        budgetValue = it
                        isError = false
                    },
                    label = { Text("סכום תקציב חודשי (₪)") },
                    placeholder = { Text("לדוגמה: 1500") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF006494),
                        focusedLabelColor = Color(0xFF006494)
                    )
                )

                if (isError) {
                    Text(
                        text = "אנא הזן סכום תקין וגדול מ-0",
                        color = Color(0xFFBA1A1A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E))
                    ) {
                        Text("ביטול", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val limit = if (budgetValue.isBlank()) 0.0 else budgetValue.toDoubleOrNull()
                            if (limit != null && limit >= 0.0) {
                                onSave(limit)
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006494))
                    ) {
                        Text("שמור תקציב", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditMonthlyBudgetDialog(
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetValue by remember { mutableStateOf(if (currentLimit > 0.0) currentLimit.toString() else "") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "תקציב חודשי כולל",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36)
                )

                Text(
                    text = "גג הוצאה כולל לחודש, על כל הקטגוריות יחד. השאר ריק כדי לבטל את התקרה.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF44474E)
                )

                OutlinedTextField(
                    value = budgetValue,
                    onValueChange = {
                        budgetValue = it
                        isError = false
                    },
                    label = { Text("סכום תקציב חודשי כולל (₪)") },
                    placeholder = { Text("לדוגמה: 5000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF006494),
                        focusedLabelColor = Color(0xFF006494)
                    )
                )

                if (isError) {
                    Text(
                        text = "אנא הזן סכום תקין וגדול או שווה ל-0",
                        color = Color(0xFFBA1A1A),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF44474E))
                    ) {
                        Text("ביטול", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val limit = if (budgetValue.isBlank()) 0.0 else budgetValue.toDoubleOrNull()
                            if (limit != null && limit >= 0.0) {
                                onSave(limit)
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006494))
                    ) {
                        Text("שמור תקציב", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartCard(
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val decFormat = remember { DecimalFormat("#,##0.00") }
    val expenseTransactions = transactions.filter { it.isExpense }
    val totalExpense = expenseTransactions.sumOf { it.amount }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "פילוח הוצאות לפי קטגוריות",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF001D36)
            )
            
            if (totalExpense == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color(0xFFC4C6D0),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "אין הוצאות מתועדות לחודש זה.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            } else {
                val categoryTotals = expenseTransactions
                    .groupBy { it.categoryName }
                    .mapValues { it.value.sumOf { tx -> tx.amount } }
                    .toList()
                    .sortedByDescending { it.second }
                
                val chartColors = listOf(
                    Color(0xFF006494), // Deep Blue
                    Color(0xFF00A699), // Teal
                    Color(0xFF2E7D32), // Forest Green
                    Color(0xFFF2A900), // Amber
                    Color(0xFFD32F2F), // Red
                    Color(0xFF8E24AA), // Purple
                    Color(0xFFE65100), // Dark Orange
                    Color(0xFF0288D1), // Sky Blue
                    Color(0xFF3949AB)  // Indigo
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(140.dp)) {
                            var startAngle = -90f
                            categoryTotals.forEachIndexed { index, (_, amt) ->
                                val sweepAngle = (amt / totalExpense * 360.0).toFloat()
                                drawArc(
                                    color = chartColors[index % chartColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 24.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                startAngle += sweepAngle
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "סה\"כ הוצאות",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF44474E),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "₪${decFormat.format(totalExpense)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }
                    }
                    
                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryTotals.take(5).forEachIndexed { index, (name, amt) ->
                            val pct = (amt / totalExpense * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(chartColors[index % chartColors.size], RoundedCornerShape(2.dp))
                                )
                                Column {
                                    Text(
                                        text = "$name ($pct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C1E)
                                    )
                                    Text(
                                        text = "₪${decFormat.format(amt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF44474E)
                                    )
                                }
                            }
                        }
                        if (categoryTotals.size > 5) {
                            val otherSum = categoryTotals.drop(5).sumOf { it.second }
                            val otherPct = (otherSum / totalExpense * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Gray, RoundedCornerShape(2.dp))
                                )
                                Column {
                                    Text(
                                        text = "אחר ($otherPct%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF191C1E)
                                    )
                                    Text(
                                        text = "₪${decFormat.format(otherSum)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF44474E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartCard(
    allTransactions: List<TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val decFormat = remember { DecimalFormat("#,##0") }
    
    val monthlyData = remember(allTransactions) {
        // Group transactions by year and month
        allTransactions.groupBy { "${it.hebrewYear}_${it.hebrewMonthIndex}" }
            .map { (key, txs) ->
                val firstTx = txs.first()
                val income = txs.filter { !it.isExpense }.sumOf { it.amount }
                val expense = txs.filter { it.isExpense }.sumOf { it.amount }
                
                MonthSummary(
                    year = firstTx.hebrewYear,
                    yearString = firstTx.hebrewYearString,
                    monthIndex = firstTx.hebrewMonthIndex,
                    monthName = firstTx.hebrewMonthName,
                    income = income,
                    expense = expense
                )
            }
            .sortedWith(compareBy<MonthSummary> { it.year }.thenBy { it.monthIndex })
            .takeLast(5) // Show last 5 months
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "השוואת הכנסות מול הוצאות לאורך זמן",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF001D36)
            )
            
            if (monthlyData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFFC4C6D0),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "אין מספיק נתונים להשוואה חודשית.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            } else {
                val maxAmount = monthlyData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
                
                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF2E7D32), RoundedCornerShape(3.dp))
                        )
                        Text("הכנסות", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF191C1E))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFBA1A1A), RoundedCornerShape(3.dp))
                        )
                        Text("הוצאות", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF191C1E))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Chart layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEach { month ->
                        val incomeHeightFraction = (month.income / maxAmount).toFloat().coerceIn(0.01f, 1.0f)
                        val expenseHeightFraction = (month.expense / maxAmount).toFloat().coerceIn(0.01f, 1.0f)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Income Bar
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(incomeHeightFraction)
                                        .background(Color(0xFF2E7D32), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                                // Expense Bar
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(expenseHeightFraction)
                                        .background(Color(0xFFBA1A1A), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = month.monthName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36),
                                maxLines = 1
                            )
                            Text(
                                text = month.yearString,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF44474E),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

data class MonthSummary(
    val year: Int,
    val yearString: String,
    val monthIndex: Int,
    val monthName: String,
    val income: Double,
    val expense: Double
)
