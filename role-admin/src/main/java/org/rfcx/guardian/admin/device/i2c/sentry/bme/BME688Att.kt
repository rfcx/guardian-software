package org.rfcx.guardian.admin.device.i2c.sentry.bme

import java.util.*

data class BME688Att(
    var measuredAt: Date = Date(),
    var pressure: Float = 0.0f,
    var humidity: Float = 0.0f,
    var temperature: Float = 0.0f,
    var gas: Float = 0.0f
    )
