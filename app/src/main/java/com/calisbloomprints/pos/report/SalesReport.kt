package com.calisbloomprints.pos.report

import com.calisbloomprints.pos.data.db.SaleWithItems
import com.calisbloomprints.pos.data.model.DailySalesSummary
import java.util.Calendar

data class SalesReport(
    val generatedAt: Long,
    val periods: List<SalesReportPeriod>,
)

data class SalesReportPeriod(
    val label: String,
    val startAt: Long,
    val endAt: Long,
    val sales: List<SaleWithItems>,
    val summary: DailySalesSummary,
) {
    val itemCount: Int
        get() = sales.sumOf { sale -> sale.items.sumOf { it.quantity } }
}

object SalesReportBuilder {
    fun build(sales: List<SaleWithItems>, now: Long = System.currentTimeMillis()): SalesReport {
        val sortedSales = sales.sortedByDescending { it.sale.createdAt }
        return SalesReport(
            generatedAt = now,
            periods = listOf(
                buildPeriod("Today", startOfDay(now), now, sortedSales),
                buildPeriod("This Week", startOfWeek(now), now, sortedSales),
                buildPeriod("This Month", startOfMonth(now), now, sortedSales),
            ),
        )
    }

    private fun buildPeriod(
        label: String,
        startAt: Long,
        endAt: Long,
        sales: List<SaleWithItems>,
    ): SalesReportPeriod {
        val periodSales = sales.filter { it.sale.createdAt in startAt..endAt }
        return SalesReportPeriod(
            label = label,
            startAt = startAt,
            endAt = endAt,
            sales = periodSales,
            summary = DailySalesSummary.fromSales(periodSales),
        )
    }

    private fun startOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun startOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
