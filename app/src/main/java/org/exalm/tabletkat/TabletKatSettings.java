package org.exalm.tabletkat;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.*;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.List;

public class TabletKatSettings extends PreferenceActivity {
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    public static final String ACTION_PREFERENCE_CHANGED = "org.exalm.tabletkat.PREFERENCE_CHANGED";

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pref_general);

        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_about);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_about);

        setUpPreferenceChangeListener(findPreference("enable_tablet_ui"));
        setUpPreferenceChangeListener(findPreference("enable_mod_recents"));
        setUpPreferenceChangeListener(findPreference("overlay_recents"));
        setUpPreferenceChangeListener(findPreference("extended_settings"));
        setUpPreferenceChangeListener(findPreference("ics_clock_font"));
        setUpPreferenceChangeListener(findPreference("battery_percents"));

        if (getString(R.string.pref_summary_translation).isEmpty()) {
            getPreferenceScreen().removePreference(findPreference("translation"));
        }
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS || !isXLargeTablet(context);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener sPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Intent i = new Intent(ACTION_PREFERENCE_CHANGED);
            i.putExtra("key", preference.getKey());
            if (newValue instanceof Boolean){
                i.putExtra("boolValue", (Boolean) newValue);
            }
            if (newValue instanceof Integer){
                i.putExtra("intValue", (Integer) newValue);
            }
            i.putExtra("stringValue", "" + newValue);
            preference.getContext().sendBroadcast(i);
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private static void setUpPreferenceChangeListener(Preference preference) {
        preference.setOnPreferenceChangeListener(sPreferenceChangeListener);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.pref_general);

            setUpPreferenceChangeListener(findPreference("enable_tablet_ui"));
            setUpPreferenceChangeListener(findPreference("enable_mod_recents"));
            setUpPreferenceChangeListener(findPreference("overlay_recents"));
            setUpPreferenceChangeListener(findPreference("extended_settings"));
            setUpPreferenceChangeListener(findPreference("ics_clock_font"));
            setUpPreferenceChangeListener(findPreference("battery_percents"));
        }
    }

    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.pref_about);

            if (getString(R.string.pref_summary_translation).isEmpty()) {
                getPreferenceScreen().removePreference(findPreference("translation"));
            }
        }
    }
}
