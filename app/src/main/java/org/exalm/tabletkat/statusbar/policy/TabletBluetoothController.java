/*
 * Copyright (C) 2008 The Android Open Source Project
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

package org.exalm.tabletkat.statusbar.policy;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import org.exalm.tabletkat.SystemR;

import java.util.ArrayList;

public class TabletBluetoothController {
    private final Context mContext;
    private final Handler mHandler = new Handler();

    // bluetooth device status
    private boolean mBluetoothEnabled = false;

    private ArrayList<ImageView> views;

    private int mIconId;
    private String mContentDescription;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) ||
                    action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                updateBluetooth(intent);
            }
        }
    };

    public TabletBluetoothController(Context context) {
        mContext = context;

        views = new ArrayList<ImageView>();

        // listen for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);

        // bluetooth status
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mIconId = SystemR.drawable.stat_sys_data_bluetooth;
        if (adapter != null) {
            mBluetoothEnabled = (adapter.getState() == BluetoothAdapter.STATE_ON);
            if (adapter.getConnectionState() == BluetoothAdapter.STATE_CONNECTED) {
                mIconId = SystemR.drawable.stat_sys_data_bluetooth_connected;
            }
        }
    }

    public void addView(ImageView v){
        views.add(v);
        refreshView(v);
    }

    private void updateBluetooth(Intent intent) {
        mIconId = SystemR.drawable.stat_sys_data_bluetooth;
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            mBluetoothEnabled = state == BluetoothAdapter.STATE_ON;
        } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                BluetoothAdapter.STATE_DISCONNECTED);
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                mIconId = SystemR.drawable.stat_sys_data_bluetooth_connected;
                mContentDescription = mContext.getString(SystemR.string.accessibility_bluetooth_connected);
            } else {
                mContentDescription = mContext.getString(
                        SystemR.string.accessibility_bluetooth_disconnected);
            }
        } else {
            return;
        }
        refreshAllViews();
    }

    private void refreshAllViews() {
        for (ImageView v : views){
            refreshView(v);
        }
    }

    private void refreshView(ImageView v){
        v.setVisibility(mBluetoothEnabled ? View.VISIBLE : View.GONE);
        v.setImageDrawable(mContext.getResources().getDrawable(mIconId));
        v.setContentDescription(mContentDescription);
    }
}
