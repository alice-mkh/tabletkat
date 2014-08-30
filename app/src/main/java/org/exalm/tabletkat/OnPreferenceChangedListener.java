package org.exalm.tabletkat;

import de.robv.android.xposed.XSharedPreferences;

public interface OnPreferenceChangedListener {
    public void onPreferenceChanged(String key, boolean value);

    public void onPreferenceChanged(String key, int value);

    public void init(XSharedPreferences pref);
}
