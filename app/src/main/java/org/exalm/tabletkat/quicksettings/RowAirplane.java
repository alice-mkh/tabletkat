package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.AirplaneModeController;

public class RowAirplane extends Row {
    private AirplaneModeController mAirplane;

    public RowAirplane(Context c) {
        super(c);
    }

    @Override
    protected int getLabel() {
        return SystemR.string.status_bar_settings_airplane;
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_airplane;
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