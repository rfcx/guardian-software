package reboot.activity;

import org.rfcx.guardian.admin.R;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import reboot.RfcxGuardian;
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
