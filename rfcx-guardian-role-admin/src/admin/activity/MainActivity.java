package admin.activity;

import org.rfcx.guardian.admin.R;

import admin.RfcxGuardian;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import rfcx.utility.device.control.DeviceKeyEntry;
import rfcx.utility.rfcx.RfcxComm;
import rfcx.utility.rfcx.RfcxLog;

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
			
		case R.id.menu_relaunch:
			app.rfcxServiceHandler.triggerIntentServiceImmediately("ForceRoleRelaunch");
			break;
			
		case R.id.menu_screenshot:
			app.rfcxServiceHandler.triggerService("ScreenShotCapture", true);
			break;
			
		case R.id.menu_sntp:
			app.rfcxServiceHandler.triggerService("DateTimeSntpSyncJob", true);
			break;
		
		case R.id.menu_i2c_view:
			app.deviceSentinelPowerUtils.saveSentinelPowerValuesToDatabase(app.getApplicationContext(), true);
			break;
		
		case R.id.menu_logcat:
			app.rfcxServiceHandler.triggerService("LogCatCapture", true);
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
