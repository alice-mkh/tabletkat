package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;

public class RowWifi extends Row {
    public RowWifi(Context c) {
        super(c);
    }

    @Override
    public int getLabel() {
        return SystemR.string.status_bar_settings_wifi_button;
    }

    @Override
    public int getIcon() {
        return TkR.drawable.ic_sysbar_wifi;
    }

    @Override
    public View.OnClickListener getOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
            }
        };
    }
}