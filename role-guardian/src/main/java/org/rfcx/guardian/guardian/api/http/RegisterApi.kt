package org.rfcx.guardian.guardian.api.http

import android.content.Context
import android.os.Handler
import android.os.StrictMode
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.manager.getTokenID
import org.rfcx.guardian.utility.network.HttpPostMultipart

object RegisterApi {

    private lateinit var httpPostMultipart: HttpPostMultipart

    fun registerGuardian(
        context: Context,
        guardianInfo: RegisterRequest,
        callback: RegisterCallback
    ) {
        val token = context.getTokenID() ?: ""
        registerGuardian(context, guardianInfo, token, true, callback)
    }

    fun registerGuardian(
        context: Context,
        guardianInfo: RegisterRequest,
        tokenId: String,
        isProduction: Boolean = true,
        callback: RegisterCallback
    ) {
        httpPostMultipart = HttpPostMultipart()
        httpPostMultipart.customHttpHeaders = listOf(arrayOf("Authorization", "Bearer $tokenId"))

        var url = getApiUrl(context, isProduction)
        val postUrl = "${url}v2/guardians/register"
        val body = listOf(
            arrayOf("guid", guardianInfo.guid)
        )

        val handler = Handler()
        val runnable = Runnable {
            val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            val response =
                httpPostMultipart.doMultipartPost(postUrl, body, null)

            if (response.isNotEmpty()) {
                callback.onRegisterSuccess(null, response)
            } else {
                callback.onRegisterFailed(null, "Unsuccessful")
            }
        }
        handler.post(runnable)
    }

    private fun getApiUrl(context: Context, isProduction: Boolean): String {
        val prefs = (context.applicationContext as RfcxGuardian).rfcxPrefs
        val protocol = prefs.getPrefAsString("api_rest_protocol")
        var host = prefs.getPrefAsString("api_rest_host")
        //Add staging-url if current rest api is not on staging
        if (!isProduction) {
            if (!host.contains("staging")) {
                host = "staging-${host}"
            }
        }
        if (protocol != null && host != null) {
            return "${protocol}://${host}/"
        }
        return "https://api.rfcx.org/"
    }
}

interface RegisterCallback {
    fun onRegisterSuccess(t: Throwable?, response: String?)
    fun onRegisterFailed(t: Throwable?, message: String?)
}
