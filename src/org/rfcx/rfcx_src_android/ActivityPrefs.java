package org.rfcx.rfcx_src_android;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ActivityPrefs extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
	
}
