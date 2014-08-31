package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.AirplaneModeController;

import de.robv.android.xposed.XposedHelpers;

public class RowAirplane extends Row {
    private AirplaneModeController mAirplane;

    public RowAirplane(Context c) {
        super(c);
    }

    @Override
    public int getLabel() {
        return SystemR.string.status_bar_settings_airplane;
    }

    @Override
    public int getIcon() {
        return TkR.drawable.ic_sysbar_airplane_on;
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mAirplane = new AirplaneModeController(mContext, checkbox);
    }

    @Override
    public void releaseControllers() {
        mAirplane.release();
    }
}