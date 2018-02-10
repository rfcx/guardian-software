package setup.activity;

import java.io.File;

import org.rfcx.guardian.setup.R;
import org.rfcx.guardian.utility.ShellCommands;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import setup.RfcxGuardian;

public class MainActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		RfcxGuardian app = (RfcxGuardian) getApplication();
		String thisAppPath = app.getApplicationContext().getFilesDir().getAbsolutePath();
		
		switch (item.getItemId()) {
		
		case R.id.menu_prefs:
			startActivity(new Intent(this, PrefsActivity.class));
			break;

		case R.id.menu_api_register:
			app.rfcxServiceHandler.triggerService("ApiRegister", true);
			break;

		case R.id.menu_api_update:
			app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
			break;
			
//		case R.id.menu_cputuner_install:
//			String cpuTunerPath = thisAppPath.substring(0,thisAppPath.lastIndexOf("/org.rfcx.guardian"))+"/ch.amana.android.cputuner";
//			if (!(new File(thisAppPath.substring(0,thisAppPath.lastIndexOf("/org.rfcx.guardian"))+"/ch.amana.android.cputuner")).isDirectory()) {
//				Log.d("Rfcx-Setup","CPUTuner will now be downloaded and installed...");
//				app.apiCore.targetAppRoleApiEndpoint = "cputuner";
//				app.apiCore.setApiCheckVersionEndpoint(app.rfcxDeviceId.getDeviceGuid());
//				app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
//			} else {
//				Log.d("Rfcx-Setup","CPUTuner is already installed...");
//			}
//			break;
			
//		case R.id.menu_spectrogram_install:
//			String spectrogramPath = thisAppPath.substring(0,thisAppPath.lastIndexOf("/org.rfcx.guardian"))+"/radonsoft.net.spectralviewpro";
//			if (!(new File(thisAppPath.substring(0,thisAppPath.lastIndexOf("/org.rfcx.guardian"))+"/radonsoft.net.spectralviewpro")).isDirectory()) {
//				Log.d("Rfcx-Setup","SpectralViewPro will now be downloaded and installed...");
//				app.apiCore.targetAppRoleApiEndpoint = "spectrogram";
//				app.apiCore.setApiCheckVersionEndpoint(app.rfcxDeviceId.getDeviceGuid());
//				app.rfcxServiceHandler.triggerService("ApiCheckVersion", true);
//			} else {
//				Log.d("Rfcx-Setup","SpectralViewPro is already installed...");
//			}
//			break;

			
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
