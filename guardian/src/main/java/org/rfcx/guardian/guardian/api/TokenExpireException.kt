package org.rfcx.guardian.guardian.api

import android.content.Context
import org.rfcx.guardian.guardian.manager.PreferenceManager


class TokenExpireException(context: Context) : Exception() {
	init {
		// clear login pref
		PreferenceManager.getInstance(context).clear()
	}
	
}


