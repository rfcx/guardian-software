package org.rfcx.guardian.guardian.utils

import android.content.Context
import android.content.pm.PackageManager

object CheckAppPermissionUtils {
    
    fun checkLocationPermission(context: Context): Boolean {
        val LOCATION_COARSE = android.Manifest.permission.ACCESS_COARSE_LOCATION
        val LOCATION_FINE = android.Manifest.permission.ACCESS_FINE_LOCATION
        val resultCoarse = context.checkCallingPermission(LOCATION_COARSE)
        val resultFine = context.checkCallingPermission(LOCATION_FINE)
        return (resultCoarse == PackageManager.PERMISSION_GRANTED) == (resultFine == PackageManager.PERMISSION_GRANTED)
    }
}
