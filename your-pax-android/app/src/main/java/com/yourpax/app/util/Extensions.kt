package com.yourpax.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Int.formatSignal(): String = when {
    this >= -50 -> "Excellent"
    this >= -60 -> "Good"
    this >= -70 -> "Fair"
    else -> "Weak"
}
