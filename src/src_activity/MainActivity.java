package src_activity;

import org.rfcx.src_android.R;
import org.rfcx.src_android.RfcxSource;
import org.rfcx.src_android.R.id;
import org.rfcx.src_android.R.layout;
import org.rfcx.src_android.R.menu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
		case R.id.menu_prefs:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
		case R.id.menu_connectivity:
			RfcxSource app = (RfcxSource) getApplication();
			app.airplaneMode.setOn(this);
			app.airplaneMode.setOff(this);
			break;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		((RfcxSource) getApplication()).launchAllServices(this);
		((RfcxSource) getApplication()).launchAllIntentServices(this);
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
