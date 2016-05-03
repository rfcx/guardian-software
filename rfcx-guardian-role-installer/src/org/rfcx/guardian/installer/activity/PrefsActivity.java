package org.rfcx.guardian.installer.activity;

import org.rfcx.guardian.installer.R;

import android.preference.PreferenceActivity;
import android.os.Bundle;

public class PrefsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
	}
	
}
