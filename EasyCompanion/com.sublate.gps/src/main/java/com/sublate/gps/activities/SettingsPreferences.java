package com.sublate.gps.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.sublate.gps.Config;
import com.sublate.gps.R;
import com.sublate.gps.preferences.Preferences;


public class SettingsPreferences extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private Preferences mSharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //messageController = new GuiServiceMessageController(this., TrackerService.class);

            addPreferencesFromResource(R.xml.activity_preferences);

            Preference enableDisablePref = findPreference("enableDisableGps");
            enableDisablePref.setOnPreferenceClickListener(this);

            Preference startstopservice = findPreference("startStopService");
            startstopservice.setOnPreferenceClickListener(this);
            Preference resetPref = findPreference("reset_preference");
            resetPref.setOnPreferenceClickListener(this);

        }


    @Override
    public boolean onPreferenceClick(Preference preference) {

        mSharedPreferences = Preferences.getPreferences(getActivity().getApplicationContext());

        if (preference.getKey().equals("enableDisableGps")) {
            startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            return true;
        }

        if (preference.getKey().equals("reset_preference")) {
                //Now go away
                getActivity().finish();
                return true;

        }

        if (preference.getKey().equals("startStopService")) {
            final CheckBoxPreference stateCheckBox = (CheckBoxPreference)findPreference("startStopService");
                //Config.setServicesEnabled(getActivity().getApplicationContext(), stateCheckBox.isChecked());
                mSharedPreferences.setStartStopService(stateCheckBox.isChecked());
            return true;
        }
        return false;

   }

}
