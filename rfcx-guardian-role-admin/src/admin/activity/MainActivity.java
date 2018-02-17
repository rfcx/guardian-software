package admin.activity;

import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import admin.device.sentinel.I2cUtils;

import org.rfcx.guardian.admin.R;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

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
			
		case R.id.menu_reboot:
			app.rfcxServiceHandler.triggerService("RebootTrigger", true);
			break;
			
		case R.id.menu_screenshot:
			app.rfcxServiceHandler.triggerService("ScreenShotJob", true);
			break;
			
		case R.id.menu_sntp:
			app.rfcxServiceHandler.triggerService("DateTimeSntpSyncJob", true);
			break;

		case R.id.menu_test_i2c:
			app.sentinelPowerUtils.getBatteryVoltage();
			app.sentinelPowerUtils.getBatteryCurrent();
			app.sentinelPowerUtils.getInputVoltage();
			app.sentinelPowerUtils.getSystemVoltage();
			break;
			
		case R.id.menu_get_prefs:

			String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity.class);
			
			try {
				
				Cursor prefsCursor = 
					app.getApplicationContext().getContentResolver().query(
					RfcxComm.getUri("guardian", "prefs", null),
					RfcxComm.getProjection("guardian", "prefs"),
					null, null, null);
				
				
					if (prefsCursor.getCount() > 0) { if (prefsCursor.moveToFirst()) { try { do {
									
						Log.v( logTag,
								prefsCursor.getString(prefsCursor.getColumnIndex("pref_key"))
								+" : "
								+prefsCursor.getString(	prefsCursor.getColumnIndex("pref_value"))
							);
									
					} while (prefsCursor.moveToNext()); } finally { prefsCursor.close(); } } }
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
			
		//	app.rfcxServiceHandler.triggerService("RebootTrigger", true);
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
