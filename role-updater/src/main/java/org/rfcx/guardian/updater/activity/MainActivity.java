package org.rfcx.guardian.updater.activity;

import android.view.View;
import android.widget.Button;
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

		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		final RfcxGuardian app = (RfcxGuardian) getApplication();
		Button checkVersion = findViewById(R.id.checkVersionButton);
		checkVersion.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				app.apiCheckVersionUtils.attemptToTriggerCheckIn(true, true);
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
