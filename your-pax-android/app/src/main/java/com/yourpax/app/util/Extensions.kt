package com.yourpax.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.formatTimestamp(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(this))
}

fun Int.formatSignal(): String = when {
    this >= -50 -> "Excellent"
    this >= -60 -> "Good"
    this >= -70 -> "Fair"
    else -> "Weak"
}
