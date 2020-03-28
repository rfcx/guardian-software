package org.rfcx.guardian.guardian.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.rfcx.guardian.utility.rfcx.RfcxLog
import kotlin.Exception

object CrashlyticsUtils : RfcxLog(){
    override fun logException(logTag: String?, exc: Exception?) {
        super.logException(logTag, exc)
        FirebaseCrashlytics.getInstance().recordException(exc ?: Exception("This exception has not message."))
    }
}
