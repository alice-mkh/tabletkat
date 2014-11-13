package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.provider.Settings;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;

public class RowSettings extends Row {
    public RowSettings(Context c) {
        super(c);
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_quicksettings;
    }

    @Override
    protected int getLabel() {
        return SystemR.string.status_bar_settings_settings_button;
    }

    @Override
    protected String getOnClickAction() {
        return Settings.ACTION_SETTINGS;
    }
}