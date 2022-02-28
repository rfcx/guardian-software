package org.rfcx.guardian.updater.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.rfcx.guardian.updater.R;
import org.rfcx.guardian.updater.RfcxGuardian;

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
        Button updateRequest = findViewById(R.id.updateRequestButton);
        updateRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((RfcxGuardian) getApplication()).apiUpdateRequestUtils.attemptToTriggerUpdateRequest(true, true);
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
