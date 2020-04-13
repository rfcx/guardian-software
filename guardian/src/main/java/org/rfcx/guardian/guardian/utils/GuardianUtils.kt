package org.rfcx.guardian.guardian.utils

import android.content.Context
import android.net.ConnectivityManager
import org.rfcx.guardian.guardian.RfcxGuardian
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object GuardianUtils {

    fun isGuardianRegistered(context: Context): Boolean {
        val path = context.filesDir.toString() + "/txt/"
        val txtFile = File(path + "/registered_at.txt")
        return txtFile.exists()
    }

    fun createRegisterFile(context: Context) {
        val path = context.filesDir.toString() + "/txt/"
        val file = File(path, "registered_at.txt")
        FileOutputStream(file).use {
            val app = context.applicationContext as RfcxGuardian
            it.write(app.rfcxGuardianIdentity.guid.toByteArray())
        }
    }

    fun readRegisterFile(context: Context): String {
        val path = context.filesDir.toString() + "/txt/"
        val file = File(path, "registered_at.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    fun readGuardianGuid(context: Context): String {
        val path = context.filesDir.toString() + "/txt/"
        val file = File(path, "guid.txt")
        return FileInputStream(file).bufferedReader().use { it.readText() }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
