package dev.alastorkaneki.gxmods

fun formatCount(value: Long): String = when {
    value >= 1_000_000_000 -> compact(value, 1_000_000_000, "B")
    value >= 1_000_000 -> compact(value, 1_000_000, "M")
    value >= 1_000 -> compact(value, 1_000, "K")
    else -> value.toString()
}

fun formatBytes(value: Long): String {
    if (value <= 0) return "Unknown size"
    val units = listOf("B", "KB", "MB", "GB")
    var amount = value.toDouble()
    var unit = 0
    while (amount >= 1024 && unit < units.lastIndex) {
        amount /= 1024
        unit++
    }
    val rounded = ((amount * 10).toLong() / 10.0)
    return "$rounded ${units[unit]}"
}

fun formatIsoDate(value: String): String = value.take(10).ifBlank { "Unknown" }

private fun compact(value: Long, divisor: Long, suffix: String): String {
    val scaled = value.toDouble() / divisor
    val rounded = ((scaled * 10).toLong() / 10.0)
    return "$rounded$suffix"
}
