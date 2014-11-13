package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.LocationController;

public class RowLocation extends Row {
    private LocationController mLocationController;

    public RowLocation(Context c) {
        super(c);
    }

    @Override
    protected int getLabel() {
        return TkR.string.status_bar_settings_location;
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_location;
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected String getOnClickAction() {
        return Settings.ACTION_LOCATION_SOURCE_SETTINGS;
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