package com.example

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExampleUnitTest {

    private fun generateTestKey(
        deviceId: String,
        client: String,
        type: String,
        start: String,
        expiry: String,
        duration: Int,
        id: String
    ): String {
        val signString = "$deviceId|$client|$type|$start|$expiry|$duration|$id"
        var hash = 0
        for (ch in signString) {
            val code = ch.code
            hash = ((hash shl 5) - hash) + code
        }
        val absHash: Long = if (hash < 0) {
            if (hash == Int.MIN_VALUE) 2147483648L else (-hash).toLong()
        } else {
            hash.toLong()
        }
        val signature = java.lang.Long.toString(absHash, 36).uppercase().padStart(10, '0')

        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("client", client)
            put("type", type)
            put("start", start)
            put("expiry", expiry)
            put("duration", duration)
            put("id", id)
        }
        val encodedPayload = android.util.Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
        return "LIC-$signature$encodedPayload"
    }

    @Test
    fun testValidLicense() {
        val deviceId = "FERR-TESTDEVICE1"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val start = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 35)
        val expiry = sdf.format(cal.time)

        val key = generateTestKey(deviceId, "Ferreteria El Sol", "anual", start, expiry, 30, "TEST-1")
        assertNotNull(key)
        assertTrue(key.startsWith("LIC-"))
    }

    @Test
    fun testExpiredLicenseIdentification() {
        val deviceId = "FERR-TESTDEVICE2"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -60)
        val start = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 30) // Expired 30 days ago
        val expiry = sdf.format(cal.time)

        val expiryDate = sdf.parse(expiry)
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        assertTrue("La fecha de expiración debe ser anterior a hoy", todayCal.after(expiryDate))
    }
}
