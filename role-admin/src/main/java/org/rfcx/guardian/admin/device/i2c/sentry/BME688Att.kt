package org.rfcx.guardian.admin.device.i2c.sentry

import java.util.*

data class BME688Att(
    val measuredAt: Date,
    val pressure: Double,
    val humidity: Double,
    val temperature: Double,
    val gas: Double
    )
