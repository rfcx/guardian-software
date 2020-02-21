package org.rfcx.guardian.guardian.manager

import android.content.Context
import android.util.Log

fun Context.getTokenID(): String? {
	val idToken = PreferenceManager.getInstance(this).getString(PreferenceManager.ID_TOKEN, "")
	return if (idToken.isEmpty()) null else idToken
}

fun Context.getUserNickname(): String {
	val nickname = PreferenceManager.getInstance(this).getString(PreferenceManager.NICKNAME)
	return if (nickname != null && nickname.length > 0) nickname else "undefined"
}