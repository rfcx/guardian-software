package org.rfcx.guardian.admin.activity;

import org.rfcx.guardian.admin.R;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class PrefsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
	
}
