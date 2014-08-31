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
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.widget.CompoundButton;

public class BluetoothController extends BroadcastReceiver
      implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "StatusBar.BluetoothController";

    private boolean mEnabled = false;
    private CompoundButton mCheckBox;
    private BluetoothManager mBluetoothManager;
    private Context mContext;

    public BluetoothController(Context context, CompoundButton checkbox) {
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(this, filter);
        mContext = context;

        mCheckBox = checkbox;
        checkbox.setChecked(mEnabled);
        checkbox.setOnCheckedChangeListener(this);

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            handleAdapterStateChange(adapter.getState());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            handleAdapterStateChange(
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
        }
    }

    private void handleAdapterStateChange(int adapterState) {
        boolean enabled = mEnabled;
        mEnabled = (adapterState == BluetoothAdapter.STATE_ON || adapterState == BluetoothAdapter.STATE_TURNING_ON);

        if (mEnabled != enabled) {
            mCheckBox.setChecked(mEnabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != mEnabled) {
            mEnabled = isChecked;
            unsafe(isChecked);
        }
    }

    private void unsafe(final boolean isChecked) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                if (isChecked) {
                    mBluetoothManager.getAdapter().enable();
                }else{
                    mBluetoothManager.getAdapter().disable();
                }
            }
        });
    }

    public void release() {
        mContext.unregisterReceiver(this);
    }
}
