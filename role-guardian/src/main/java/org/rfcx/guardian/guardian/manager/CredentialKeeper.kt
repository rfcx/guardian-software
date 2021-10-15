package org.rfcx.guardian.guardian.manager

import android.content.Context
import org.rfcx.guardian.guardian.entity.UserAuthResponse

class CredentialKeeper(val context: Context) {

    fun save(user: UserAuthResponse) {
        val preferences = PreferenceManager.getInstance(context)
        // Required
        preferences.putString(PreferenceManager.USER_GUID, user.guid)
        preferences.putString(PreferenceManager.ID_TOKEN, user.idToken)
        preferences.putLong(PreferenceManager.TOKEN_EXPIRED_AT, user.expiredAt)

        // Optional
        if (user.accessToken != null) {
            preferences.putString(PreferenceManager.ACCESS_TOKEN, user.accessToken)
        }
        if (user.refreshToken != null) {
            preferences.putString(PreferenceManager.REFRESH_TOKEN, user.refreshToken)
        }
        if (user.email != null) {
            preferences.putString(PreferenceManager.EMAIL, user.email)
        }
        if (user.nickname != null) {
            preferences.putString(PreferenceManager.NICKNAME, user.nickname)
        }
        preferences.putStringSet(PreferenceManager.ROLES, user.roles)
        preferences.putStringSet(PreferenceManager.ACCESSIBLE_SITES, user.accessibleSites)
        if (user.defaultSite != null) {
            preferences.putString(PreferenceManager.DEFAULT_SITE, user.defaultSite)
        }
    }

    fun clear() {
        val preferences = PreferenceManager.getInstance(context)
        preferences.clear()
    }

    fun hasValidCredentials(): Boolean {
        val preferences = PreferenceManager.getInstance(context)
        return preferences.getString(PreferenceManager.ID_TOKEN, "")
            .isNotEmpty() && preferences.getStringSet(PreferenceManager.ROLES).contains("rfcxUser")
    }

}
