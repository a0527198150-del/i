package com.example.data

import java.util.Calendar

data class GregorianCycleInfo(
    val year: Int,
    val month: Int, // 1-12
    val monthName: String
)

object GregorianCycleHelper {

    private val monthNamesHebrew = listOf(
        "ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני",
        "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר"
    )

    /**
     * Determines which billing-cycle "month bucket" a given timestamp falls into,
     * based on a configurable cycle start day (1-28). Days before the start day
     * belong to the previous month's cycle bucket - this matches how credit card
     * statement periods typically work (e.g. a cycle starting on the 10th means
     * the 1st-9th still belong to the prior month's statement).
     */
    fun getCycleInfo(timestampMillis: Long, startDay: Int): GregorianCycleInfo {
        val safeStartDay = startDay.coerceIn(1, 28)
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMillis

        val day = cal.get(Calendar.DAY_OF_MONTH)
        var month = cal.get(Calendar.MONTH) // 0-based
        var year = cal.get(Calendar.YEAR)

        if (day < safeStartDay) {
            month -= 1
            if (month < 0) {
                month = 11
                year -= 1
            }
        }

        return GregorianCycleInfo(
            year = year,
            month = month + 1,
            monthName = monthNamesHebrew[month]
        )
    }
}
