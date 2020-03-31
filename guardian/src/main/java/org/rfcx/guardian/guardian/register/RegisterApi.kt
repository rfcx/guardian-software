package org.rfcx.guardian.guardian.register

import android.content.Context
import android.util.Log
import org.rfcx.guardian.guardian.entity.RegisterRequest
import org.rfcx.guardian.guardian.entity.RegisterResponse
import org.rfcx.guardian.guardian.manager.getTokenID
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterApi {

    fun registerGuardian(context: Context, guardianInfo: RegisterRequest, callback: RegisterCallback) {
        val token = context.getTokenID()
        if (token == null) {
            callback.onFailed(TokenExpireException(context), null)
            return
        }

        val apiService = ApiInterface.create(context)
        apiService.register("Bearer $token", guardianInfo)
            .enqueue(object : Callback<RegisterResponse> {
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    callback.onFailed(t, null)
                    Log.d("register_failed","fail from call api")
                }

                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if(response.isSuccessful){
                        callback.onSuccess()
                    }else{
                        Log.d("register", response.toString())
                        callback.onFailed(null, "Unsuccessful")
                    }
                }
            })
    }

    interface RegisterCallback : ApiCallback {
        fun onSuccess()
    }
}