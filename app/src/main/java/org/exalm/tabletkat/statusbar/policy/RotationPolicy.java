/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;

import org.exalm.tabletkat.TabletKatModule;

import de.robv.android.xposed.XposedHelpers;

/**
 * Provides helper functions for configuring the display rotation policy.
 */
public final class RotationPolicy {
    private static final String TAG = "RotationPolicy";

    private RotationPolicy() {
    }

    /**
     * Gets whether the device supports rotation. In general such a
     * device has an accelerometer and has the portrait and landscape
     * features.
     *
     * @param context Context for accessing system resources.
     * @return Whether the device supports rotation.
     */
    public static boolean isRotationSupported(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
                && pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_PORTRAIT)
                && pm.hasSystemFeature(PackageManager.FEATURE_SCREEN_LANDSCAPE);
    }

    /**
     * Returns true if the device supports the rotation-lock toggle feature
     * in the system UI or system bar.
     *
     * When the rotation-lock toggle is supported, the "auto-rotate screen" option in
     * Display settings should be hidden, but it should remain available in Accessibility
     * settings.
     */
    public static boolean isRotationLockToggleSupported(Context context) {
        return isRotationSupported(context)
                && context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /**
     * Returns true if the rotation-lock toggle should be shown in the UI.
     */
    public static boolean isRotationLockToggleVisible(Context context) {
        String HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY = (String) XposedHelpers.getStaticObjectField(Settings.System.class, "HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY");

        return isRotationLockToggleSupported(context) &&
                (Integer) XposedHelpers.callStaticMethod(Settings.System.class, "getIntForUser", context.getContentResolver(),
                        HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0,
                        XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT")) == 0;
    }

    /**
     * Returns true if rotation lock is enabled.
     */
    public static boolean isRotationLocked(Context context) {
        return (Integer) XposedHelpers.callStaticMethod(Settings.System.class, "getIntForUser", context.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0, XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT")) == 0;
    }

    /**
     * Enables or disables rotation lock.
     *
     * Should be used by the rotation lock toggle.
     */
    public static void setRotationLock(Context context, final boolean enabled) {
        String HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY = (String) XposedHelpers.getStaticObjectField(Settings.System.class, "HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY");

        XposedHelpers.callStaticMethod(Settings.System.class, "putIntForUser", context.getContentResolver(),
                HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY, 0,
                XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT"));

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object wm = XposedHelpers.callStaticMethod(TabletKatModule.mWindowManagerGlobalClass, "getWindowManagerService");
                    if (enabled) {
                        XposedHelpers.callMethod(wm, "freezeRotation", -1);
                    } else {
                        XposedHelpers.callMethod(wm, "thawRotation");
                    }
                } catch (Exception exc) {
                    Log.w(TAG, "Unable to save auto-rotate setting");
                }
            }
        });
    }

    /**
     * Enables or disables rotation lock and adjusts whether the rotation lock toggle
     * should be hidden for accessibility purposes.
     *
     * Should be used by Display settings and Accessibility settings.
     */
    public static void setRotationLockForAccessibility(Context context, final boolean enabled) {
        XposedHelpers.callStaticMethod(Settings.System.class, "putIntForUser", context.getContentResolver(),
                XposedHelpers.getStaticObjectField(Settings.System.class, "HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY"), enabled ? 1 : 0,
                XposedHelpers.getStaticIntField(UserHandle.class, "USER_CURRENT"));

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object wm = XposedHelpers.callStaticMethod(TabletKatModule.mWindowManagerGlobalClass, "getWindowManagerService");
                    if (enabled) {
                        XposedHelpers.callMethod(wm, "freezeRotation", Surface.ROTATION_0);
                    } else {
                        XposedHelpers.callMethod(wm, "thawRotation");
                    }
                } catch (Exception exc) {
                    Log.w(TAG, "Unable to save auto-rotate setting");
                }
            }
        });
    }

    /**
     * Registers a listener for rotation policy changes affecting the caller's user
     */
    public static void registerRotationPolicyListener(Context context,
            RotationPolicyListener listener) {
        registerRotationPolicyListener(context, listener, (Integer) XposedHelpers.callStaticMethod(UserHandle.class, "getCallingUserId"));
    }

    /**
     * Registers a listener for rotation policy changes affecting a specific user,
     * or USER_ALL for all users.
     */
    public static void registerRotationPolicyListener(Context context,
            RotationPolicyListener listener, int userHandle) {
        String HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY = (String) XposedHelpers.getStaticObjectField(Settings.System.class, "HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY");

        XposedHelpers.callMethod(context.getContentResolver(), "registerContentObserver", Settings.System.getUriFor(
                Settings.System.ACCELEROMETER_ROTATION),
                false, listener.mObserver, userHandle);
        XposedHelpers.callMethod(context.getContentResolver(), "registerContentObserver", Settings.System.getUriFor(
                        HIDE_ROTATION_LOCK_TOGGLE_FOR_ACCESSIBILITY),
                false, listener.mObserver, userHandle);
    }

    /**
     * Unregisters a listener for rotation policy changes.
     */
    public static void unregisterRotationPolicyListener(Context context,
            RotationPolicyListener listener) {
        context.getContentResolver().unregisterContentObserver(listener.mObserver);
    }

    /**
     * Listener that is invoked whenever a change occurs that might affect the rotation policy.
     */
    public static abstract class RotationPolicyListener {
        final ContentObserver mObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange, Uri uri) {
                RotationPolicyListener.this.onChange();
            }
        };

        public abstract void onChange();
    }
}