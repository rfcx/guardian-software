package guardian.activity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.R;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import guardian.RfcxGuardian;

public class MainActivity extends Activity {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity.class);

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
			app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
			break;

		case R.id.menu_relaunch:
			app.deviceControlUtils.runOrTriggerDeviceControl("relaunch", app.getApplicationContext().getContentResolver());
			break;

		case R.id.menu_screenshot:
			app.deviceControlUtils.runOrTriggerDeviceControl("screenshot", app.getApplicationContext().getContentResolver());
			break;

		case R.id.menu_logcat:
			app.deviceControlUtils.runOrTriggerDeviceControl("logcat", app.getApplicationContext().getContentResolver());
			break;

		case R.id.menu_sntp:
			app.deviceControlUtils.runOrTriggerDeviceControl("datetime_sntp_sync", app.getApplicationContext().getContentResolver());
			break;

		case R.id.menu_purge_checkins:
			app.apiCheckInUtils.purgeAllCheckIns();
			break;
		
		}
		
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		final RfcxGuardian app = (RfcxGuardian) getApplication();
	
		Button preferences = (Button) findViewById(R.id.preferencesButton);
		preferences.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, PrefsActivity.class));
			}
		});
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
