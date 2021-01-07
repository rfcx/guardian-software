package org.rfcx.guardian.classify.activity;

import org.rfcx.guardian.classify.AudioConverter;
import org.rfcx.guardian.classify.MLPredictor;
import org.rfcx.guardian.classify.R;
import org.rfcx.guardian.classify.RfcxGuardian;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.List;

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
		Button sbdMsgSend = findViewById(R.id.sendButton);
		sbdMsgSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		//		((RfcxGuardian) getApplication()).apiUpdateRequestUtils.attemptToTriggerUpdateRequest(true, true);
			}
		});
		AudioConverter converter = AudioConverter.INSTANCE;
		RfcxGuardian app = (RfcxGuardian) this.getApplicationContext();
		app.audioClassifyUtils.classifyAudio(Environment.getExternalStorageDirectory().getAbsolutePath() + "/chainsaw12000.wav");
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
