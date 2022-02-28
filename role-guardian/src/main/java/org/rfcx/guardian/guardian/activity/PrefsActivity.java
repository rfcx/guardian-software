package org.rfcx.guardian.guardian.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.rfcx.guardian.guardian.R;

public class PrefsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.prefs);
    }

}
