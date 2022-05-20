package org.rfcx.guardian.admin.device.i2c.sentry.infineon

import java.util.*

data class InfineonAtt(
    var measuredAt: Date = Date(),
    var co2: Int = 0
)