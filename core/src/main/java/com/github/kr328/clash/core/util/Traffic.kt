package com.github.kr328.clash.core.util

import com.github.kr328.clash.core.model.Traffic

fun Traffic.trafficUpload(): String {
    return trafficString(scaleTraffic(this ushr 32))
}

fun Traffic.trafficDownload(): String {
    return trafficString(scaleTraffic(this and 0xFFFFFFFF))
}

fun Traffic.trafficTotal(): String {
    val upload = scaleTraffic(this ushr 32)
    val download = scaleTraffic(this and 0xFFFFFFFF)

    return trafficString(upload + download)
}

fun Traffic.trafficCompact(): String {
    val upload = scaleTraffic(this ushr 32)
    val download = scaleTraffic(this and 0xFFFFFFFF)

    return compactBytes(upload + download)
}

fun Traffic.trafficUploadCompact(): String {
    return compactBytes(scaleTraffic(this ushr 32))
}

fun Traffic.trafficDownloadCompact(): String {
    return compactBytes(scaleTraffic(this and 0xFFFFFFFF))
}

fun Traffic.trafficUploadSpeed(): String {
    return speedString(scaleTraffic(this ushr 32))
}

fun Traffic.trafficDownloadSpeed(): String {
    return speedString(scaleTraffic(this and 0xFFFFFFFF))
}

/** Raw scaled upload byte count (cumulative), for computing real per-second speed. */
fun Traffic.rawUploadBytes(): Long = scaleTraffic(this ushr 32)

/** Raw scaled download byte count (cumulative), for computing real per-second speed. */
fun Traffic.rawDownloadBytes(): Long = scaleTraffic(this and 0xFFFFFFFF)

/** Format a bytes-per-second value with adaptive B/s, KB/s, MB/s units. */
fun Long.formatSpeed(): String = speedString(this)

private fun compactBytes(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }

    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0

    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }

    return String.format("%.1f %s", value, units[unit])
}

private fun speedString(bytesPerSec: Long): String {
    if (bytesPerSec < 1024) {
        return "$bytesPerSec B/s"
    }

    val units = arrayOf("KB/s", "MB/s", "GB/s", "TB/s")
    var value = bytesPerSec.toDouble()
    var unit = 0

    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }

    return String.format("%.1f %s", value, units[unit])
}

private fun trafficString(scaled: Long): String {
    return when {
        scaled > 1024 * 1024 * 1024 * 100L -> {
            val data = scaled / 1024 / 1024 / 1024

            String.format("%.2f GiB", data.toFloat() / 100)
        }
        scaled > 1024 * 1024 * 100L -> {
            val data = scaled / 1024 / 1024

            String.format("%.2f MiB", data.toFloat() / 100)
        }
        scaled > 1024 * 100L -> {
            val data = scaled / 1024

            String.format("%.2f KiB", data.toFloat() / 100)
        }
        else -> {
            "$scaled Bytes"
        }
    }
}

private fun scaleTraffic(value: Long): Long {
    val type = (value ushr 30) and 0x3
    val data = value and 0x3FFFFFFF

    return when (type) {
        0L -> data
        1L -> data * 1024
        2L -> data * 1024 * 1024
        3L -> data * 1024 * 1024 * 1024
        else -> throw IllegalArgumentException("invalid value type")
    }
}