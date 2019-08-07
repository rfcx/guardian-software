package org.rfcx.guardian.updater.activity;

import org.rfcx.guardian.updater.R;
import org.rfcx.guardian.updater.RfcxGuardian;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import org.rfcx.guardian.utility.misc.ShellCommands;

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
		
		case R.id.menu_check_version:
			app.rfcxServiceHandler.triggerService("ApiCheckVersion",true);
			break;

		case R.id.menu_root_command:
			ShellCommands.triggerNeedForRootAccess(getApplicationContext());
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
