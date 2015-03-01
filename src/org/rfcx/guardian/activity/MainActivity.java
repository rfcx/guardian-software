package org.rfcx.guardian.activity;

import org.rfcx.guardian.R;
import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.DeviceScreenShot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		switch (item.getItemId()) {
		case R.id.menu_prefs:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
		case R.id.menu_purge_audio:
			app.audioCore.purgeAllAudioAssets(app.audioDb);
			break;
		case R.id.menu_send_checkin:
			app.apiCore.triggerCheckIn(true);
			break;
		case R.id.menu_carrier_topup:
			app.triggerIntentService("CarrierCodeTrigger-TopUp",0,0);
			break;
		case R.id.menu_save_screenshot:
			(new DeviceScreenShot()).saveScreenShot(app.getApplicationContext());
			break;
		
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
	}

	@Override
	public void onResume() {
		super.onResume();
		((RfcxGuardian) getApplication()).appResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		((RfcxGuardian) getApplication()).appPause();
	}

}
