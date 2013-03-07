package org.rfcx.rfcx_src_android;

import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_device.DeviceStateService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuSettings:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
		case R.id.menuArduinoServiceStop:
			stopService(new Intent(this,
					org.rfcx.src_arduino.ArduinoService.class));
			break;
		case R.id.menuAudioServiceStop:
			stopService(new Intent(this, AudioCaptureService.class));
			break;
		case R.id.menuCpuServiceStop:
			stopService(new Intent(this, DeviceStateService.class));
			break;
		case R.id.menuAirplaneModeToggle:
			((RfcxSource) getApplication()).airplaneMode.setToggle(this);
			break;
		case R.id.menuApiSendTest:
			((RfcxSource) getApplication()).apiComm.sendData(this);
			break;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		((RfcxSource) getApplication()).launchServices(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		((RfcxSource) getApplication()).appResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		((RfcxSource) getApplication()).appPause();
	}

}
