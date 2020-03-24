package org.rfcx.guardian.guardian.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.guardian.guardian.RfcxGuardian
import org.rfcx.guardian.guardian.entity.GuardianResponse
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.entity.RegisterResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiInterface {
    @POST("v1/guardians/register")
    fun register(@Header("Authorization") authUser: String,
                 @Body guardianInfo: RegisterRequest): Call<RegisterResponse>

    @GET("v2/guardians/{guid}")
    fun isGuardianExisted(@Header("Authorization") token: String,
                          @Path("guid") guid: String): Call<GuardianResponse>

    companion object {
        fun create(context: Context): ApiInterface {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl(context))
                .addConverterFactory(GsonConverterFactory.create())
                .client(createClient())
                .build()
            return retrofit.create(ApiInterface::class.java)
        }

        private fun createClient(): OkHttpClient {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            return OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build()
        }

        private fun baseUrl(context: Context): String {
            val prefs = (context.getApplicationContext() as RfcxGuardian).rfcxPrefs
            val protocol = prefs.getPrefAsString("api_rest_protocol")
            val host = prefs.getPrefAsString("api_rest_host")
            if (protocol != null && host != null) {
                return "${protocol}://${host}/"
            }
            return "https://api.rfcx.org/"
        }
    }
}