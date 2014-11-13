package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.provider.Settings;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;

public class RowWifi extends Row {
    public RowWifi(Context c) {
        super(c);
    }

    @Override
    protected int getLabel() {
        return SystemR.string.status_bar_settings_wifi_button;
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_wifi;
    }

    @Override
    protected String getOnClickAction() {
        return Settings.ACTION_WIFI_SETTINGS;
    }
}