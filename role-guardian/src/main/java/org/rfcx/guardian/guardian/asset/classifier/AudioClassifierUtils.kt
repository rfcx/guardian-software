package org.rfcx.guardian.guardian.asset.classifier

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.utility.asset.RfcxAsset
import org.rfcx.guardian.utility.rfcx.RfcxLog

class AudioClassifierUtils(context: Context) {

    private val logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifierUtils")

    private var app: RfcxGuardian = context.applicationContext as RfcxGuardian

    fun getActiveClassifierInfoAsJson(): JSONObject {
        val activeClsfObj = JSONObject()
        try {
            val classifierActiveArr = JSONArray()
            app.audioClassifierDb.dbActive.allRows.forEach {
                if (it[0] != null) {
                    val classifierId = it[1]
                    val classifierName = it[2]
                    val classifierVersion = it[3]
                    val classifierObj = JSONObject()
                    classifierObj.put("id", classifierId)
                    classifierObj.put("guid", "$classifierName-v$classifierVersion")
                    classifierActiveArr.put(classifierObj)
                }
            }
            activeClsfObj.put(RfcxAsset.getTypePlural("active-classifier"), classifierActiveArr)
        } catch (e: JSONException) {
            RfcxLog.logExc(logTag, e)
        }
        return activeClsfObj
    }

    fun getActiveClassifierCount(): Int {
        return app.audioClassifierDb.dbActive.count
    }
}
