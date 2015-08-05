package org.rfcx.guardian.installer.activity;

import org.rfcx.guardian.installer.RfcxGuardianInstaller;
import org.rfcx.guardian.installer.R;
import org.rfcx.guardian.utility.ShellCommands;

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
		RfcxGuardianInstaller app = (RfcxGuardianInstaller) getApplication();
		switch (item.getItemId()) {
		
		case R.id.menu_prefs:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
		
		case R.id.menu_check_version:
			app.triggerService("ApiCheckVersion",true);
			break;

		case R.id.menu_root_command:
			(new ShellCommands()).executeCommandAsRoot("pm list features",null,getApplicationContext());
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
		((RfcxGuardianInstaller) getApplication()).appPause();
	}
	
}
