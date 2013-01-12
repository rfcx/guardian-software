package org.rfcx.rfcx_src_android;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Home extends Activity {
	
	Button bttnStart, bttnStop, bttnPowerOn, bttnPowerOff;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuServiceStart:
			startService(new Intent(this, ArduinoCommService.class));
			break;
		case R.id.menuServiceStop:
			stopService(new Intent(this, ArduinoCommService.class));
			break;
		case R.id.menuSettings:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
		}
		return true;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		bttnStart = (Button) findViewById(R.id.bttnStart);
		bttnStop = (Button) findViewById(R.id.bttnStop);
		bttnPowerOn = (Button) findViewById(R.id.bttnPowerOn);
		bttnPowerOff = (Button) findViewById(R.id.bttnPowerOff);

	    bttnStart.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSrcApplication) getApplication()).sendBtCommand("a");
	    	}
	    });

	    bttnStop.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSrcApplication) getApplication()).sendBtCommand("b");
	    	}
	    });
	    
	    bttnPowerOn.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSrcApplication) getApplication()).sendBtCommand("s");
	    	}
	    });
	    
	    bttnPowerOff.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	    		((RfcxSrcApplication) getApplication()).sendBtCommand("t");
	    	}
	    });
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((RfcxSrcApplication) getApplication()).appResume();

	}
	
	@Override
	public void onPause() {
		super.onPause();
		((RfcxSrcApplication) getApplication()).appPause();
	}
	
}
