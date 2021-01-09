package org.rfcx.guardian.classify.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.rfcx.guardian.classify.R;
import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

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


		Button placeholderButton = findViewById(R.id.clickMe);
		placeholderButton.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("LongLogTag")
			@Override
			public void onClick(View v) {

				RfcxGuardian app = (RfcxGuardian) getApplicationContext();
				// example how model running process called
				int sampleRate = 12000;
				float stepSize = app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.PREDICTION_STEP_SIZE);
				float windowSize = app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.PREDICTION_WINDOW_SIZE);

				app.audioClassifyUtils.initClassifier(sampleRate, windowSize, stepSize);
				List<float[]> output = app.audioClassifyUtils.classifyAudio(Environment.getExternalStorageDirectory().getAbsolutePath() + "/chainsaw12000.wav");

				for (float[] flt : output) {

					for (float _flt : flt) {
						Log.e("Rfcx-Classify-MainActivity",""+ _flt);
					}
				}
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
