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

package org.exalm.tabletkat.statusbar;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RemoteViews;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.statusbar.tablet.StatusBarPanel;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class BaseStatusBarMod implements IMod {
    public static final boolean DEBUG = false;
    public static final String TAG = "BaseStatusBarMod";
    public static final boolean MULTIUSER_DEBUG = false;

    protected static Object self;

    protected Context mContext;

    public static final int EXPANDED_LEAVE_ALONE = -10000;
    protected static final int MSG_TOGGLE_RECENTS_PANEL = 1020;
    protected static final int MSG_CLOSE_RECENTS_PANEL = 1021;
    protected static final int MSG_OPEN_SEARCH_PANEL = 1024;
    protected static final int MSG_CLOSE_SEARCH_PANEL = 1025;
    protected static final int MSG_HIDE_HEADS_UP = 1027;
    protected static final boolean ENABLE_HEADS_UP = true;
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    protected Object mNotificationData;
    protected WindowManager mWindowManager;
    protected Handler mHandler;
    protected View.OnTouchListener mRecentsPreloadOnTouchListener;
    protected ViewGroup mPile;
    protected Object mWindowManagerService;
    protected Object mBarService;
    protected Object mSearchPanelView;
    protected Object mCommandQueue;
    protected boolean mIsTv;

    public void init(Object statusBar){
        self = statusBar;
        mIsTv = TabletKatModule.mTvStatusBarClass.isInstance(self);
        mContext = (Context) XposedHelpers.getObjectField(self, "mContext");
    }

    public void reset(){
        mNotificationData = null;
        mWindowManager = null;
        mHandler = null;
        mRecentsPreloadOnTouchListener = null;
        mPile = null;
        mWindowManagerService = null;
        mBarService = null;
        mSearchPanelView = null;
        mCommandQueue = null;
        mContext = null;
        self = null;
    }

    @Override
    public void addHooks(ClassLoader cl) {
        Class tv = TabletKatModule.mTvStatusBarClass;
        Class base = TabletKatModule.mBaseStatusBarClass;

        XposedHelpers.findAndHookMethod(tv, "toggleRecentApps", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int msg = MSG_TOGGLE_RECENTS_PANEL;
                mHandler.removeMessages(msg);
                mHandler.sendEmptyMessage(msg);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "updateNotification", IBinder.class, StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                IBinder key = (IBinder)methodHookParam.args[0];
                StatusBarNotification notification = (StatusBarNotification)methodHookParam.args[1];
                Object mInterruptingNotificationEntry = XposedHelpers.getObjectField(self, "mInterruptingNotificationEntry");
                mRecentsPreloadOnTouchListener = (View.OnTouchListener) XposedHelpers.getObjectField(self, "mRecentsPreloadOnTouchListener");
                mPile = (ViewGroup) XposedHelpers.getObjectField(self, "mPile");

                if (DEBUG) Log.d(TAG, "updateNotification(" + key + " -> " + notification + ")");

                final Object oldEntry = XposedHelpers.callMethod(mNotificationData, "findByKey", key);
                if (oldEntry == null) {
                    Log.w(TAG, "updateNotification for unknown key: " + key);
                    return null;
                }

                final StatusBarNotification oldNotification = (StatusBarNotification)XposedHelpers.getObjectField(oldEntry, "notification");

                // XXX: modify when we do something more intelligent with the two content views
                final RemoteViews oldContentView = oldNotification.getNotification().contentView;
                final RemoteViews contentView = notification.getNotification().contentView;
                final RemoteViews oldBigContentView = oldNotification.getNotification().bigContentView;
                final RemoteViews bigContentView = notification.getNotification().bigContentView;

                Object row = XposedHelpers.getObjectField(oldEntry, "row");
                if (DEBUG) {
                    Object parent = XposedHelpers.callMethod(row, "getParent");
                    Log.d(TAG, "old notification: when=" + oldNotification.getNotification().when
                            + " ongoing=" + oldNotification.isOngoing()
                            + " expanded=" + XposedHelpers.getObjectField(oldEntry, "expanded")
                            + " contentView=" + oldContentView
                            + " bigContentView=" + oldBigContentView
                            + " rowParent=" + parent);
                    Log.d(TAG, "new notification: when=" + notification.getNotification().when
                            + " ongoing=" + oldNotification.isOngoing()
                            + " contentView=" + contentView
                            + " bigContentView=" + bigContentView);
                }

                // Can we just reapply the RemoteViews in place?  If when didn't change, the order
                // didn't change.

                // 1U is never null
                boolean contentsUnchanged = (XposedHelpers.getObjectField(oldEntry, "expanded") != null
                        && contentView.getPackage() != null
                        && oldContentView.getPackage() != null
                        && oldContentView.getPackage().equals(contentView.getPackage())
                        && oldContentView.getLayoutId() == contentView.getLayoutId());
                // large view may be null
                boolean bigContentsUnchanged =
                        (XposedHelpers.callMethod(oldEntry, "getBigContentView") == null && bigContentView == null)
                                || ((XposedHelpers.callMethod(oldEntry, "getBigContentView") != null && bigContentView != null)
                                && bigContentView.getPackage() != null
                                && oldBigContentView.getPackage() != null
                                && oldBigContentView.getPackage().equals(bigContentView.getPackage())
                                && oldBigContentView.getLayoutId() == bigContentView.getLayoutId());
                ViewGroup rowParent = (ViewGroup) XposedHelpers.callMethod(row, "getParent");
                int notificationScore = (Integer) XposedHelpers.callMethod(notification, "getScore");
                int oldNotificationScore = (Integer) XposedHelpers.callMethod(oldNotification, "getScore");
                boolean orderUnchanged = notification.getNotification().when== oldNotification.getNotification().when
                        && notificationScore == oldNotificationScore;
                // score now encompasses/supersedes isOngoing()

                StatusBarNotification s = (StatusBarNotification) XposedHelpers.getObjectField(oldEntry, "notification");
                boolean updateTicker = notification.getNotification().tickerText != null
                        && !TextUtils.equals(notification.getNotification().tickerText,
                        s.getNotification().tickerText);
                boolean isTopAnyway = (Boolean)XposedHelpers.callMethod(self, "isTopNotification", rowParent, oldEntry);
                if (contentsUnchanged && bigContentsUnchanged && (orderUnchanged || isTopAnyway)) {
                    if (DEBUG) Log.d(TAG, "reusing notification for key: " + key);
                    XposedHelpers.setObjectField(oldEntry, "notification", notification);
                    try {
                        updateNotificationViews(oldEntry, notification);

                        if (ENABLE_HEADS_UP && mInterruptingNotificationEntry != null
                                && oldNotification == XposedHelpers.getObjectField(mInterruptingNotificationEntry, "notification")) {
                            if (!(Boolean) XposedHelpers.callMethod(self, "shouldInterrupt", notification)) {
                                if (DEBUG) Log.d(TAG, "no longer interrupts!");
                                mHandler.sendEmptyMessage(MSG_HIDE_HEADS_UP);
                            } else {
                                if (DEBUG) Log.d(TAG, "updating the current heads up:" + notification);
                                XposedHelpers.setObjectField(mInterruptingNotificationEntry, "notification", notification);
                                updateNotificationViews(mInterruptingNotificationEntry, notification);
                            }
                        }

                        // Update the icon.
                        final Parcelable ic = (Parcelable) XposedHelpers.newInstance(TabletKatModule.mStatusBarIconClass,
                                notification.getPackageName(),
                                (UserHandle) XposedHelpers.callMethod(notification, "getUser"),
                                notification.getNotification().icon, notification.getNotification().iconLevel,
                                notification.getNotification().number,
                                notification.getNotification().tickerText);
                        Object icon = XposedHelpers.getObjectField(oldEntry, "icon");
                        if (!(Boolean) XposedHelpers.callMethod(icon, "set", ic)) {
                            XposedHelpers.callMethod(self, "handleNotificationError", key, notification, "Couldn't update icon: " + ic);
                            return null;
                        }
                        XposedHelpers.callMethod(self, "updateExpansionStates");
                    }
                    catch (RuntimeException e) {
                        // It failed to add cleanly.  Log, and remove the view from the panel.
                        Log.w(TAG, "Couldn't reapply views for package " + contentView.getPackage(), e);
                        removeNotificationViews(key);
                        addNotificationViews(key, notification);
                    }
                } else {
                    if (DEBUG) Log.d(TAG, "not reusing notification for key: " + key);
                    if (DEBUG) Log.d(TAG, "contents was " + (contentsUnchanged ? "unchanged" : "changed"));
                    if (DEBUG) Log.d(TAG, "order was " + (orderUnchanged ? "unchanged" : "changed"));
                    if (DEBUG) Log.d(TAG, "notification is " + (isTopAnyway ? "top" : "not top"));
                    final boolean wasExpanded = (Boolean) XposedHelpers.callMethod(row, "isUserExpanded");
                    removeNotificationViews(key);
                    addNotificationViews(key, notification);  // will also replace the heads up
                    if (wasExpanded) {
                        final Object newEntry = XposedHelpers.callMethod(mNotificationData, "findByKey", key);
                        Object nRow = XposedHelpers.getObjectField(newEntry, "row");
                        XposedHelpers.callMethod(nRow, "setExpanded", true);
                        XposedHelpers.callMethod(nRow, "setUserExpanded", true);
                    }
                }

                // Update the veto button accordingly (and as a result, whether this row is
                // swipe-dismissable)
                XposedHelpers.callMethod(self, "updateNotificationVetoButton", row, notification);

                // Is this for you?
                boolean isForCurrentUser = (Boolean)XposedHelpers.callMethod(self, "notificationIsForCurrentUser", notification);
                if (DEBUG) Log.d(TAG, "notification is " + (isForCurrentUser ? "" : "not ") + "for you");

                // Restart the ticker if it's still running
                if (updateTicker && isForCurrentUser) {
                    XposedHelpers.callMethod(self, "haltTicker");
                    XposedHelpers.callMethod(self, "tick", key, notification, false);
                }

                // Recalculate the position of the sliding windows and the titles.
                setAreThereNotifications();

                XposedHelpers.callMethod(self, "updateExpandedViewPos", EXPANDED_LEAVE_ALONE);

                return null;
            }
        });

        XposedHelpers.findAndHookMethod(base, "start", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsTv){
                    return;
                }

                mHandler = (Handler) XposedHelpers.getObjectField(self, "mHandler");
                mRecentsPreloadOnTouchListener = (View.OnTouchListener) XposedHelpers.getObjectField(self, "mRecentsPreloadOnTouchListener");
                mPile = (ViewGroup) XposedHelpers.getObjectField(self, "mPile");
                mBarService = XposedHelpers.getObjectField(self, "mBarService");
                mSearchPanelView = XposedHelpers.getObjectField(self, "mSearchPanelView");
                mCommandQueue = XposedHelpers.getObjectField(self, "mCommandQueue");
                onStart();
            }
        });
        //AOSPA hook
        try {
            XposedHelpers.findAndHookMethod(base, "updateHoverState", XC_MethodReplacement.DO_NOTHING);
        }catch (NoSuchMethodError e){}
    }

    protected void onStart() {}

    @Override
    public void initResources(XResources res, XModuleResources res2) {

    }

    protected boolean inKeyguardRestrictedInputMode() {
        try {
            return (Boolean) XposedHelpers.callMethod(self, "inKeyguardRestrictedInputMode");
        }catch (NoSuchMethodError e){
            return false;
        }
    }

    public void toggleRecentApps() {
        XposedHelpers.callMethod(self, "toggleRecentApps");
    }

    protected void updateSearchPanel() {
        try {
            XposedHelpers.callMethod(self, "updateSearchPanel");
        }catch(NoSuchMethodError e){}
    }

    protected void showSearchPanel() {
        XposedHelpers.callMethod(self, "showSearchPanel");
    }

    public boolean inflateViews(Object entry, ViewGroup parent) {
        return (Boolean) XposedHelpers.callMethod(self, "inflateViews", entry, parent);
    }

    protected void visibilityChanged(boolean visible) {
        XposedHelpers.callMethod(self, "visibilityChanged", visible);
    }

    protected View.OnLongClickListener getNotificationLongClicker() {
        return (View.OnLongClickListener) XposedHelpers.callMethod(self, "getNotificationLongClicker");
    }

    protected void addNotificationViews(IBinder key, StatusBarNotification notification) {
        addNotificationViews(XposedHelpers.callMethod(self, "createNotificationViews", key, notification));
    }

    protected void addNotificationViews(Object o) {
        XposedHelpers.callMethod(self, "addNotificationViews", o);
    }

    protected StatusBarNotification removeNotificationViews(IBinder key) {
        return (StatusBarNotification) XposedHelpers.callMethod(self, "removeNotificationViews", key);
    }

    protected void tick(IBinder key, StatusBarNotification n, boolean firstTime){
        XposedHelpers.callMethod(self, "tick", key, n, firstTime);
    }

    public void setSystemUiVisibility(int vis, int mask) {
        XposedHelpers.callMethod(self, "setSystemUiVisibility", vis, mask);
    }

    public boolean isDeviceProvisioned() {
        return (Boolean) XposedHelpers.callMethod(self, "isDeviceProvisioned");
    }

    protected void setAreThereNotifications() {
        XposedHelpers.callMethod(self, "setAreThereNotifications");
    }

    protected boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        return (Boolean) XposedHelpers.callMethod(self, "showNotificationEvenIfUnprovisioned", sbn);
    }

    protected void updateNotificationIcons(){
        XposedHelpers.callMethod(self, "updateNotificationIcons");
    }

    protected boolean shouldDisableNavbarGestures(){
        return (Boolean) XposedHelpers.callMethod(self, "shouldDisableNavbarGestures");
    }

    private void updateNotificationViews(Object entry,
                                         StatusBarNotification notification) {
        View expanded = (View) XposedHelpers.getObjectField(entry, "expanded");
        View eBigContentView = (View) XposedHelpers.callMethod(entry, "getBigContentView");
        View content = (View) XposedHelpers.getObjectField(entry, "content");

        final RemoteViews contentView = notification.getNotification().contentView;
        final RemoteViews bigContentView = notification.getNotification().bigContentView;

        // Reapply the RemoteViews
        contentView.reapply(mContext, expanded, mOnClickHandler);
        if (bigContentView != null && eBigContentView != null) {
            bigContentView.reapply(mContext, eBigContentView, mOnClickHandler);
        }
        // update the contentIntent
        final PendingIntent contentIntent = notification.getNotification().contentIntent;
        if (contentIntent != null) {
            final View.OnClickListener listener = makeClicker(contentIntent,
                    notification.getPackageName(), notification.getTag(), notification.getId());
            content.setOnClickListener(listener);
        } else {
            content.setOnClickListener(null);
        }
    }

    public View.OnClickListener makeClicker(PendingIntent p, String s, String tag, int id) {
        return (View.OnClickListener) XposedHelpers.callMethod(self, "makeClicker", p, s, tag, id);
    }

    private RemoteViews.OnClickHandler mOnClickHandler = new RemoteViews.OnClickHandler() {
        @Override
        public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent) {
            if (DEBUG) {
                Log.v(TAG, "Notification click handler invoked for intent: " + pendingIntent);
            }
            final boolean isActivity = (Boolean) XposedHelpers.callMethod(pendingIntent, "isActivity");
            if (isActivity) {
                try {
                    Object o = XposedHelpers.callStaticMethod(TabletKatModule.mActivityManagerNativeClass, "getDefault");
                    XposedHelpers.callMethod(o, "resumeAppSwitches");
                    XposedHelpers.callMethod(o, "dismissKeyguardOnNextActivity");
                } catch (Exception e) {
                }
            }

            boolean handled = super.onClickHandler(view, pendingIntent, fillInIntent);

            if (isActivity && handled) {
                // close the shade if it was open
                animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
                visibilityChanged(false);
            }
            return handled;
        }
    };

    public void animateCollapsePanels(int i) {
        XposedHelpers.callMethod(self, "animateCollapsePanels", i);

    }

    public class TouchOutsideListener implements View.OnTouchListener {
        private int mMsg;
        private StatusBarPanel mPanel;

        public TouchOutsideListener(int msg, StatusBarPanel panel) {
            mMsg = msg;
            mPanel = panel;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            final int action = ev.getAction();
            if (action == MotionEvent.ACTION_OUTSIDE
                    || (action == MotionEvent.ACTION_DOWN
                    && !mPanel.isInContentArea((int)ev.getX(), (int)ev.getY()))) {
                mHandler.removeMessages(mMsg);
                mHandler.sendEmptyMessage(mMsg);
                return true;
            }
            return false;
        }
    }
}


