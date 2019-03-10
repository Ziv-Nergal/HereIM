package fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import com.austingreco.imagelistpreference.ImageListPreference;

import gis.hereim.R;

import static activities.MainActivity.sCurrentFirebaseUser;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        EditTextPreference emailPreference = (EditTextPreference) getPreferenceManager().findPreference("pref_info_email");
        EditTextPreference fullNamePreference = (EditTextPreference) getPreferenceManager().findPreference("pref_info_full_name");
        EditTextPreference statusPreference = (EditTextPreference) getPreferenceManager().findPreference("pref_info_status");
        
        if (emailPreference != null) {
            emailPreference.setSummary(sCurrentFirebaseUser.GetEmailAddress());
        }

        if (fullNamePreference != null) {
            fullNamePreference.setSummary(sCurrentFirebaseUser.GetFullName());
            fullNamePreference.setOnPreferenceChangeListener(this);
        }

        if (statusPreference != null) {
            statusPreference.setSummary(sCurrentFirebaseUser.GetUserStatus());
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
        }

        return false;
    }

    private void changeFullName(Object newValue) {
        sCurrentFirebaseUser.SetFullName(newValue.toString());
    }

    private void changeStatus(Object newValue) {
        sCurrentFirebaseUser.SetUserStatus(newValue.toString());
    }
}
