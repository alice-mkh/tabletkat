/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exalm.tabletkat.statusbar.tablet;

import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.AirplaneModeController;
import org.exalm.tabletkat.statusbar.policy.WifiController;
import org.exalm.tabletkat.statusbar.policy.DoNotDisturbController;
import org.exalm.tabletkat.statusbar.policy.RotationLockController;

import de.robv.android.xposed.XposedHelpers;

public class SettingsView extends LinearLayout implements View.OnClickListener {
    static final String TAG = "SettingsView";

    AirplaneModeController mAirplane;
    RotationLockController mRotationController;
	WifiController mWifiController;
    Object mBrightness;
    DoNotDisturbController mDoNotDisturb;
    View mRotationLockContainer;
    View mRotationLockSeparator;

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        final Context context = getContext();

        mAirplane = new AirplaneModeController(context,
                (CompoundButton) findViewById(TkR.id.airplane_checkbox));
        findViewById(TkR.id.network).setOnClickListener(this);

        mRotationLockContainer = findViewById(TkR.id.rotate);
        mRotationLockSeparator = findViewById(TkR.id.rotate_separator);
        mRotationController = new RotationLockController(context);
        mRotationController.addRotationLockControllerCallback(
                new RotationLockController.RotationLockControllerCallback() {
                    @Override
                    public void onRotationLockStateChanged(boolean locked, boolean visible) {
                        mRotationLockContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
                        mRotationLockSeparator.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }
                });
        CompoundButton rotateCheckbox = (CompoundButton) findViewById(TkR.id.rotate_checkbox);
        rotateCheckbox.setChecked(!mRotationController.isRotationLocked());
        rotateCheckbox.setVisibility(mRotationController.isRotationLockAffordanceVisible()
                ? View.VISIBLE : View.GONE);
        rotateCheckbox.setOnCheckedChangeListener(new CompoundButton. OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRotationController.setRotationLocked(!buttonView.isChecked());
            }
        });

        View slider = (View)XposedHelpers.newInstance(TabletKatModule.mToggleSliderClass, mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT);
        lp.weight = 1;
        lp.setMarginEnd(2);
        slider.setLayoutParams(lp);
        ((TextView) XposedHelpers.getObjectField(slider, "mLabel")).setText(SystemR.string.status_bar_settings_auto_brightness_label);
        ((ViewGroup) findViewById(TkR.id.brightness)).addView(slider);

        mBrightness = XposedHelpers.newInstance(TabletKatModule.mBrightnessControllerClass, context,
                (ImageView)findViewById(SystemR.id.brightness_icon),
                slider);
        mDoNotDisturb = new DoNotDisturbController(context,
                (CompoundButton)findViewById(TkR.id.do_not_disturb_checkbox));
		mWifiController = new WifiController(context,
				(CompoundButton)findViewById(TkR.id.network_checkbox));
        findViewById(TkR.id.settings).setOnClickListener(this);

        ((TextView) findViewById(TkR.id.airplane_label)).setText(SystemR.string.status_bar_settings_airplane);
        ((TextView) findViewById(TkR.id.network_label)).setText(SystemR.string.status_bar_settings_wifi_button);
        ((TextView) findViewById(TkR.id.rotate_label)).setText(SystemR.string.status_bar_settings_auto_rotation);
        ((TextView) findViewById(TkR.id.do_not_disturb_label)).setText(SystemR.string.status_bar_settings_notifications);
        ((TextView) findViewById(TkR.id.settings_label)).setText(SystemR.string.status_bar_settings_settings_button);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAirplane.release();
        mDoNotDisturb.release();
        mRotationController.release();
    }

    public void onClick(View v) {
        if (v.getId() == TkR.id.network){
            onClickNetwork();
        }
        if (v.getId() == TkR.id.settings){
            onClickSettings();
        }
    }

    private StatusBarManager getStatusBarManager() {
        return (StatusBarManager)getContext().getSystemService("statusbar");
    }

    // Network
    // ----------------------------
    private void onClickNetwork() {
        getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        getStatusBarManager().collapsePanels();
    }

    // Settings
    // ----------------------------
    private void onClickSettings() {
        getContext().startActivityAsUser(new Intent(Settings.ACTION_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                new UserHandle(UserHandle.USER_CURRENT));
        getStatusBarManager().collapsePanels();
    }
}

