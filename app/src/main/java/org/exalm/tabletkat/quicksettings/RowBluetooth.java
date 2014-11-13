package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.BluetoothController;

public class RowBluetooth extends Row {
    private BluetoothController mBluetoothController;

    public RowBluetooth(Context c) {
        super(c);
    }

    @Override
    protected int getLabel() {
        return TkR.string.status_bar_settings_bluetooth;
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_bluetooth;
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected String getOnClickAction() {
        return Settings.ACTION_BLUETOOTH_SETTINGS;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mBluetoothController = new BluetoothController(mContext, checkbox);
    }

    @Override
    public void releaseControllers() {
        mBluetoothController.release();
    }
}