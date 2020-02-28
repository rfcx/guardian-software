package org.rfcx.guardian.guardian.utils

import android.content.Context
import android.content.pm.PackageManager

object CheckAppPermissionUtils {

    fun checkLocationPermission(context: Context): Boolean{
        val LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION
        val result = context.checkCallingPermission(LOCATION_COARSE)
        return result == PackageManager.PERMISSION_GRANTED
    }
}
