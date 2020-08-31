package org.rfcx.guardian.guardian.socket

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInUtils
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInQueueService
import org.rfcx.guardian.utility.rfcx.RfcxLog

class WifiCommunicationUtils(private val context: Context) {

    private val app = context.applicationContext as RfcxGuardian
    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "WifiCommunicationUtils"
    )

    fun getCurrentConfigurationAsJson(): JSONArray {
        val configurationJsonArray = JSONArray()
        try {
            val configurationJson = JSONObject()
            val simpleRate = app.rfcxPrefs.getPrefAsInt("audio_sample_rate")
            val bitrate = app.rfcxPrefs.getPrefAsInt("audio_encode_bitrate")
            val fileFormat = app.rfcxPrefs.getPrefAsString("audio_encode_codec")
            val duration = app.rfcxPrefs.getPrefAsInt("audio_cycle_duration")

            configurationJson.let {
                it.put("sample_rate", simpleRate)
                it.put("bitrate", bitrate)
                it.put("file_format", fileFormat)
                it.put("duration", duration)
            }

            configurationJsonArray.put(configurationJson)
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        } finally {
            return configurationJsonArray
        }
    }

    fun getDiagnosticAsJson(): JSONArray {
        val diagnosticJsonArray = JSONArray()
        try {
            val diagnosticJson = JSONObject()
            val totalLocalAudio = ApiCheckInQueueService.totalLocalAudio
            val totalSyncedAudio = ApiCheckInUtils.totalSyncedAudio
            val totalRecordedTime = ApiCheckInQueueService.totalRecordedTime
            val totalFileSize = ApiCheckInUtils.totalFileSize
            val battery = app.deviceBattery.getBatteryChargePercentage(this.context, null)

            diagnosticJson.let {
                it.put("total_local", totalLocalAudio)
                it.put("total_checkin", totalSyncedAudio)
                it.put("total_record_time", totalRecordedTime)
                it.put("total_file_size", totalFileSize)
                it.put("battery_percentage", battery)
            }

            diagnosticJsonArray.put(diagnosticJson)
        } catch (e: Exception) {
            RfcxLog.logExc(logTag, e)
        } finally {
            return diagnosticJsonArray
        }
    }

    fun getPrefsChangesAsJson(): JSONArray {
        val jsonArray = JSONArray()
        val jsonObject = JSONObject()
        jsonObject.put("result", "success")
        jsonArray.put(jsonObject)
        return  jsonArray
    }

    fun getAudioBufferAsJson(): JSONArray? {
        if (app.audioCaptureUtils.isAudioChanged) {
            val jsonArray = JSONArray()
            val jsonObject = JSONObject()
            val audioBufferPair = app.audioCaptureUtils.audioBuffer
            jsonObject.put("buffer", Base64.encodeToString(audioBufferPair.first, Base64.NO_WRAP))
            jsonObject.put("read_size", audioBufferPair.second)
            jsonArray.put(jsonObject)
            return  jsonArray
        }
        return JSONArray()
    }
}
