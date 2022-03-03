package org.rfcx.guardian.admin.comms.swm.data

data class SwmGSResponse(
    val hdop: Int,
    val vdop: Int,
    val gnss: Int,
    val type: String
)
