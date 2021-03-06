package fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Switch;

import androidx.annotation.Nullable;

import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference emailPreference = (EditTextPreference) getPreferenceManager()
                .findPreference("pref_info_email");
        EditTextPreference fullNamePreference = (EditTextPreference) getPreferenceManager()
                .findPreference("pref_info_full_name");
        EditTextPreference statusPreference = (EditTextPreference) getPreferenceManager()
                .findPreference("pref_info_status");

        SwitchPreference locationPregerence = (SwitchPreference) getPreferenceManager()
                .findPreference("pref_location");

        if(locationPregerence != null) {
            locationPregerence.setOnPreferenceChangeListener(this);
        }

        if (emailPreference != null) {
            emailPreference.setSummary(sCurrentFirebaseUser.getEmailAddress());
        }

        if (fullNamePreference != null) {
            fullNamePreference.setSummary(sCurrentFirebaseUser.getFullName());
            fullNamePreference.setOnPreferenceChangeListener(this);
        }

        if (statusPreference != null) {
            statusPreference.setSummary(sCurrentFirebaseUser.getUserStatus());
            statusPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        preference.setSummary(newValue.toString());

        switch (preference.getKey()){
            case "pref_info_full_name":
                changeFullName(newValue);
                break;
            case "pref_info_status":
                changeStatus(newValue);
                break;
            case "pref_location":
                ((SwitchPreference)preference).setChecked((boolean) newValue);
                sCurrentFirebaseUser.setSharingLocation((boolean) newValue);
                break;
        }

        return false;
    }

    private void changeFullName(Object newValue) {
        sCurrentFirebaseUser.setFullName(newValue.toString());
    }

    private void changeStatus(Object newValue) {
        sCurrentFirebaseUser.setUserStatus(newValue.toString());
    }
}
