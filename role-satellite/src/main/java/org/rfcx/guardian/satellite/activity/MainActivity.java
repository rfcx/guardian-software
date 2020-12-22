package org.rfcx.guardian.satellite.activity;

import org.rfcx.guardian.satellite.R;
import org.rfcx.guardian.satellite.RfcxGuardian;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		Button sbdMsgSend = findViewById(R.id.sbdMsgSendButton);
		sbdMsgSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		//		((RfcxGuardian) getApplication()).apiUpdateRequestUtils.attemptToTriggerUpdateRequest(true, true);
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
