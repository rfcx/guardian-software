package org.rfcx.guardian.guardian.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.rfcx.guardian.guardian.BuildConfig
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.entity.RegisterResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiInterface {
    @POST("v1/guardians/register")
    fun register(@Header("Authorization") authUser: String,
                 @Body guardianInfo: RegisterRequest): Call<RegisterResponse>

//    @GET("v1/guardians")
//    abstract fun getAllGuardians(): Call<GuardianResponse>

    companion object {
        val BASE_URL = "https://api.rfcx.org/"
        fun create(): ApiInterface {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createClient())
                .build()
            return retrofit.create(ApiInterface::class.java)
        }

        fun createClient(): OkHttpClient{
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

            return OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build()
        }
    }
}