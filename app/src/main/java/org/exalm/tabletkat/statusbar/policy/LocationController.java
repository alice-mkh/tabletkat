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

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;

public class LocationController extends BroadcastReceiver
      implements CompoundButton.OnCheckedChangeListener{
    private static final String TAG = "StatusBar.LocationController";

    private boolean mEnabled = false;
    private CompoundButton mCheckBox;
    private LocationManager mLocationManager;
    private Context mContext;

    public LocationController(Context context, CompoundButton checkbox) {
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        context.registerReceiver(this, filter);
        mContext = context;

        mCheckBox = checkbox;
        checkbox.setChecked(mEnabled);
        checkbox.setOnCheckedChangeListener(this);

        handleStateChange(isLocationEnabled());
    }

    private boolean isLocationEnabled(){
        ContentResolver resolver = mContext.getContentResolver();

        int mode = Settings.Secure.getIntForUser(resolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF, ActivityManager.getCurrentUser());
        return mode != Settings.Secure.LOCATION_MODE_OFF;
    }

    private boolean isUserLocationRestricted(int userId) {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(
                UserManager.DISALLOW_SHARE_LOCATION,
                new UserHandle(userId));
    }

    private void setLocationEnabled(final boolean enabled) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int currentUserId = ActivityManager.getCurrentUser();
                if (isUserLocationRestricted(currentUserId)) {
                    return;
                }
                final ContentResolver cr = mContext.getContentResolver();

                int mode = enabled
                        ? Settings.Secure.LOCATION_MODE_HIGH_ACCURACY : Settings.Secure.LOCATION_MODE_OFF;

                Settings.Secure
                        .putIntForUser(cr, Settings.Secure.LOCATION_MODE, mode, currentUserId);
            }
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            handleStateChange(isLocationEnabled());
        }
    }

    private void handleStateChange(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            mCheckBox.setChecked(mEnabled);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != mEnabled) {
            setLocationEnabled(isChecked);
        }
    }

    public void release() {
        mContext.unregisterReceiver(this);
    }
}
