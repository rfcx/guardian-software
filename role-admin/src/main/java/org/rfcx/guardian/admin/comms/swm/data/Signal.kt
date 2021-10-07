package org.rfcx.guardian.admin.comms.swm

data class Signal(
    val rssiBackground: Int,
    val rssiSatellite: Int? = null,
    val signalToNoiseRatio: Int? = null,
    val frequencyDeviation: Int? = null,
    val packetTimestamp: String? = null,
    val satelliteId: String? = null
    )
