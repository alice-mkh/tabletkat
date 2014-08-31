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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.exalm.tabletkat.OnPreferenceChangedListener;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.quicksettings.Row;
import org.exalm.tabletkat.quicksettings.RowFactory;

import java.util.ArrayList;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class SettingsView extends LinearLayout implements View.OnClickListener {

    static final String TAG = "SettingsView";

    BroadcastReceiver settingsReceiver;
    ArrayList<Row> rows;

    private static String[] defaultRows = new String[]{"airplane", "wifi", "rotate", "brightness", "dnd"};

    public SettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        rows = new ArrayList<Row>();
    }

    private void rebuild(String[] strings) {
        LinearLayout container = (LinearLayout) findViewById(TkR.id.settings_container);
        releaseControllers();
        container.removeAllViews();
        for (String s : strings){
            Row row = RowFactory.createRowFromString(getContext(), s);
            if (row == null){
                continue;
            }
            rows.add(row);
            container.addView(row.getView());
            container.addView(row.getSeparator());
        }
    }

    private void releaseControllers() {
        for (Row row : rows){
            row.releaseControllers();
        }
        rows.clear();
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
                String[] customRows = new String[]{"airplane", "wifi-switch", "rotate", "brightness", "dnd"}; //TODO: Customization UI
                rebuild(pref.getBoolean("extended_settings", false) ? customRows : defaultRows);
            }
        });
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

