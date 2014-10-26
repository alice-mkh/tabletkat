package org.exalm.tabletkat.quicksettings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;

public class RowPowerOff extends Row {
    public RowPowerOff(Context c) {
        super(c);
    }

    @Override
    protected int getIcon() {
        return android.R.drawable.ic_lock_power_off;
    }

    @Override
    protected int getLabel() {
        return TkR.string.status_bar_settings_poweroff;
    }

    @Override
    protected View.OnClickListener getOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int power_off = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStringClass, "power_off");
                int shutdown_confirm_question = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStringClass, "shutdown_confirm_question");
                int yes = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStringClass, "yes");
                int no = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStringClass, "no");
                XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
                // Create dialog to get user confirmation
                final AlertDialog dialog = new AlertDialog.Builder(mContext)
                            .setTitle(power_off)
                            .setMessage(shutdown_confirm_question)
                            .setPositiveButton(yes,
                                new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Send request to start ShutdownActivity
                                    Intent intent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
                                    intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                }
                            })
                            .setNegativeButton(no, null)
                            .create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
            }
        };
    }
}