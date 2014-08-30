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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.OnPreferenceChangedListener;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.policy.AirplaneModeController;
import org.exalm.tabletkat.statusbar.policy.DoNotDisturbController;
import org.exalm.tabletkat.statusbar.policy.RotationLockController;
import org.exalm.tabletkat.statusbar.policy.WifiController;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class SettingsView extends LinearLayout implements View.OnClickListener {

    static final String TAG = "SettingsView";

    AirplaneModeController mAirplane;
    WifiController mWifiController;
    RotationLockController mRotationController;
    Object mBrightness;
    DoNotDisturbController mDoNotDisturb;
    View mRotationLockContainer;
    View mRotationLockSeparator;
    BroadcastReceiver settingsReceiver;

    private static String[] defaultRows = new String[]{"airplane", "wifi", "rotation", "brightness", "dnd"};

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void rebuild(String[] strings) {
        LinearLayout container = (LinearLayout) findViewById(TkR.id.settings_container);
        releaseControllers();
        container.removeAllViews();
        for (String s : strings){
            View row = createRowFromString(s);
            if (row == null){
                continue;
            }
            container.addView(row);

            View sep = makeSeparator();
            container.addView(sep);
            if (s.equals("rotation")){
                mRotationLockSeparator = sep;
            }
        }
    }

    private View createRowFromString(String id) {
        LinearLayout view = new LinearLayout(getContext());

        DisplayMetrics d = getContext().getResources().getDisplayMetrics();
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, d);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, d);
        view.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        view.setPaddingRelative(0, 0, padding, 0);

        LinearLayout row = (LinearLayout) inflate(getContext(), TkR.layout.system_bar_settings_row, view);
        row = (LinearLayout) row.getChildAt(0);
        TextView label = (TextView) row.findViewById(TkR.id.row_label);
        ImageView icon = (ImageView) row.findViewById(TkR.id.row_icon);
        Switch checkbox = (Switch) row.findViewById(TkR.id.row_checkbox);

        XposedBridge.log(id);

        if (id.equals("airplane")){
            mAirplane = new AirplaneModeController(getContext(), checkbox);
            icon.setImageResource(TkR.drawable.ic_sysbar_airplane_on);
            label.setText(SystemR.string.status_bar_settings_airplane);
        }
        if (id.startsWith("wifi")){
            icon.setImageResource(TkR.drawable.ic_sysbar_wifi_on);
            row.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickNetwork();
                }
            });
            label.setText(SystemR.string.status_bar_settings_wifi_button);
            if (id.equals("wifi-switch")){
                mWifiController = new WifiController(getContext(), checkbox);
            }else{
                row.removeView(checkbox);
            }
        }
        if (id.equals("rotation")){
            mRotationLockContainer = row;
            mRotationController = new RotationLockController(getContext());
            mRotationController.addRotationLockControllerCallback(
                    new RotationLockController.RotationLockControllerCallback() {
                        @Override
                        public void onRotationLockStateChanged(boolean locked, boolean visible) {
                            if (mRotationLockSeparator == null | mRotationLockContainer == null){
                                return;
                            }
                            mRotationLockContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
                            mRotationLockSeparator.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }
                    });

            checkbox.setChecked(!mRotationController.isRotationLocked());
            checkbox.setVisibility(mRotationController.isRotationLockAffordanceVisible()
                    ? View.VISIBLE : View.GONE);
            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mRotationController.setRotationLocked(!buttonView.isChecked());
                }
            });

            icon.setImageResource(TkR.drawable.ic_sysbar_rotate_on);
            label.setText(SystemR.string.status_bar_settings_auto_rotation);
        }
        if (id.equals("brightness")){
            row.removeView(checkbox);
            row.removeView(label);

            View slider = (View) XposedHelpers.newInstance(TabletKatModule.mToggleSliderClass, getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT);
            lp.weight = 1;
            lp.setMarginEnd((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, d));
            slider.setLayoutParams(lp);
            ((TextView) XposedHelpers.getObjectField(slider, "mLabel")).setText(SystemR.string.status_bar_settings_auto_brightness_label);
            row.addView(slider);

            mBrightness = XposedHelpers.newInstance(TabletKatModule.mBrightnessControllerClass, getContext(), icon, slider);
        }
        if (id.equals("dnd")){
            mDoNotDisturb = new DoNotDisturbController(getContext(), checkbox);
            icon.setImageResource(TkR.drawable.ic_notification_open);
            label.setText(SystemR.string.status_bar_settings_notifications);
        }
        return view;
    }

    private View makeSeparator() {
        View v = new View(getContext());
        v.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
        return v;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        findViewById(TkR.id.settings).setOnClickListener(this);
        ((TextView) findViewById(TkR.id.settings_label)).setText(SystemR.string.status_bar_settings_settings_button);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        settingsReceiver = TabletKatModule.registerReceiver(getContext(), new OnPreferenceChangedListener() {
            @Override
            public void onPreferenceChanged(String key, boolean value) {
            }

            @Override
            public void onPreferenceChanged(String key, int value) {
            }

            @Override
            public void init(XSharedPreferences pref) {
                String[] customRows = new String[]{"airplane", "wifi-switch", "brightness", "rotation", "dnd"}; //TODO: Customization UI
                rebuild(pref.getBoolean("extended_settings", false) ? customRows : defaultRows);
            }
        });
    }

    private void releaseControllers() {
        if (mAirplane != null) {
            mAirplane.release();
            mAirplane = null;
        }
        if (mWifiController != null) {
            mWifiController.release();
            mWifiController = null;
        }
        if (mDoNotDisturb != null) {
            mDoNotDisturb.release();
            mDoNotDisturb = null;
        }
        if (mRotationController != null) {
            mRotationController.release();
            mRotationController = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseControllers();
        if (settingsReceiver != null) {
            getContext().unregisterReceiver(settingsReceiver);
            settingsReceiver = null;
        }
    }

    public void onClick(View v) {
        if (v.getId() == TkR.id.settings){
            onClickSettings();
        }
    }

    private Object getStatusBarManager() {
        return getContext().getSystemService("statusbar");
    }

    // Network
    // ----------------------------
    private void onClickNetwork() {
        getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
    }

    // Settings
    // ----------------------------
    private void onClickSettings() {
        int USER_CURRENT = XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT");
        Intent i = new Intent(Settings.ACTION_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        UserHandle h = (UserHandle) XposedHelpers.newInstance(UserHandle.class, USER_CURRENT);
        XposedHelpers.callMethod(getContext(), "startActivityAsUser", i, h);
        XposedHelpers.callMethod(getStatusBarManager(), "collapsePanels");
    }
}

