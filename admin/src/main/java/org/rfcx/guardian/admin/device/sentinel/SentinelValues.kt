package org.rfcx.guardian.admin.device.sentinel

data class SentinelValues(
    val system: SentinalValueSet,
    val input: SentinalValueSet,
    val battery: SentinalValueSet
)

data class SentinalValueSet(
    val voltage: Double,
    val current: Double,
    val temp: Double,
    val power: Double
) {
    val voltageString: String get() = Math.round(voltage).toString()
    val currentString: String get() = Math.round(current).toString()
    val tempString: String get() = Math.round(temp).toString()
    val powerString: String get() = Math.round(power).toString()
    override fun toString(): String {
        return "${voltageString}mV - ${currentString}mA - ${tempString}deg - ${powerString}mW"
    }
}