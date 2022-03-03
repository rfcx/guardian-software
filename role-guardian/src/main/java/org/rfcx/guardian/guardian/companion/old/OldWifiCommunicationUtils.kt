package org.rfcx.guardian.guardian.companion.old

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.rfcx.RfcxLog
import org.rfcx.guardian.utility.rfcx.RfcxPrefs

class OldWifiCommunicationUtils(private val context: Context) {

    private val app = context.applicationContext as RfcxGuardian
    private val logTag = RfcxLog.generateLogTag(
        RfcxGuardian.APP_ROLE,
        "OldWifiCommunicationUtils"
    )



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
