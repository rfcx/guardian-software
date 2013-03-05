package org.rfcx.rfcx_src_android;

import org.rfcx.src_audio.AudioCaptureService;
import org.rfcx.src_audio.AudioState;
import org.rfcx.src_api.ApiComm;
import org.rfcx.src_api.ApiCommService;
import org.rfcx.src_arduino.*;
import org.rfcx.src_device.DeviceStatsService;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

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
			stopService(new Intent(this, DeviceStatsService.class));
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

		if (ArduinoState.isArduinoEnabled()) {
			this.startService(new Intent(this, ArduinoService.class));
		}
		if (AudioState.isAudioEnabled()) {
			this.startService(new Intent(this, AudioCaptureService.class));
		}
		if (DeviceStatsService.areDeviceStatsEnabled()) {
			this.startService(new Intent(this, DeviceStatsService.class));
		}
		if (ApiComm.isApiCommEnabled()) {
			this.startService(new Intent(this, ApiCommService.class));
		}
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
