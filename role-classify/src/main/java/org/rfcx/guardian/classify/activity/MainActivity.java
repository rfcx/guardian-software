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
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
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


//		Button placeholderButton = findViewById(R.id.clickMe);
//		placeholderButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//
//				// Do something here...
//
//			}
//		});

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
