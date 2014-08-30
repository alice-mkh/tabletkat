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

package org.exalm.tabletkat.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.widget.CompoundButton;

public class WifiController extends BroadcastReceiver
        implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "StatusBar.WifiController";

    private Context mContext;
    private CompoundButton mCheckBox;

    private boolean mWifiEnabled;
	private WifiManager mWifiManager;

    public WifiController( Context context, CompoundButton checkbox ) {
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mContext = context;
		mWifiEnabled = getWifiEnabled();
        mCheckBox = checkbox;
        checkbox.setChecked(mWifiEnabled);
        checkbox.setOnCheckedChangeListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        context.registerReceiver(this, filter);

    }

    public void release() {
        mContext.unregisterReceiver(this);
    }

    public void onCheckedChanged(CompoundButton view, boolean checked) {
        if (checked != mWifiEnabled) {
			mWifiEnabled = checked;
            unsafe(checked);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            final boolean enabled = mWifiManager.isWifiEnabled();
            if (enabled != mWifiEnabled) {
				mWifiEnabled = enabled;
                mCheckBox.setChecked(enabled);
            }
        }
    }

    private boolean getWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    private void unsafe(final boolean enabled) {
        AsyncTask.execute(new Runnable() {
                public void run() {
					ContentResolver cr = mContext.getContentResolver();

					mWifiManager.setWifiApEnabled(null, false);
					mWifiManager.setWifiEnabled(enabled);
                }
            });
    }
}

