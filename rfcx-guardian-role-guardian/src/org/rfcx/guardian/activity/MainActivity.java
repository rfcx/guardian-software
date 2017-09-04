package org.rfcx.guardian.activity;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		switch (item.getItemId()) {
		
		case R.id.menu_prefs:
			startActivity(new Intent(this, PrefsActivity.class));
			break;

		case R.id.menu_reboot:
			app.rfcxServiceHandler.triggerService("RebootTrigger", true);
			break;

		case R.id.menu_screenshot:
			app.rfcxServiceHandler.triggerService("DeviceScreenShotJob", true);
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
	}

	@Override
	public void onPause() {
		super.onPause();
		((RfcxGuardian) getApplication()).appPause();
	}
	
}
