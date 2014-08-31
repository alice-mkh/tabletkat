package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.LocationController;

import de.robv.android.xposed.XposedHelpers;

public class RowLocation extends Row {
    private LocationController mLocationController;

    public RowLocation(Context c) {
        super(c);
    }

    @Override
    public int getLabel() {
        return TkR.string.status_bar_settings_location;
    }

    @Override
    public int getIcon() {
        return TkR.drawable.ic_sysbar_location;
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected View.OnClickListener getOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
            }
        };
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mLocationController = new LocationController(mContext, checkbox);
    }

    @Override
    public void releaseControllers() {
        mLocationController.release();
    }
}