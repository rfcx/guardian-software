package activity;

import org.rfcx.guardian.R;
import org.rfcx.guardian.RfcxGuardian;
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
			RfcxGuardian app = (RfcxGuardian) getApplication();
			app.apiCheckIn.sendCheckIn(getApplicationContext());
			app.airplaneMode.setOn(this);
			app.airplaneMode.setOff(this);
			break;
		}
		return true;
	}
	
//
	/*
    private TextView view;
    
    protected void call(String phoneNumber) {
        this.view.append("\n"+phoneNumber);
        try {
               startActivityForResult(
                		new Intent("android.intent.action.CALL",Uri.parse("tel:" + phoneNumber))
                		, 1);
        } catch (Exception eExcept) { this.view.append("\n\n\n"+eExcept.toString()); }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
    	
    	this.view.append("\nUSSD: " + requestCode + " " + resultCode + "" + data+"\n");
    }
    */
//
    /*
 @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String encodedHash = Uri.encode("#");
        this.view = new TextView(this);
        setContentView(this.view);  
        
       call("*123*" + encodedHash); //send *123*#
    }

protected void call(String phoneNumber) {
        this.view.append("\n"+phoneNumber);
        try {
                startActivityForResult(new Intent("android.intent.action.CALL",Uri.parse("tel:" + phoneNumber)), 1);
        } catch (Exception eExcept) { this.view.append("\n\n\n"+eExcept.toString()); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {
    	
    	this.view.append("\nUSSD: " + requestCode + " " + resultCode + "" + data+"\n");
    	USSD ussd=new USSD(4000,4000); //read the log 4 s before the call of this app and 4s after
    	if (ussd.IsFound()) this.view.append("\n Msg String:\n"+ussd.getMsg());
    	else this.view.append("No USSD msg received");
}

	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		((RfcxGuardian) getApplication()).launchAllServices(this);
		((RfcxGuardian) getApplication()).launchAllIntentServices(this);
		
//        String encodedHash = Uri.encode("#");
//        this.view = new TextView(this);
//        setContentView(this.view);  
//        
//       call("*123"+encodedHash); 
//       call("*147*1"+encodedHash); 
		
	}

	@Override
	public void onResume() {
		super.onResume();
		((RfcxGuardian) getApplication()).appResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		((RfcxGuardian) getApplication()).appPause();
	}

}
