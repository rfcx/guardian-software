package org.rfcx.guardian.guardian.api

import android.content.Context
import com.crashlytics.android.Crashlytics
import org.rfcx.guardian.guardian.R
import org.rfcx.guardian.guardian.entity.Err
import org.rfcx.guardian.guardian.entity.ErrorResponse
import org.rfcx.guardian.guardian.entity.Ok
import org.rfcx.guardian.guardian.entity.Result
import org.rfcx.guardian.guardian.manager.GsonProvider
import retrofit2.Response

class ResponseParserException(message: String) : Exception(message)
class ResponseUnauthenticatedException : Exception()

fun <T> responseParser(response: Response<T>?): Result<T, Exception> {

    if (response == null) {
        val exception = ResponseParserException("responseParser: response is null")
        return Err(exception)
    }

    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
            return Ok(body)
        }
        else {
            return Err(ResponseParserException("responseParser: isSuccessful but body is null"))
        }
    }

    if (response.code() == 401) {
        return Err(ResponseUnauthenticatedException())
    }

    if (response.errorBody() == null) {
        return Err(ResponseParserException("error and missing error body"))
    }

    try {
        val error: ErrorResponse = GsonProvider.getInstance().gson.fromJson(response.errorBody()?.string(), ErrorResponse::class.java)
        return Err(ResponseParserException("error: ${error.message}"))
    } catch (e: Exception) {
        return Err(ResponseParserException("error: ${response.errorBody()}"))
    }
}

fun responseErrorHandler(error: Exception, callback: ApiCallback, context: Context, exceptionMessagePrefix: String = "") {
    when (error) {
        is ResponseUnauthenticatedException -> {
//            Crashlytics.logException(error)
            callback.onFailed(TokenExpireException(context), null)
        }
        else -> {
//            Crashlytics.logException(Exception("$exceptionMessagePrefix ${error.message}"))
            callback.onFailed(null, context.getString(R.string.error_common))
        }
    }
}