package com.sublate.gps.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class SettingsActivity extends AppCompatActivity {
    private boolean otherActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction()==null ? "":getIntent().getAction();
        Bundle localBundle = getIntent().getExtras();


        try {
           if (localBundle.getBoolean("gpsSettings", false)) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingsPreferences()).commit();
                otherActivity = true;
            } else
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new SettingsPreferences()).commit();

        } catch (Exception e) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsPreferences()).commit();
        }

    }

    @Override
    protected void onDestroy() {
        /*
        if (!otherActivity)
            Toast.makeText(getBaseContext(), getBaseContext().getString(R.string.closeSettingsInfo), Toast.LENGTH_LONG).show();
        */
        super.onDestroy();
    }



}
