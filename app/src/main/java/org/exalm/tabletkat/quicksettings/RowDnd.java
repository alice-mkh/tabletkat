package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.DoNotDisturbController;

public class RowDnd extends Row {
    private DoNotDisturbController mDoNotDisturb;

    public RowDnd(Context c) {
        super(c);
    }

    @Override
    public int getLabel() {
        return SystemR.string.status_bar_settings_notifications;
    }

    @Override
    public int getIcon() {
        return TkR.drawable.ic_notification_open;
    }

    @Override
    protected boolean hasSwitch() {
        return true;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mDoNotDisturb = new DoNotDisturbController(mContext, checkbox);
    }

    @Override
    public void releaseControllers() {
        mDoNotDisturb.release();
    }
}