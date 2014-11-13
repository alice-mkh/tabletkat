package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.statusbar.policy.WifiController;

public class RowWifiSwitch extends RowWifi {
    private WifiController mWifiController;

    public RowWifiSwitch(Context c) {
        super(c);
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mWifiController = new WifiController(mContext, checkbox);
    }

    @Override
    public void releaseControllers() {
        mWifiController.release();
    }
}