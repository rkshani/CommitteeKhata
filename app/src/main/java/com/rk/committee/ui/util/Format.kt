package com.rk.committee.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.rk.committee.R
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatNumber(value: Long, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(value)

fun formatMonth(month: YearMonth, locale: Locale): String =
    month.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** "Rs 10,000" / "10,000 روپے" */
@Composable
fun money(amount: Long): String = stringResource(R.string.currency_format, formatNumber(amount, currentLocale()))

@Composable
fun monthLabel(month: YearMonth): String = formatMonth(month, currentLocale())
