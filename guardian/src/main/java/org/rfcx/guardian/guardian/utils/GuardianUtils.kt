package org.rfcx.guardian.guardian.utils

import android.content.Context
import android.net.ConnectivityManager
import org.rfcx.guardian.guardian.RfcxGuardian
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object GuardianUtils {

    fun isGuidExisted(context: Context): Boolean {
        val path = context.filesDir.toString() + "/txt/"
        val txtFile = File(path + "/guardian_guid.txt")
        return txtFile.exists()
    }

    fun createRegisterFile(context: Context) {
        val path = context.filesDir.toString() + "/txt/"
        val file = File(path, "guardian_guid.txt")
        FileOutputStream(file).use {
            val app = context.applicationContext as RfcxGuardian
            it.write(app.rfcxDeviceGuid.deviceGuid.toByteArray())
        }
    }

    fun readRegisterFile(context: Context): String {
        val path = context.filesDir.toString() + "/txt/"
        val file = File(path, "guardian_guid.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
