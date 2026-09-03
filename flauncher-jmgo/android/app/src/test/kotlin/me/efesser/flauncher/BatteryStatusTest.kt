package me.efesser.flauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStatusTest {
    @Test
    fun prefersValidMcuValuesOverBrokenAndroidBroadcast() {
        assertEquals(
            BatterySnapshot(percent = 48, charging = false),
            BatteryStatus.select(
                mcuCapacity = "48\n",
                mcuStatus = "Discharging\n",
                broadcastLevel = 100,
                broadcastScale = 100,
                plugged = true,
            ),
        )
    }

    @Test
    fun fallsBackToScaledAndroidBroadcast() {
        assertEquals(
            BatterySnapshot(percent = 50, charging = true),
            BatteryStatus.select(
                mcuCapacity = null,
                mcuStatus = null,
                broadcastLevel = 25,
                broadcastScale = 50,
                plugged = true,
            ),
        )
    }

    @Test
    fun rejectsInvalidCapacityAndRecognizesFullAsCharging() {
        assertEquals(
            BatterySnapshot(percent = null, charging = false),
            BatteryStatus.select("-1", "Discharging", null, null, false),
        )
        assertEquals(
            BatterySnapshot(percent = 100, charging = true),
            BatteryStatus.select("100", "Full", null, null, false),
        )
    }
}
