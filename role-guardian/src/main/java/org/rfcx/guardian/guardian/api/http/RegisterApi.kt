package org.rfcx.guardian.guardian.api.http

import android.content.Context
import android.os.Handler
import android.os.StrictMode
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
        registerGuardian(context, guardianInfo, token, callback)
    }

    fun registerGuardian(
        context: Context,
        guardianInfo: RegisterRequest,
        tokenId: String,
        callback: RegisterCallback
    ) {
        httpPostMultipart = HttpPostMultipart()
        httpPostMultipart.customHttpHeaders = listOf(arrayOf("Authorization", "Bearer $tokenId"))

        val url = ApiRest.baseUrl(context)
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
}

interface RegisterCallback {
    fun onRegisterSuccess(t: Throwable?, response: String?)
    fun onRegisterFailed(t: Throwable?, message: String?)
}
