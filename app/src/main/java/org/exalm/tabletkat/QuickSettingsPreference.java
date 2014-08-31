package org.exalm.tabletkat;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class QuickSettingsPreference extends DialogPreference {
    public QuickSettingsPreference(Context context) {
        this(context, null);
    }

    public QuickSettingsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickSettingsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.quick_settings_pref_dialog);
    }
}
