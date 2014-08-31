package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;

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
    protected View.OnClickListener getOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int USER_CURRENT = XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT");
                Intent i = new Intent(Settings.ACTION_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                UserHandle h = (UserHandle) XposedHelpers.newInstance(UserHandle.class, USER_CURRENT);
                XposedHelpers.callMethod(mContext, "startActivityAsUser", i, h);
                XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
            }
        };
    }
}