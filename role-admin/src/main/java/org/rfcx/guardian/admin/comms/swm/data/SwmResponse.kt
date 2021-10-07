package org.rfcx.guardian.admin.comms.swm.data

data class SwmTD(
    val messageId: String
)

data class SwmMT(
    val unsentMessages: List<SwmUnsentMsg>
)

data class SwmSL(
    val message: String
)

data class SwmRTSatellite (
    val rssi: Int,
    val signalToNoiseRatio: Int,
    val frequencyDeviation: Int,
    val packetTimestamp: String,
    val satelliteId: String
)

data class SwmRTBackground (
    val rssi: Int
)

data class SwmDT(
    val date: Long
)

data class SwmUnsentMsg(
    val hex: String,
    val messageId: String
)
