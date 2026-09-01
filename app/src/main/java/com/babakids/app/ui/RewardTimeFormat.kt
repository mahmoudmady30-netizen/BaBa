package com.babakids.app.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simple, locale-neutral date/time formatting for "when was this reward
 * earned" — numeric/short-month format on purpose so it reads clearly
 * regardless of the app's language setting, without needing a
 * translated variant.
 */
fun formatRewardTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US)
    return formatter.format(Date(timestamp))
}
