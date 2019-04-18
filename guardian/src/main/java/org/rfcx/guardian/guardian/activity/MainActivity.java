package org.rfcx.guardian.guardian.activity;

import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import org.rfcx.guardian.guardian.R;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureService;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import org.rfcx.guardian.guardian.RfcxGuardian;

import java.net.URL;

public class MainActivity extends Activity {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, MainActivity.class);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        RfcxGuardian app = (RfcxGuardian) getApplication();

        switch (item.getItemId()) {

            case R.id.menu_prefs:
                startActivity(new Intent(this, PrefsActivity.class));
                break;

            case R.id.menu_reboot:
                app.deviceControlUtils.runOrTriggerDeviceControl("reboot", app.getApplicationContext().getContentResolver());
                break;

            case R.id.menu_relaunch:
                app.deviceControlUtils.runOrTriggerDeviceControl("relaunch", app.getApplicationContext().getContentResolver());
                break;

            case R.id.menu_screenshot:
                app.deviceControlUtils.runOrTriggerDeviceControl("screenshot", app.getApplicationContext().getContentResolver());
                break;

            case R.id.menu_logcat:
                app.deviceControlUtils.runOrTriggerDeviceControl("logcat", app.getApplicationContext().getContentResolver());
                break;

            case R.id.menu_sntp:
                app.deviceControlUtils.runOrTriggerDeviceControl("datetime_sntp_sync", app.getApplicationContext().getContentResolver());
                break;

            case R.id.menu_purge_checkins:
                app.apiCheckInUtils.purgeAllCheckIns();
                break;

        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        final RfcxGuardian app = (RfcxGuardian) getApplication();

        Button preferences = (Button) findViewById(R.id.preferencesButton);
        preferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PrefsActivity.class));
            }
        });

        Button start = (Button) findViewById(R.id.startButton);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.initializeRoleServices();
                runThread();
            }
        });

        Button stop = (Button) findViewById(R.id.stopButton);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.rfcxServiceHandler.stopAllServices();
            }
        });


    }

    private void runThread() {
        final TextView checkinText = (TextView) findViewById(R.id.checkInText);
        final TextView sizeText = (TextView) findViewById(R.id.sizeText);
        final RfcxGuardian app = (RfcxGuardian) getApplication();
        final AudioEncodeJobService audioEncodeJobService = new AudioEncodeJobService();
        new Thread() {
            public void run() {
                while (true) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String lastestCheckIn = "";
                                if (app.apiCheckInUtils.lastTimeCheckIn != null) {
                                    lastestCheckIn = DateFormat.format("yyyy-MM-dd'T'HH:mm:ss.mmm'Z'", Long.parseLong(app.apiCheckInUtils.lastTimeCheckIn)).toString();
                                }
                                checkinText.setText(lastestCheckIn);

                                String fileSize = "";
                                if (audioEncodeJobService.encodedFileSize != null) {
                                    fileSize = String.valueOf(Integer.parseInt(audioEncodeJobService.encodedFileSize) / 1000);
                                }
                                sizeText.setText(fileSize + "kb");
                            }
                        });
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
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
