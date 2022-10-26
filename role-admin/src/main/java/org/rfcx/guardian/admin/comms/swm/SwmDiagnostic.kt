package org.rfcx.guardian.admin.comms.swm

import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.admin.comms.swm.api.SwmApi
import java.util.*

class SwmDiagnostic(private val api: SwmApi, private val device: SwmDevice) {

    fun getMomentaryConcatDiagnosticValuesAsJsonArray(): JSONArray {
        val diagnostic = getDiagnostic()
        val swmDiagnosticJSONArr = JSONArray()
        if (diagnostic.isNotEmpty()) {
            val diagnosticJson = JSONObject()
            diagnosticJson.put("swm", diagnostic)
            swmDiagnosticJSONArr.put(diagnosticJson)
        }
        return swmDiagnosticJSONArr
    }

    private fun getDiagnostic(): String {
        val rtBackground = api.getRTBackground()
        val rtSatellite = api.getRTSatellite()
        val unsentMessageNumbers = api.getNumberOfUnsentMessages()
        device.awake()

        var swmBlob = ""
        if (rtBackground != null) {
            // Same style as query from db
            swmBlob += "${Date().time}*${rtBackground.rssi}"
            if (rtSatellite != null && rtSatellite.packetTimestamp != "1970-01-01 00:00:00") {
                swmBlob += "*${rtSatellite.rssi}*${rtSatellite.signalToNoiseRatio}*${rtSatellite.frequencyDeviation}*${rtSatellite.packetTimestamp}*${rtSatellite.satelliteId}"
            }
            swmBlob += "*$unsentMessageNumbers"
        }
        return swmBlob
    }
}
