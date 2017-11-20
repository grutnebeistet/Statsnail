package com.statsnail.roberts.statsnail.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.sync.SyncUtils;

import timber.log.Timber;

/**
 * Created by Adrian on 03/11/2017.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_preferences);

        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = prefScreen.getSharedPreferences();
        if (sharedPreferences.getString(getString(R.string.pref_map_type_key),
                getString(R.string.map_type_def_value)).equals(getString(R.string.map_type_def_value)))
            findPreference(getString(R.string.map_pref_key)).setEnabled(true);

        int count = prefScreen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference, Object value) {
        String stringValue = value.toString();
        String key = preference.getKey();
        Timber.d("setPreferenceSummary, value, key: " + value + ", " + key);

        if (preference instanceof ListPreference) {
            /* For list preferences, look up the correct display value in */
            /* the preference's 'entries' list (since they have separate labels/values). */
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0) {
                Timber.d("set summary: " + listPreference.getEntries()[prefIndex]);
                preference.setSummary(listPreference.getEntries()[prefIndex]);
                if (key.equals(getString(R.string.pref_map_type_key)))
                    if (!value.equals(getString(R.string.map_type_def_value)))
                        findPreference(getString(R.string.map_pref_key)).setEnabled(false);
                    else findPreference(getString(R.string.map_pref_key)).setEnabled(true);
            }
        } else {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Timber.d("onSharedPreferenceChanged, string: " + s);
        if (s.equals(getString(R.string.notify_hours_key)) ||
                s.equals(getString(R.string.pref_enable_notifications_key))) {
            SyncUtils.startImmediateSync(getActivity()); // TODO instead of parsing again, just query in make notifics
        }
        Preference preference = findPreference(s);
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {
                setPreferenceSummary(preference, sharedPreferences.getString(s, ""));
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        // register the preference change listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }
}
