package org.rfcx.guardian.admin.comms.swm.data

data class SwmRTResponse (
    val rssi: Int,
    val signalToNoiseRatio: Int,
    val frequencyDeviation: Int,
    val packetTimestamp: String,
    val satelliteId: String
)

