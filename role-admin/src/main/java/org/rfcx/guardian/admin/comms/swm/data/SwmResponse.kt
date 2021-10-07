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

data class SwmRT(
    val rssiBackground: Int,
    val rssiSatellite: Int? = null,
    val signalToNoiseRatio: Int? = null,
    val frequencyDeviation: Int? = null,
    val packetTimestamp: String? = null,
    val satelliteId: String? = null
)

data class SwmDT(
    val date: Long
)

data class SwmUnsentMsg(
    val hex: String,
    val messageId: String
)
