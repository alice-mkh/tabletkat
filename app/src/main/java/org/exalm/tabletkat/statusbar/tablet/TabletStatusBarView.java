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

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;

public class TabletStatusBarView extends FrameLayout {
    private Handler mHandler;

    private final int MAX_PANELS = 5;
    private final View[] mIgnoreChildren = new View[MAX_PANELS];
    private final View[] mPanels = new View[MAX_PANELS];
    private final int[] mPos = new int[2];
    private Object mDelegateHelper;

    private final TabletStatusBarTransitions mBarTransitions;

    public TabletStatusBarView(Context context, Object service) {
        this(context, null, service);
    }

    public TabletStatusBarView(Context context, AttributeSet attrs, Object barService) {
        super(context, attrs);
        mDelegateHelper = XposedHelpers.newInstance(TabletKatModule.mDelegateViewHelperClass, this);
        setBackgroundResource(SystemR.drawable.system_bar_background);
        mBarTransitions = new TabletStatusBarTransitions(this, barService);
    }

    public void setDelegateView(View view) {
        XposedHelpers.callMethod(mDelegateHelper, "setDelegateView", view);
    }

    public void setBar(Object phoneStatusBar) {
        XposedHelpers.callMethod(mDelegateHelper, "setBar", phoneStatusBar);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBarTransitions.reloadBackground();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDelegateHelper != null) {
            XposedHelpers.callMethod(mDelegateHelper, "onInterceptTouchEvent", event);
        }
        return true;
    }

    public TabletStatusBarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Find the view we wish to grab events from in order to detect search gesture.
        // Depending on the device, this will be one of the id's listed below.
        // If we don't find one, we'll use the view provided in the constructor above (this view).

        View view = findViewById(TkR.id.navigationArea);

        XposedHelpers.callMethod(mDelegateHelper, "setSourceView", view);
        XposedHelpers.callMethod(mDelegateHelper, "setInitialTouchRegion", new Object[]{new View[]{view}});
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (TabletStatusBarMod.DEBUG) {
                Log.d(TabletStatusBarMod.TAG, "TabletStatusBarView intercepting touch event: " + ev);
            }

            mHandler.removeMessages(TabletStatusBarMod.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBarMod.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.removeMessages(TabletStatusBarMod.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBarMod.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.removeMessages(TabletStatusBarMod.MSG_STOP_TICKER);
            mHandler.sendEmptyMessage(TabletStatusBarMod.MSG_STOP_TICKER);

            for (int i=0; i < mPanels.length; i++) {
                if (mPanels[i] != null && mPanels[i].getVisibility() == View.VISIBLE) {
                    if (eventInside(mIgnoreChildren[i], ev)) {
                        if (TabletStatusBarMod.DEBUG) {
                            Log.d(TabletStatusBarMod.TAG,
                                    "TabletStatusBarView eating event for view: "
                                    + mIgnoreChildren[i]);
                        }
                        return true;
                    }
                }
            }
        }
        if (TabletStatusBarMod.DEBUG) {
            Log.d(TabletStatusBarMod.TAG, "TabletStatusBarView not intercepting event");
        }
        boolean b = (Boolean) XposedHelpers.callMethod(mDelegateHelper, "onInterceptTouchEvent", ev);
        if (mDelegateHelper != null && b) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean eventInside(View v, MotionEvent ev) {
        // assume that x and y are window coords because we are.
        final int x = (int)ev.getX();
        final int y = (int)ev.getY();

        final int[] p = mPos;
        v.getLocationInWindow(p);

        final int l = p[0];
        final int t = p[1];
        final int r = p[0] + v.getWidth();
        final int b = p[1] + v.getHeight();

        return x >= l && x < r && y >= t && y < b;
    }

    public void setHandler(Handler h) {
        mHandler = h;
    }

    /**
     * Let the status bar know that if you tap on ignore while panel is showing, don't do anything.
     *
     * Debounces taps on, say, a popup's trigger when the popup is already showing.
     */
    public void setIgnoreChildren(int index, View ignore, View panel) {
        mIgnoreChildren[index] = ignore;
        mPanels[index] = panel;
    }

    public View getBackButton() {
        return findViewById(SystemR.id.back);
    }

    public View getHomeButton() {
        return findViewById(SystemR.id.home);
    }

    public View getRecentsButton() {
        return findViewById(SystemR.id.recent_apps);
    }

    public View getMenuButton() {
        return findViewById(SystemR.id.menu);
    }
}
