package com.parqueadero.appparqueadero.utils

object Constants {
    val BASE_URL: String
        get() = if (isEmulator()) {
            "http://10.0.2.2:8080/api/"
        } else {
            "http://192.168.1.3:8080/api/"
        }

    private fun isEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.contains("generic") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.BOARD.lowercase().contains("nox") ||
                android.os.Build.BRAND.startsWith("generic") &&
                android.os.Build.DEVICE.startsWith("generic")
    }
}
