package org.rfcx.guardian.guardian.manager

import android.content.Context
import android.util.Log

fun Context.getTokenID(): String? {
	val idToken = PreferenceManager.getInstance(this).getString(PreferenceManager.ID_TOKEN, "")
	Log.d("getToken", idToken)
	return if (idToken.isEmpty()) null else idToken
}

//fun Context.getSiteName(): String {
//	val defaultSiteName = PreferenceManager.getInstance(this).getString(PreferenceManager.DEFAULT_SITE, "")
//	val database = SiteGuardianDb()
//	val guardianGroupId = PreferenceManager.getInstance(this).getString(PreferenceManager.SELECTED_GUARDIAN_GROUP) ?: ""
//	val siteId = database.guardianGroup(guardianGroupId)?.siteId ?: ""
//	val site = database.site(siteId)
//	return if (site != null) site.name else defaultSiteName.capitalize()
//}

fun Context.getGuardianGroup(): String? {
	val group = PreferenceManager.getInstance(this).getString(PreferenceManager.SELECTED_GUARDIAN_GROUP, "")
	return if (group.isEmpty()) null else group
}

fun Context.getUserGuId(): String? {
	val guId = PreferenceManager.getInstance(this).getString(PreferenceManager.USER_GUID, "")
	return if (guId.isEmpty()) null else guId
}

fun Context.getUserNickname(): String {
	val nickname = PreferenceManager.getInstance(this).getString(PreferenceManager.NICKNAME)
	return if (nickname != null && nickname.length > 0) nickname else "undefined"
}