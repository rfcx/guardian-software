package org.rfcx.guardian.guardian.api.http

import android.content.Context
import android.os.Handler
import android.os.StrictMode
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.manager.getTokenID
import org.rfcx.guardian.utility.http.HttpGet

object GuardianCheckApi {

    private lateinit var httpGet: HttpGet

    fun exists(context: Context, guid: String, callback: GuardianCheckCallback) {
        val token = context.getTokenID()
        httpGet = HttpGet(context, RfcxGuardian.APP_ROLE)
        httpGet.customHttpHeaders = listOf(arrayOf("Authorization", "Bearer ${token!!}"))

        val url = ApiRest.baseUrl(context)
        val postUrl = "${url}v2/guardians/${guid}"

        val handler = Handler()
        val runnable = Runnable {
            val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val response = httpGet.getAsJson(postUrl, null)

            if (response.length() > 0) {
                callback.onGuardianCheckSuccess(null, response.getString("token"))
            } else {
                callback.onGuardianCheckFailed(null, "Unsuccessful")
            }
        }
        handler.post(runnable)
    }
}

interface GuardianCheckCallback {
    fun onGuardianCheckSuccess(t: Throwable?, authToken: String?)
    fun onGuardianCheckFailed(t: Throwable?, message: String?)
}
