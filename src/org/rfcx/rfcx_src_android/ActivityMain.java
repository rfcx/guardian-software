package org.rfcx.rfcx_src_android;

import org.rfcx.src_state.*;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ActivityMain extends Activity {
	
	Button bttnPowerOn, bttnPowerOff;
	
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
			startActivity(new Intent(this, ActivityPrefs.class));
			break;
		case R.id.menuArduinoServiceStart:
			startService(new Intent(this, ArduinoService.class));
			break;
		case R.id.menuAudioServiceStart:
			startService(new Intent(this, ServiceAudioCapture.class));
			break;
		case R.id.menuCpuServiceStart:
			startService(new Intent(this, CpuService.class));
			break;
		case R.id.menuAirplaneModeToggle:
			((RfcxSource) getApplication()).airplaneMode.setToggle(this);
			break;
		}
		return true;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

	    startService(new Intent(this, ArduinoService.class));
	    startService(new Intent(this, ServiceAudioCapture.class));

		bttnPowerOn = (Button) findViewById(R.id.bttnPowerOn);
		bttnPowerOff = (Button) findViewById(R.id.bttnPowerOff);
	    
	    bttnPowerOn.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSource) getApplication()).sendArduinoCommand("s");
	    	}
	    });
	    
	    bttnPowerOff.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSource) getApplication()).sendArduinoCommand("t");
	    	}
	    });
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
