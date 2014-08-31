package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.RotationLockController;

import de.robv.android.xposed.XposedHelpers;

public class RowRotate extends Row {
    private RotationLockController mRotationController;
    private View mRow;
    private View mSeparator;

    public RowRotate(Context c) {
        super(c);
    }

    @Override
    public int getLabel() {
        return SystemR.string.status_bar_settings_auto_rotation;
    }

    @Override
    public int getIcon() {
        return TkR.drawable.ic_sysbar_rotate_on;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mRotationController = new RotationLockController(mContext);
        mRotationController.addRotationLockControllerCallback(
            new RotationLockController.RotationLockControllerCallback() {
                @Override
                public void onRotationLockStateChanged(boolean locked, boolean visible) {
                    mRow.setVisibility(visible ? View.VISIBLE : View.GONE);
                    mSeparator.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            }
        );

        checkbox.setChecked(!mRotationController.isRotationLocked());
        checkbox.setVisibility(mRotationController.isRotationLockAffordanceVisible()
                ? View.VISIBLE : View.GONE);
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRotationController.setRotationLocked(!buttonView.isChecked());
            }
        });
    }

    @Override
    public void releaseControllers() {
        mRotationController.release();
        mRow = null;
        mSeparator = null;
    }

    @Override
    public View getView() {
        View v = super.getView();
        mRow = v;
        return v;
    }

    @Override
    public View getSeparator() {
        View v = super.getSeparator();
        mSeparator = v;
        return v;
    }
}