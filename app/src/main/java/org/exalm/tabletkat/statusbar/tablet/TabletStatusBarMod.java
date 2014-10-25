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

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.exalm.tabletkat.CustomDimenReplacement;
import org.exalm.tabletkat.OnPreferenceChangedListener;
import org.exalm.tabletkat.R;
import org.exalm.tabletkat.StatusBarManager;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.ViewConstants;
import org.exalm.tabletkat.ViewHelper;
import org.exalm.tabletkat.statusbar.BaseStatusBarMod;
import org.exalm.tabletkat.statusbar.CommandQueue;
import org.exalm.tabletkat.statusbar.DoNotDisturb;
import org.exalm.tabletkat.statusbar.phone.BarTransitions;
import org.exalm.tabletkat.statusbar.policy.BatteryPercentView;
import org.exalm.tabletkat.statusbar.policy.CompatModeButton;
import org.exalm.tabletkat.statusbar.policy.EventHole;
import org.exalm.tabletkat.statusbar.policy.Prefs;
import org.exalm.tabletkat.statusbar.policy.TabletBluetoothController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

/*
 Transforms TvStatusBar into a TabletStatusBar
 */
public class TabletStatusBarMod extends BaseStatusBarMod implements
        InputMethodsPanel.OnHardKeyboardEnabledChangeListener {
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_COMPAT_HELP = false;
    public static final String TAG = "TabletStatusBarMod";

    public static final int MSG_OPEN_NOTIFICATION_PANEL = 1000;
    public static final int MSG_CLOSE_NOTIFICATION_PANEL = 1001;
    public static final int MSG_OPEN_NOTIFICATION_PEEK = 1002;
    public static final int MSG_CLOSE_NOTIFICATION_PEEK = 1003;
    // 1020-1030 reserved for BaseStatusBar
    public static final int MSG_SHOW_CHROME = 1031;
    public static final int MSG_HIDE_CHROME = 1032;
    public static final int MSG_OPEN_INPUT_METHODS_PANEL = 1040;
    public static final int MSG_CLOSE_INPUT_METHODS_PANEL = 1041;
    public static final int MSG_OPEN_COMPAT_MODE_PANEL = 1050;
    public static final int MSG_CLOSE_COMPAT_MODE_PANEL = 1051;
    public static final int MSG_STOP_TICKER = 2000;

    // Fitts' Law assistance for LatinIME; see policy.EventHole
    private static final boolean FAKE_SPACE_BAR = true;

    // Notification "peeking" (flyover preview of individual notifications)
    final static boolean NOTIFICATION_PEEK_ENABLED = false;
    final static int NOTIFICATION_PEEK_HOLD_THRESH = 200; // ms
    final static int NOTIFICATION_PEEK_FADE_DELAY = 3000; // ms
    private static final long AUTOHIDE_TIMEOUT_MS = 3000;

    private static final int NOTIFICATION_PRIORITY_MULTIPLIER = 10; // see NotificationManagerService
    private static final int HIDE_ICONS_BELOW_SCORE = Notification.PRIORITY_LOW * NOTIFICATION_PRIORITY_MULTIPLIER;

    // The height of the bar, as definied by the build.  It may be taller if we're plugged
    // into hdmi.
    int mNaturalBarHeight = -1;
    int mIconSize = -1;
    int mIconHPadding = -1;
    int mNavIconWidth = -1;
    int mMenuNavIconWidth = -1;
    private int mMaxNotificationIcons = 5;

    TabletStatusBarView mStatusBarView;
    View mNotificationArea;
    View mNotificationTrigger;
    NotificationIconArea mNotificationIconArea;
    ViewGroup mNavigationArea;

    boolean mNotificationDNDMode;
    Object mNotificationDNDDummyEntry;

    ImageView mBackButton;
    View mHomeButton;
    View mMenuButton;
    View mRecentButton;

    private LinearLayout mStatusIcons;

    InputMethodButton mInputMethodSwitchButton;
    CompatModeButton mCompatModeButton;

    NotificationPanel mNotificationPanel;
    WindowManager.LayoutParams mNotificationPanelParams;
    NotificationPeekPanel mNotificationPeekWindow;
    ViewGroup mNotificationPeekRow;
    int mNotificationPeekIndex;
    IBinder mNotificationPeekKey;
    LayoutTransition mNotificationPeekScrubLeft, mNotificationPeekScrubRight;

    int mNotificationPeekTapDuration;
    int mNotificationFlingVelocity;

    TabletBluetoothController mBluetoothController;
    Object mLocationController;
    Object mNetworkController;
    DoNotDisturb mDoNotDisturb;

    ViewGroup mBarContents;

    // hide system chrome ("lights out") support
    View mShadow;

    NotificationIconArea.IconLayout mIconLayout;

    TabletTicker mTicker;

    View mFakeSpaceBar;
    KeyEvent mSpaceBarKeyEvent = null;

    private int mStatusBarWindowState = StatusBarManager.WINDOW_STATE_SHOWING;

    View mCompatibilityHelpDialog = null;

    // for disabling the status bar
    int mDisabled = 0;

    private InputMethodsPanel mInputMethodsPanel;
    private CompatModePanel mCompatModePanel;

    private int mSystemUiVisibility = 0;

    private int mNavigationIconHints = 0;

    private int mShowSearchHoldoff = 0;
    private int mStatusBarMode;
    private boolean checkBarModes;
    private boolean mAutohideSuspended;
    private int mInteractingWindows;
    private Boolean mScreenOn;
    private Context mLargeIconContext;
    private BroadcastReceiver mSettingsReceiver;

    // for heads up notifications
    FrameLayout mHeadsUpNotificationView;
    private int mHeadsUpNotificationDecay;

    @Override
    public void reset() {
        super.reset();
        mNaturalBarHeight = -1;
        mIconSize = -1;
        mIconHPadding = -1;
        mNavIconWidth = -1;
        mMenuNavIconWidth = -1;
        mMaxNotificationIcons = 5;
        mStatusBarView = null;
        mNotificationArea = null;
        mNotificationTrigger = null;
        mNotificationIconArea = null;
        mNavigationArea = null;
        mNotificationDNDMode = false;
        mNotificationDNDDummyEntry = null;
        mBackButton = null;
        mHomeButton = null;
        mMenuButton = null;
        mRecentButton = null;
        mStatusIcons = null;
        mInputMethodSwitchButton = null;
        mCompatModeButton = null;
        mNotificationPanel = null;
        mNotificationPanelParams = null;
        mNotificationPeekWindow = null;
        mNotificationPeekRow = null;
        mNotificationPeekIndex = 0;
        mNotificationPeekKey = null;
        mNotificationPeekScrubLeft = null;
        mNotificationPeekScrubRight = null;
        mNotificationPeekTapDuration = 0;
        mNotificationFlingVelocity = 0;
        mBluetoothController = null;
        mLocationController = null;
        mNetworkController = null;
        mDoNotDisturb = null;
        mBarContents = null;
        mShadow = null;
        mIconLayout = null;
        mTicker = null;
        mFakeSpaceBar = null;
        mSpaceBarKeyEvent = null;
        mStatusBarWindowState = 0;
        mCompatibilityHelpDialog = null;
        mDisabled = 0;
        mInputMethodsPanel = null;
        mCompatModePanel = null;
        mSystemUiVisibility = 0;
        mNavigationIconHints = 0;
        mShowSearchHoldoff = 0;
        mStatusBarMode = 0;
        checkBarModes = false;
        mAutohideSuspended = false;
        mInteractingWindows = 0;
        mScreenOn = null;
        mLargeIconContext = null;
        mSettingsReceiver = null;
        mHeadsUpNotificationDecay = 0;
        mHeadsUpNotificationView = null;
    }

    public Context getContext() {
        return mContext;
    }

    private Runnable mShowSearchPanel = new Runnable() {
        public void run() {
            showSearchPanel();
            awakenDreams();
        }
    };

    private View.OnTouchListener mHomeSearchActionListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (!shouldDisableNavbarGestures() && !inKeyguardRestrictedInputMode()) {
                        mHandler.removeCallbacks(mShowSearchPanel);
                        mHandler.postDelayed(mShowSearchPanel, mShowSearchHoldoff);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mHandler.removeCallbacks(mShowSearchPanel);
                    awakenDreams();
                    break;
            }
            return false;
        }
    };

    private void awakenDreams() {
        Object mDreamManager = XposedHelpers.getObjectField(self, "mDreamManager");
        if (mDreamManager != null) {
            try {
                XposedHelpers.callMethod(mDreamManager, "awaken");
            } catch (Exception e) {
                // fine, stay asleep then
            }
        }
    }

    private final ContentObserver mHeadsUpObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            if (self == null) {
                return;
            }
            boolean wasUsing = XposedHelpers.getBooleanField(self, "mUseHeadsUp");
            boolean mUseHeadsUp = ENABLE_HEADS_UP && 0 != Settings.Global.getInt(
                    mContext.getContentResolver(), SETTING_HEADS_UP, 0);
            XposedHelpers.setBooleanField(self, "mUseHeadsUp", mUseHeadsUp);
            Log.d(TAG, "heads up is " + (mUseHeadsUp ? "enabled" : "disabled"));
            if (wasUsing != mUseHeadsUp) {
                if (!mUseHeadsUp) {
                    Log.d(TAG, "dismissing any existing heads up notification on disable event");
                    mHandler.sendEmptyMessage(MSG_HIDE_HEADS_UP);
                    removeHeadsUpView();
                } else {
                    addHeadsUpView();
                }
            }
        }
    };

    private void addStatusBarWindow() {
        final View sb = makeStatusBarView();

        mWindowManager = (WindowManager) XposedHelpers.getObjectField(self, "mWindowManager");

        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                XposedHelpers.getStaticIntField(TabletKatModule.mWindowManagerLayoutParamsClass, "TYPE_NAVIGATION_BAR"),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);

        if ((Boolean) XposedHelpers.callStaticMethod(ActivityManager.class, "isHighEndGfx")) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }

        lp.gravity = getStatusBarGravity();
        lp.setTitle("NavigationBar"); //Should be SystemBar, but autohide is mor
        lp.packageName = mContext.getPackageName();
        mWindowManager.addView(sb, lp);
    }

    protected void addPanelWindows() {
        final Context context = mContext;
        final Resources res = mContext.getResources();

        // Notification Panel
        RelativeLayout l = (RelativeLayout)View.inflate(context, TkR.layout.system_bar_notification_panel, null);
        ViewHelper.replaceView(l, TkR.id.title_area, new NotificationPanelTitle(context, mLargeIconContext, null));
        mNotificationPanel = (NotificationPanel) ViewHelper.replaceView(l, new NotificationPanel(context, null));
        mNotificationPanel.onFinishInflate();

        mNotificationPanel.setBar(this);
        mNotificationPanel.show(false, false);
        mNotificationPanel.setOnTouchListener(new TouchOutsideListener(MSG_CLOSE_NOTIFICATION_PANEL, mNotificationPanel));

        mBluetoothController.addView((ImageView)mNotificationPanel.findViewById(TkR.id.bluetooth));

        // network icons: either a combo icon that switches between mobile and data, or distinct
        // mobile and data icons
        XposedHelpers.callMethod(mNetworkController, "addCombinedLabelView",
                (TextView) mNotificationPanel.findViewById(TkR.id.network_text));

        FrameLayout f = (FrameLayout) mNotificationPanel.findViewById(SystemR.id.signal_cluster);
        ViewStub cluster = new ViewStub(mLargeIconContext);
        ViewHelper.replaceView(f.getChildAt(0), cluster);
        cluster.setLayoutResource(SystemR.layout.signal_cluster_view);
        cluster.inflate();
        XposedHelpers.callMethod(mNetworkController, "addSignalCluster", f.getChildAt(0));

        XposedHelpers.callMethod(mNetworkController, "addCombinedLabelView",
                (TextView)mBarContents.findViewById(TkR.id.network_text));

        mStatusBarView.setIgnoreChildren(0, mNotificationTrigger, mNotificationPanel);

        WindowManager.LayoutParams lp = mNotificationPanelParams = new WindowManager.LayoutParams(
                res.getDimensionPixelSize(SystemR.dimen.notification_panel_width),
                getNotificationPanelHeight(),
                XposedHelpers.getStaticIntField(TabletKatModule.mWindowManagerLayoutParamsClass, "TYPE_NAVIGATION_BAR_PANEL"),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.setTitle("NotificationPanel");
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
//        lp.windowAnimations = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStyleClass, "Animation"); // == no animation
        lp.windowAnimations = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStyleClass, "Animation_ZoomButtons"); // simple fade

        mWindowManager.addView(mNotificationPanel, lp);

        if (NOTIFICATION_PEEK_ENABLED) {
            RelativeLayout temp = (RelativeLayout) View.inflate(context,
                    TkR.layout.system_bar_notification_peek, null);
            mNotificationPeekWindow = (NotificationPeekPanel)
                    ViewHelper.replaceView(temp, new NotificationPeekPanel(context, null));
            mNotificationPeekWindow.setBar(this);

            int width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 512, res.getDisplayMetrics());
            mNotificationPeekRow = (ViewGroup) mNotificationPeekWindow.findViewById(SystemR.id.content);
            mNotificationPeekWindow.setVisibility(View.GONE);
            mNotificationPeekWindow.setOnTouchListener(
                  new TouchOutsideListener(MSG_CLOSE_NOTIFICATION_PEEK, mNotificationPeekWindow));
            mNotificationPeekScrubRight = new LayoutTransition();
            mNotificationPeekScrubRight.setAnimator(LayoutTransition.APPEARING,
                  ObjectAnimator.ofInt(null, "left", -width, 0));
            mNotificationPeekScrubRight.setAnimator(LayoutTransition.DISAPPEARING,
                  ObjectAnimator.ofInt(null, "left", -width, 0));
            mNotificationPeekScrubRight.setDuration(500);

            mNotificationPeekScrubLeft = new LayoutTransition();
            mNotificationPeekScrubLeft.setAnimator(LayoutTransition.APPEARING,
                  ObjectAnimator.ofInt(null, "left", width, 0));
            mNotificationPeekScrubLeft.setAnimator(LayoutTransition.DISAPPEARING,
                  ObjectAnimator.ofInt(null, "left", width, 0));
            mNotificationPeekScrubLeft.setDuration(500);

            // XXX: setIgnoreChildren?
            lp = new WindowManager.LayoutParams(
                  width, // ViewGroup.LayoutParams.WRAP_CONTENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT,
                  WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                          | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                          | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                  PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.y = res.getDimensionPixelOffset(SystemR.dimen.peek_window_y_offset);
            lp.setTitle("NotificationPeekWindow");
            lp.windowAnimations = com.android.internal.R.style.Animation_Toast;

            mWindowManager.addView(mNotificationPeekWindow, lp);
        }

        if (ENABLE_HEADS_UP) {
            mHeadsUpNotificationView =
                    (FrameLayout) View.inflate(context, SystemR.layout.heads_up, null);
            mHeadsUpNotificationView.setVisibility(View.GONE);
            XposedHelpers.callMethod(mHeadsUpNotificationView, "setBar", self);
        }

        // Search Panel
        mStatusBarView.setBar(self);
        mHomeButton.setOnTouchListener(mHomeSearchActionListener);
        mShowSearchHoldoff = mContext.getResources().getInteger(
                SystemR.integer.config_show_search_delay);
        updateSearchPanel();

        // Input methods Panel
        LinearLayout layout = (LinearLayout)View.inflate(context,
                TkR.layout.system_bar_input_methods_panel, null);
        mInputMethodsPanel = (InputMethodsPanel) ViewHelper.replaceView(layout, new InputMethodsPanel(context, null));
        mInputMethodsPanel.onFinishInflate();
        mInputMethodsPanel.setHardKeyboardEnabledChangeListener(this);
        mInputMethodsPanel.setOnTouchListener(new TouchOutsideListener(MSG_CLOSE_INPUT_METHODS_PANEL, mInputMethodsPanel));
        mInputMethodsPanel.setImeSwitchButton(mInputMethodSwitchButton);
        mStatusBarView.setIgnoreChildren(2, mInputMethodSwitchButton, mInputMethodsPanel);
        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                XposedHelpers.getStaticIntField(TabletKatModule.mWindowManagerLayoutParamsClass, "TYPE_NAVIGATION_BAR_PANEL"),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.setTitle("InputMethodsPanel");
//        lp.windowAnimations = android.R.style.Animation_InputMethod;
//TODO:        lp.windowAnimations = R.style.Animation_RecentPanel;

        mWindowManager.addView(mInputMethodsPanel, lp);

        // Compatibility mode selector panel
        FrameLayout frame = (FrameLayout) View.inflate(context,
                TkR.layout.system_bar_compat_mode_panel, null);
        mCompatModePanel = (CompatModePanel) ViewHelper.replaceView(frame, new CompatModePanel(context, null));
        mCompatModePanel.setOnTouchListener(new TouchOutsideListener(MSG_CLOSE_COMPAT_MODE_PANEL, mCompatModePanel));
        mCompatModePanel.setTrigger(mCompatModeButton);
        mCompatModePanel.setVisibility(View.GONE);
        mCompatModePanel.onFinishInflate();
        mStatusBarView.setIgnoreChildren(3, mCompatModeButton, mCompatModePanel);
        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                XposedHelpers.getStaticIntField(TabletKatModule.mWindowManagerLayoutParamsClass, "TYPE_NAVIGATION_BAR_PANEL"),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.setTitle("CompatModePanel");
        lp.windowAnimations = android.R.style.Animation_Dialog;

        mWindowManager.addView(mCompatModePanel, lp);

        mRecentButton.setOnTouchListener(mRecentsPreloadOnTouchListener);

        mPile = (ViewGroup) mNotificationPanel.findViewById(SystemR.id.content);
        XposedHelpers.setObjectField(self, "mPile", mPile);
        mPile.removeAllViews();
        XposedHelpers.callMethod(mPile, "setLongPressListener", getNotificationLongClicker());

        ScrollView scroller = (ScrollView) mPile.getParent();
        scroller.setFillViewport(true);
    }

    private void addHeadsUpView() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL, // above the status bar!
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp.gravity = Gravity.BOTTOM;
        lp.setTitle("Heads Up");
        lp.packageName = mContext.getPackageName();
        lp.windowAnimations = android.R.style.Animation_InputMethod;

        mWindowManager.addView(mHeadsUpNotificationView, lp);
    }

    private void removeHeadsUpView() {
        mWindowManager.removeView(mHeadsUpNotificationView);
    }


    private int getNotificationPanelHeight() {
        final Display d = mWindowManager.getDefaultDisplay();
        final Point size = new Point();
        d.getRealSize(size);
        return size.y;
    }

    protected void loadDimens() {
        final Resources res = mContext.getResources();

        Configuration c = new Configuration(res.getConfiguration());
        XposedBridge.log("Native density: " + c.densityDpi);

        if (c.densityDpi >= 280) {
            c.densityDpi = DisplayMetrics.DENSITY_XXHIGH;
        }else if (c.densityDpi >= 187) {
            c.densityDpi = DisplayMetrics.DENSITY_XHIGH;
        } else {
            c.densityDpi = DisplayMetrics.DENSITY_HIGH;
        }

        XposedBridge.log("Status bar icons will use density: " + c.densityDpi);
        mLargeIconContext = mContext.createConfigurationContext(c);

        mNaturalBarHeight = res.getDimensionPixelSize(
              TkR.dimen.system_bar_height);

        int newIconSize = res.getDimensionPixelSize(
                TkR.dimen.system_bar_icon_drawing_size);
        int newIconHPadding = res.getDimensionPixelSize(
                TkR.dimen.system_bar_icon_padding);

        loadDimens2(false);

        mHeadsUpNotificationDecay = res.getInteger(SystemR.integer.heads_up_notification_decay);
        XposedHelpers.setIntField(self, "mRowHeight", res.getDimensionPixelSize(SystemR.dimen.notification_row_min_height));

        if (newIconHPadding != mIconHPadding || newIconSize != mIconSize) {
//            Log.d(TAG, "size=" + newIconSize + " padding=" + newIconHPadding);
            mIconHPadding = newIconHPadding;
            mIconSize = newIconSize;
            reloadAllNotificationIcons(); // reload the tray
        }

        final int numIcons = res.getInteger(SystemR.integer.config_maxNotificationIcons);
        if (numIcons != mMaxNotificationIcons) {
            mMaxNotificationIcons = numIcons;
            if (DEBUG) Log.d(TAG, "max notification icons: " + mMaxNotificationIcons);
            reloadAllNotificationIcons();
        }
    }

    protected void loadDimens2(boolean force) {
        final Resources res = mContext.getResources();

        int newNavIconWidth = res.getDimensionPixelSize(TkR.dimen.system_bar_navigation_key_width);
        int newMenuNavIconWidth = res.getDimensionPixelSize(TkR.dimen.system_bar_navigation_menu_key_width);

        if (mNavigationArea != null && (newNavIconWidth != mNavIconWidth)) {
            mNavIconWidth = newNavIconWidth;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    mNavIconWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            mBackButton.setLayoutParams(lp);
            mHomeButton.setLayoutParams(lp);
            mRecentButton.setLayoutParams(lp);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            params.width = mNavIconWidth;
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.leftMargin = mNavIconWidth;
            mStatusBarView.findViewById(SystemR.id.search_light).setLayoutParams(params);
        }

        if (mNavigationArea != null && (force || newMenuNavIconWidth != mMenuNavIconWidth)) {
            mMenuNavIconWidth = newMenuNavIconWidth;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    mMenuNavIconWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            mMenuButton.setLayoutParams(lp);
        }
    }

    protected View makeStatusBarView() {
        mWindowManagerService = XposedHelpers.getObjectField(self, "mWindowManagerService");
        mHandler = (Handler) XposedHelpers.getObjectField(self, "mHandler");

        final Context context = mContext;

        loadDimens();

        FrameLayout temp = (FrameLayout)View.inflate(context, TkR.layout.system_bar, null);
        ViewHelper.replaceView(temp, TkR.id.fake_space_bar, new EventHole(context, null));
        ViewHelper.replaceView(temp, TkR.id.notificationArea, new NotificationArea(context, null));
        ViewHelper.replaceView(temp, TkR.id.imeSwitchButton, new InputMethodButton(context, null));
        ViewHelper.replaceView(temp, TkR.id.compatModeButton, new CompatModeButton(context, null));
        ViewHelper.replaceView(temp, SystemR.id.notificationIcons, new NotificationIconArea(context, null));
        ViewHelper.replaceView(temp, TkR.id.icons, new NotificationIconArea.IconLayout(context, null));
        final TabletStatusBarView sb = (TabletStatusBarView) ViewHelper.replaceView(temp, new TabletStatusBarView(context, mBarService));

        finalizeStatusBarView(sb);

        sb.getBarTransitions().init();

        mStatusBarView = sb;

        mStatusBarView.setDisabledFlags(mDisabled);
        mStatusBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                checkUserAutohide(v, event);
                return false;
            }});

        sb.setHandler(mHandler);

        mBarContents = (ViewGroup) sb.findViewById(TkR.id.bar_contents);

        // the whole right-hand side of the bar
        mNotificationArea = sb.findViewById(TkR.id.notificationArea);
        if (!NOTIFICATION_PEEK_ENABLED) {
            mNotificationArea.setOnTouchListener(new NotificationTriggerTouchListener());
        }
        mNotificationArea.setVisibility(View.VISIBLE);

        // the button to open the notification area
        mNotificationTrigger = sb.findViewById(TkR.id.notificationTrigger);
        if (NOTIFICATION_PEEK_ENABLED) {
            mNotificationTrigger.setOnTouchListener(new NotificationTriggerTouchListener());
        }

        // the more notifications icon
        mNotificationIconArea = (NotificationIconArea)sb.findViewById(SystemR.id.notificationIcons);

        mStatusIcons = (LinearLayout)sb.findViewById(SystemR.id.statusIcons);

        // where the icons go
        mIconLayout = (NotificationIconArea.IconLayout) sb.findViewById(TkR.id.icons);
        if (NOTIFICATION_PEEK_ENABLED) {
            mIconLayout.setOnTouchListener(new NotificationIconTouchListener());
        }

        mNotificationPeekTapDuration = ViewConfiguration.getTapTimeout();
        mNotificationFlingVelocity = 300; // px/s

        mTicker = new TabletTicker(this);

        // The icons
        mLocationController = XposedHelpers.newInstance(TabletKatModule.mLocationControllerClass, mContext); // will post a notification

        // watch the PREF_DO_NOT_DISTURB and convert to appropriate disable() calls
        mDoNotDisturb = new DoNotDisturb(mContext);

        mBluetoothController = new TabletBluetoothController(mLargeIconContext);
        mBluetoothController.addView((ImageView)sb.findViewById(TkR.id.bluetooth));

        mNetworkController = XposedHelpers.newInstance(TabletKatModule.mNetworkControllerClass, mContext);
        final View signalCluster = ((FrameLayout) sb.findViewById(SystemR.id.signal_cluster)).getChildAt(0);
        XposedHelpers.callMethod(mNetworkController, "addSignalCluster", signalCluster);

        // The navigation buttons
        mBackButton = (ImageView)sb.findViewById(SystemR.id.back);
        mNavigationArea = (ViewGroup) sb.findViewById(SystemR.id.nav_buttons);
        mHomeButton = mNavigationArea.findViewById(SystemR.id.home);
        mMenuButton = mNavigationArea.findViewById(SystemR.id.menu);
        mRecentButton = mNavigationArea.findViewById(SystemR.id.recent_apps);
        mRecentButton.setOnClickListener(mOnClickListener);

        loadDimens2(true);

        LayoutTransition lt = new LayoutTransition();
        lt.setDuration(250);
        // don't wait for these transitions; we just want icons to fade in/out, not move around
        lt.setDuration(LayoutTransition.CHANGE_APPEARING, 0);
        lt.setDuration(LayoutTransition.CHANGE_DISAPPEARING, 0);
        lt.addTransitionListener(new LayoutTransition.TransitionListener() {
            public void endTransition(LayoutTransition transition, ViewGroup container,
                                      View view, int transitionType) {
                // ensure the menu button doesn't stick around on the status bar after it's been
                // removed
                mBarContents.invalidate();
            }
            public void startTransition(LayoutTransition transition, ViewGroup container,
                                        View view, int transitionType) {}
        });
        mNavigationArea.setLayoutTransition(lt);
        // no multi-touch on the nav buttons
        mNavigationArea.setMotionEventSplittingEnabled(false);

        mInputMethodSwitchButton = (InputMethodButton) sb.findViewById(TkR.id.imeSwitchButton);
        // Overwrite the lister
        mInputMethodSwitchButton.setOnClickListener(mOnClickListener);
        mInputMethodSwitchButton.setPadding(mIconHPadding, 0, mIconHPadding, 0);

        mCompatModeButton = (CompatModeButton) sb.findViewById(TkR.id.compatModeButton);
        mCompatModeButton.setOnClickListener(mOnClickListener);
        mCompatModeButton.setVisibility(View.GONE);
        mCompatModeButton.setPadding(mIconHPadding, 0, mIconHPadding, 0);

        // for redirecting errant bar taps to the IME
        mFakeSpaceBar = sb.findViewById(TkR.id.fake_space_bar);

        // "shadows" of the status bar features, for lights-out mode
        mShadow = sb.findViewById(TkR.id.bar_shadow);
        mShadow.setOnTouchListener(
                new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent ev) {
                        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                            // even though setting the systemUI visibility below will turn these views
                            // on, we need them to come up faster so that they can catch this motion
                            // event
                            mShadow.setVisibility(View.GONE);
                            mBarContents.setVisibility(View.VISIBLE);

                            try {
                                XposedHelpers.callMethod(mBarService, "setSystemUiVisibility", 0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
                            } catch (Exception ex) {
                                // system process dead
                            }
                        }
                        return false;
                    }
                });

        // set the initial view visibility
        setAreThereNotifications();

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mBroadcastReceiver, filter);

        return sb;
    }

    public void setClockFont(TextView clock, boolean useOldFont){
        DisplayMetrics d = getContext().getResources().getDisplayMetrics();

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) clock.getLayoutParams();
        if (useOldFont){
            params.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, d));
            params.bottomMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, d);
            clock.setTypeface(Typeface.createFromFile("/system/fonts/AndroidClock_Solid.ttf"));
            clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
        }else{
            params.setMarginStart((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, d));
            params.bottomMargin = 0;
            clock.setTypeface(Typeface.DEFAULT);
            clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        }
        clock.setLayoutParams(params);
        clock.requestLayout();
    }

    //TODO: Refactor
    private void finalizeStatusBarView(TabletStatusBarView v) {
        final TextView clock = (TextView) XposedHelpers.newInstance(TabletKatModule.mClockClass, mContext);
        ViewHelper.replaceView(v, SystemR.id.clock, clock);
        clock.setTextColor(mContext.getResources().getColor(SystemR.color.status_bar_clock_color));

        FrameLayout f = (FrameLayout) v.findViewById(SystemR.id.signal_cluster);
        ViewStub cluster = new ViewStub(mLargeIconContext);
        ViewHelper.replaceView(f.getChildAt(0), cluster);
        cluster.setLayoutResource(SystemR.layout.signal_cluster_view);
        try {
            cluster.inflate();
        }catch (Throwable t){
            XposedBridge.log(t);
        }

        View battery = ViewHelper.replaceView(v, SystemR.id.battery, (View) XposedHelpers.newInstance(TabletKatModule.mBatteryMeterViewClass, mLargeIconContext));
        try {
            XposedHelpers.callMethod(battery, "updateSettings", false);
            XposedHelpers.callMethod(battery, "setColors", false);
        } catch (NoSuchMethodError e) {
        }
        ViewHelper.replaceView(v, TkR.id.battery_text, new BatteryPercentView(mContext));
        final BatteryPercentView percent = (BatteryPercentView) v.findViewById(TkR.id.battery_text);
        percent.setTextColor(mContext.getResources().getColor(SystemR.color.status_bar_clock_color));

        ViewGroup view = (ViewGroup)v.findViewById(SystemR.id.nav_buttons);

        int[] ids = {SystemR.id.back, SystemR.id.home, SystemR.id.recent_apps, SystemR.id.menu};
        int[] keyCodes = {4, 3, -1, 82};
        int[] src = {
                SystemR.drawable.ic_sysbar_back,
                SystemR.drawable.ic_sysbar_home,
                SystemR.drawable.ic_sysbar_recent,
                SystemR.drawable.ic_sysbar_menu
        };
        int[] descriptions = {
                SystemR.string.accessibility_back,
                SystemR.string.accessibility_home,
                SystemR.string.accessibility_recent,
                SystemR.string.accessibility_menu
        };

        for (int i = 0; i < ids.length; i++) {
            ImageView button = (ImageView) XposedHelpers.newInstance(TabletKatModule.mKeyButtonViewClass, mContext, null);
            button.setId(ids[i]);

            button.setImageResource(src[i]);
            button.setContentDescription(mContext.getResources().getString(descriptions[i]));

            if (keyCodes[i] > 0){
                XposedHelpers.setIntField(button, "mCode", keyCodes[i]);
            }

            Drawable glowBackground = mContext.getResources().getDrawable(SystemR.drawable.ic_sysbar_highlight);
            XposedHelpers.setObjectField(button, "mGlowBG", glowBackground);
            XposedHelpers.setObjectField(button, "mGlowWidth", glowBackground.getIntrinsicWidth());
            XposedHelpers.setObjectField(button, "mGlowHeight", glowBackground.getIntrinsicHeight());

            if (ids[i] == SystemR.id.menu){
                button.setVisibility(View.INVISIBLE);
            }

            view.addView(button);
        }

        ImageView searchLight = (ImageView) XposedHelpers.newInstance(TabletKatModule.mKeyButtonViewClass, mContext, null);
        searchLight.setId(SystemR.id.search_light);
        searchLight.setImageResource(SystemR.drawable.search_light);
        searchLight.setContentDescription(mContext.getResources().getString(SystemR.string.accessibility_search_light));
        searchLight.setScaleType(ImageView.ScaleType.CENTER);
        searchLight.setVisibility(View.GONE);
        ((ViewGroup) view.getParent()).addView(searchLight);

        ((ImageView) v.findViewById(TkR.id.dot0)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);
        ((ImageView) v.findViewById(TkR.id.dot1)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_large);
        ((ImageView) v.findViewById(TkR.id.dot2)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);
        ((ImageView) v.findViewById(TkR.id.dot3)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);

        mSettingsReceiver = TabletKatModule.registerReceiver(mContext, new OnPreferenceChangedListener() {
            @Override
            public void onPreferenceChanged(String key, boolean value) {
                if (key.equals("ics_clock_font")) {
                    setClockFont(clock, value);
                }
                if (key.equals("battery_percents")){
                    percent.setVisibility(value ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onPreferenceChanged(String key, int value) {

            }

            @Override
            public void init(XSharedPreferences pref) {
                setClockFont(clock, pref.getBoolean("ics_clock_font", false));
                percent.setVisibility(pref.getBoolean("battery_percents", false) ? View.VISIBLE : View.GONE);
            }
        });
    }

    public int getStatusBarHeight() {
        return mStatusBarView != null ? mStatusBarView.getHeight()
                : mContext.getResources().getDimensionPixelSize(
                TkR.dimen.system_bar_height);
    }

    protected int getStatusBarGravity() {
        return Gravity.BOTTOM | Gravity.FILL_HORIZONTAL;
    }

    public void onBarHeightChanged(int height) {
        final WindowManager.LayoutParams lp
                = (WindowManager.LayoutParams)mStatusBarView.getLayoutParams();
        if (lp == null) {
            // haven't been added yet
            return;
        }
        if (lp.height != height) {
            lp.height = height;
            mWindowManager.updateViewLayout(mStatusBarView, lp);
        }
    }

    public void handleMessage(Message m) {
        switch (m.what) {
            case MSG_OPEN_NOTIFICATION_PEEK:
                if (DEBUG) Log.d(TAG, "opening notification peek window; arg=" + m.arg1);

                awakenDreams();

                if (m.arg1 >= 0) {
                    final int N = (Integer) XposedHelpers.callMethod(mNotificationData, "size");

                    if (!mNotificationDNDMode) {
                        if (mNotificationPeekIndex >= 0 && mNotificationPeekIndex < N) {
                            Object entry = XposedHelpers.callMethod(mNotificationData, "get", N - 1 - mNotificationPeekIndex);
                            Object icon = XposedHelpers.getObjectField(entry, "icon");
                            XposedHelpers.callMethod(icon, "setBackgroundColor", 0);
                            mNotificationPeekIndex = -1;
                            mNotificationPeekKey = null;
                        }
                    }

                    final int peekIndex = m.arg1;
                    if (peekIndex < N) {
                        //Log.d(TAG, "loading peek: " + peekIndex);
                        Object entry =
                                mNotificationDNDMode
                                        ? mNotificationDNDDummyEntry
                                        : XposedHelpers.callMethod(mNotificationData, "get", N-1-peekIndex);
                        Object copy = XposedHelpers.newInstance(TabletKatModule.mNotificationDataEntryClass,
                                XposedHelpers.getObjectField(entry, "key"),
                                XposedHelpers.getObjectField(entry, "notification"),
                                XposedHelpers.getObjectField(entry, "icon"));
                        inflateViews(copy, mNotificationPeekRow);

                        if (mNotificationDNDMode) {
                            View content = (View) XposedHelpers.getObjectField(copy, "content");
                            content.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    SharedPreferences.Editor editor = Prefs.edit(mContext);
                                    editor.putBoolean(Prefs.DO_NOT_DISTURB_PREF, false);
                                    editor.apply();
                                    animateCollapsePanels();
                                    visibilityChanged(false);
                                }
                            });
                        }

                        Object icon = XposedHelpers.getObjectField(entry, "icon");
                        XposedHelpers.callMethod(icon, "setBackgroundColor", 0x20FFFFFF);

//                      mNotificationPeekRow.setLayoutTransition(
//                          peekIndex < mNotificationPeekIndex
//                              ? mNotificationPeekScrubLeft
//                              : mNotificationPeekScrubRight);

                        mNotificationPeekRow.removeAllViews();
                        mNotificationPeekRow.addView((View) XposedHelpers.getObjectField(copy, "row"));

                        mNotificationPeekWindow.setVisibility(View.VISIBLE);
                        mNotificationPanel.show(false, true);

                        mNotificationPeekIndex = peekIndex;
                        mNotificationPeekKey = (IBinder) XposedHelpers.getObjectField(entry, "key");
                    }
                }
                break;
            case MSG_CLOSE_NOTIFICATION_PEEK:
                if (DEBUG) Log.d(TAG, "closing notification peek window");
                mNotificationPeekWindow.setVisibility(View.GONE);
                mNotificationPeekRow.removeAllViews();

                final int N = (Integer) XposedHelpers.callMethod(mNotificationData, "size");
                if (mNotificationPeekIndex >= 0 && mNotificationPeekIndex < N) {
                    Object entry =
                            mNotificationDNDMode
                                    ? mNotificationDNDDummyEntry
                                    : XposedHelpers.callMethod(mNotificationData, "get", N-1-mNotificationPeekIndex);
                    Object icon = XposedHelpers.getObjectField(entry, "icon");
                    XposedHelpers.callMethod(icon, "setBackgroundColor", 0);
                }

                mNotificationPeekIndex = -1;
                mNotificationPeekKey = null;
                break;
            case MSG_OPEN_NOTIFICATION_PANEL:
                if (DEBUG) Log.d(TAG, "opening notifications panel");
                awakenDreams();
                if (!mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(true, true);
                    mNotificationArea.setAlpha(0);
                    if (NOTIFICATION_PEEK_ENABLED) {
                        mNotificationPeekWindow.setVisibility(View.GONE);
                    }
                    mTicker.halt();
                }
                break;
            case MSG_CLOSE_NOTIFICATION_PANEL:
                if (DEBUG) Log.d(TAG, "closing notifications panel");
                if (mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(false, true);
                    mNotificationArea.setAlpha(1);
                }
                break;
            case MSG_OPEN_INPUT_METHODS_PANEL:
                awakenDreams();
                if (DEBUG) Log.d(TAG, "opening input methods panel");
                if (mInputMethodsPanel != null) mInputMethodsPanel.openPanel();
                break;
            case MSG_CLOSE_INPUT_METHODS_PANEL:
                if (DEBUG) Log.d(TAG, "closing input methods panel");
                if (mInputMethodsPanel != null) mInputMethodsPanel.closePanel(false);
                break;
            case MSG_OPEN_COMPAT_MODE_PANEL:
                awakenDreams();
                if (DEBUG) Log.d(TAG, "opening compat panel");
                if (mCompatModePanel != null) mCompatModePanel.openPanel();
                break;
            case MSG_CLOSE_COMPAT_MODE_PANEL:
                if (DEBUG) Log.d(TAG, "closing compat panel");
                if (mCompatModePanel != null) mCompatModePanel.closePanel();
                break;
            case MSG_SHOW_CHROME:
                if (DEBUG) Log.d(TAG, "hiding shadows (lights on)");
                mBarContents.setVisibility(View.VISIBLE);
                mShadow.setVisibility(View.GONE);
                mSystemUiVisibility &= ~View.SYSTEM_UI_FLAG_LOW_PROFILE;
                notifyUiVisibilityChanged(mSystemUiVisibility);
                break;
            case MSG_HIDE_CHROME:
                if (DEBUG) Log.d(TAG, "showing shadows (lights out)");
                animateCollapsePanels();
                visibilityChanged(false);
                mBarContents.setVisibility(View.GONE);
                mShadow.setVisibility(View.VISIBLE);
                mSystemUiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
                notifyUiVisibilityChanged(mSystemUiVisibility);
                break;
            case MSG_STOP_TICKER:
                mTicker.halt();
                break;
            case MSG_SHOW_HEADS_UP:
                setHeadsUpVisibility(true);
                break;
            case MSG_HIDE_HEADS_UP:
                setHeadsUpVisibility(false);
                break;
            case MSG_ESCALATE_HEADS_UP:
                escalateHeadsUp();
                setHeadsUpVisibility(false);
                break;
        }
    }

    /**  if the interrupting notification had a fullscreen intent, fire it now.  */
    private void escalateHeadsUp() {
        Object entry = XposedHelpers.getObjectField(self, "mInterruptingNotificationEntry");
        if (entry != null) {
            final StatusBarNotification sbn = (StatusBarNotification) XposedHelpers.getObjectField(entry, "notification");
            final Notification notification = sbn.getNotification();
            if (notification.fullScreenIntent != null) {
                if (DEBUG)
                    Log.d(TAG, "converting a heads up to fullScreen");
                try {
                    notification.fullScreenIntent.send();
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
    }

    public void showClock(boolean show) {
        View clock = mBarContents.findViewById(SystemR.id.clock);
        View network_text = mBarContents.findViewById(TkR.id.network_text);
        if (clock != null) {
            clock.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (network_text != null) {
            network_text.setVisibility((!show) ? View.VISIBLE : View.GONE);
        }
    }

    private boolean hasTicker(Notification n) {
        return n.tickerView != null || !TextUtils.isEmpty(n.tickerText);
    }

    // called by TabletTicker when it's done with all queued ticks
    public void startTicking() {
        mNotificationArea.setVisibility(View.INVISIBLE);
    }

    // called by TabletTicker when it's done with all queued ticks
    public void doneTicking() {
        if (mNotificationArea != null) {
            mNotificationArea.setVisibility(View.VISIBLE);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
    }

//FIXME
    public void setNavigationIconHints(int hints) {
        if (hints == mNavigationIconHints) return;

        if (DEBUG) {
            android.widget.Toast.makeText(mContext,
                    "Navigation icon hints = " + hints,
                    Toast.LENGTH_SHORT).show();
        }

        mNavigationIconHints = hints;

        mStatusBarView.setNavigationIconHints(hints);
        checkBarModes();
    }

    private void notifyUiVisibilityChanged(int vis) {
        try {
            XposedHelpers.callMethod(mWindowManagerService, "statusBarVisibilityChanged", vis);
        } catch (Exception ex) {
        }
    }

    public void setLightsOn(boolean on) {
        // Policy note: if the frontmost activity needs the menu key, we assume it is a legacy app
        // that can't handle lights-out mode.
        if (mMenuButton.getVisibility() == View.VISIBLE) {
            on = true;
        }

        Log.v(TAG, "setLightsOn(" + on + ")");
        if (on) {
            setSystemUiVisibility(0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE, View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    private void showCompatibilityHelp() {
        if (mCompatibilityHelpDialog != null) {
            return;
        }

        mCompatibilityHelpDialog = View.inflate(mContext, TkR.layout.compat_mode_help, null);
        View button = mCompatibilityHelpDialog.findViewById(TkR.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideCompatibilityHelp();
                SharedPreferences.Editor editor = Prefs.edit(mContext);
                editor.putBoolean(Prefs.SHOWN_COMPAT_MODE_HELP, true);
                editor.apply();
            }
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("CompatibilityModeDialog");
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        lp.windowAnimations = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStyleClass, "Animation_ZoomButtons"); // simple fade

        mWindowManager.addView(mCompatibilityHelpDialog, lp);
    }

    private void hideCompatibilityHelp() {
        if (mCompatibilityHelpDialog != null) {
            mWindowManager.removeView(mCompatibilityHelpDialog);
            mCompatibilityHelpDialog = null;
        }
    }

    private boolean isImmersive() {
        try {
            Object o = XposedHelpers.callStaticMethod(TabletKatModule.mActivityManagerNativeClass, "getDefault");
            return (Boolean) XposedHelpers.callMethod(o, "isTopActivityImmersive");
            //Log.d(TAG, "Top activity is " + (immersive?"immersive":"not immersive"));
        } catch (Exception ex) {
            // the end is nigh
            return false;
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v == mRecentButton) {
                onClickRecentButton();
            } else if (v == mInputMethodSwitchButton) {
                onClickInputMethodSwitchButton();
            } else if (v == mCompatModeButton) {
                onClickCompatModeButton();
            }
        }
    };

    public void onClickRecentButton() {
        if (DEBUG) Log.d(TAG, "clicked recent apps; disabled=" + mDisabled);
        if ((mDisabled & StatusBarManager.DISABLE_EXPAND) == 0) {
            awakenDreams();
            toggleRecentApps();
        }
    }

    public void onClickInputMethodSwitchButton() {
        if (DEBUG) Log.d(TAG, "clicked input methods panel; disabled=" + mDisabled);
        int msg = (mInputMethodsPanel.getVisibility() == View.GONE) ?
                MSG_OPEN_INPUT_METHODS_PANEL : MSG_CLOSE_INPUT_METHODS_PANEL;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessage(msg);
    }

    public void onClickCompatModeButton() {
        int msg = (mCompatModePanel.getVisibility() == View.GONE) ?
                MSG_OPEN_COMPAT_MODE_PANEL : MSG_CLOSE_COMPAT_MODE_PANEL;
        mHandler.removeMessages(msg);
        mHandler.sendEmptyMessage(msg);
    }

    public void setOverlayRecentsVisible(boolean visible) {
        if (mIsTv && mStatusBarView != null) {
            mStatusBarView.setBlockEvents(visible);
        }
    }

    private class NotificationTriggerTouchListener implements View.OnTouchListener {
        VelocityTracker mVT;
        float mInitialTouchX, mInitialTouchY;
        int mTouchSlop;

        public NotificationTriggerTouchListener() {
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        private Runnable mHiliteOnR = new Runnable() { public void run() {
            mNotificationArea.setBackgroundResource(
                    XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRDrawableClass, "list_selector_pressed_holo_dark"));
        }};
        public void hilite(final boolean on) {
            if (on) {
                mNotificationArea.postDelayed(mHiliteOnR, 100);
            } else {
                mNotificationArea.removeCallbacks(mHiliteOnR);
                mNotificationArea.setBackground(null);
            }
        }

        public boolean onTouch(View v, MotionEvent event) {
//            Log.d(TAG, String.format("touch: (%.1f, %.1f) initial: (%.1f, %.1f)",
//                        event.getX(),
//                        event.getY(),
//                        mInitialTouchX,
//                        mInitialTouchY));

            if ((mDisabled & StatusBarManager.DISABLE_EXPAND) != 0) {
                return true;
            }

            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mVT = VelocityTracker.obtain();
                    mInitialTouchX = event.getX();
                    mInitialTouchY = event.getY();
                    hilite(true);
                    // fall through
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_MOVE:
                    // check for fling
                    if (mVT != null) {
                        mVT.addMovement(event);
                        mVT.computeCurrentVelocity(1000); // pixels per second
                        // require a little more oomph once we're already in peekaboo mode
                        if (mVT.getYVelocity() < -mNotificationFlingVelocity) {
                            animateExpandNotificationsPanel();
                            visibilityChanged(true);
                            hilite(false);
                            mVT.recycle();
                            mVT = null;
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    hilite(false);
                    if (mVT != null) {
                        if (action == MotionEvent.ACTION_UP
                                // was this a sloppy tap?
                                && Math.abs(event.getX() - mInitialTouchX) < mTouchSlop
                                && Math.abs(event.getY() - mInitialTouchY) < (mTouchSlop / 3)
                                // dragging off the bottom doesn't count
                                && (int)event.getY() < v.getBottom()) {
                            animateExpandNotificationsPanel();
                            visibilityChanged(true);
                            v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                            v.playSoundEffect(SoundEffectConstants.CLICK);
                        }

                        mVT.recycle();
                        mVT = null;
                        return true;
                    }
            }
            return false;
        }
    }

    public void resetNotificationPeekFadeTimer() {
        if (DEBUG) {
            Log.d(TAG, "setting peek fade timer for " + NOTIFICATION_PEEK_FADE_DELAY
                    + "ms from now");
        }
        mHandler.removeMessages(MSG_CLOSE_NOTIFICATION_PEEK);
        mHandler.sendEmptyMessageDelayed(MSG_CLOSE_NOTIFICATION_PEEK,
                NOTIFICATION_PEEK_FADE_DELAY);
    }

    private void reloadAllNotificationIcons() {
        if (mIconLayout == null) return;
        mIconLayout.removeAllViews();
        mCompatModeButton.setPadding(mIconHPadding, 0, mIconHPadding, 0);
        mInputMethodSwitchButton.setPadding(mIconHPadding, 0, mIconHPadding, 0);
        updateNotificationIcons();
    }

    private void loadNotificationPanel() {
        mNotificationData = XposedHelpers.getObjectField(self, "mNotificationData");

        int N = (Integer) XposedHelpers.callMethod(mNotificationData, "size");

        ArrayList<View> toShow = new ArrayList<View>();

        final boolean provisioned = isDeviceProvisioned();
        // If the device hasn't been through Setup, we only show system notifications
        for (int i=0; i<N; i++) {
            Object ent = XposedHelpers.callMethod(mNotificationData, "get", N-i-1);
            if (provisioned || showNotificationEvenIfUnprovisioned((StatusBarNotification) XposedHelpers.getObjectField(ent, "notification"))) {
                toShow.add((View) XposedHelpers.getObjectField(ent, "row"));
            }
        }

        ArrayList<View> toRemove = new ArrayList<View>();
        int n = (Integer) XposedHelpers.callMethod(mPile, "getChildCount");
        for (int i=0; i<n; i++) {
            View child = (View) XposedHelpers.callMethod(mPile, "getChildAt", i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        for (View remove : toRemove) {
            XposedHelpers.callMethod(mPile, "removeView", remove);
        }

        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                // the notification panel has the most important things at the bottom
                int count = (Integer) XposedHelpers.callMethod(mPile, "getChildCount");
                XposedHelpers.callMethod(mPile, "addView", v, Math.min(toShow.size() - 1 - i, count));
            }
        }

        mNotificationPanel.setNotificationCount(toShow.size());
        mNotificationPanel.setSettingsEnabled(isDeviceProvisioned());
    }

    public void clearAll() {
        try {
            XposedHelpers.callMethod(mBarService, "onClearAllNotifications");
        } catch (Exception ex) {
            // system process is dead if we're here.
        }
        animateCollapsePanels();
        visibilityChanged(false);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                int flags = CommandQueue.FLAG_EXCLUDE_NONE;
                if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
                    String reason = intent.getStringExtra("reason");
                    if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                        flags |= CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL;
                    }
                }
                mStatusBarView.notifyScreenOn(false);
                animateCollapsePanels(flags);
            }
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOn = false;
                // no waiting!
                notifyHeadsUpScreenOn(false);
                finishBarAnimations();
                mStatusBarView.notifyScreenOn(true);
            }
            else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOn = true;
            }
        }
    };

    private void setHeadsUpVisibility(boolean vis) {
        if (!ENABLE_HEADS_UP) return;
        if (DEBUG) Log.v(TAG, (vis ? "showing" : "hiding") + " heads up window");
        mHeadsUpNotificationView.setVisibility(vis ? View.VISIBLE : View.GONE);
        if (!vis) {
            if (DEBUG) Log.d(TAG, "setting heads up entry to null");
            XposedHelpers.setObjectField(self, "mInterruptingNotificationEntry", null);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mDisabled=0x");
        pw.println(Integer.toHexString(mDisabled));
        pw.println("mNetworkController:");
        XposedHelpers.callMethod(mNetworkController, "dump", fd, pw, args);
    }

    @Override
    public void addHooks(ClassLoader cl) {
        super.addHooks(cl);

        Class tv = TabletKatModule.mTvStatusBarClass;
        Class base = TabletKatModule.mBaseStatusBarClass;
        Class h = TabletKatModule.mBaseStatusBarHClass;

        XposedHelpers.findAndHookMethod(tv, "setWindowState", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int window = (Integer) methodHookParam.args[0];
                int state = (Integer) methodHookParam.args[1];

                boolean showing = state == StatusBarManager.WINDOW_STATE_SHOWING;
                if (mStatusBarView != null
                        && window == StatusBarManager.WINDOW_NAVIGATION_BAR
                        && mStatusBarWindowState != state) {
                    mStatusBarWindowState = state;
                    if (!showing) {
                        animateCollapsePanels();
                    }
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "setImeWindowStatus", IBinder.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int IME_ACTIVE = XposedHelpers.getStaticIntField(InputMethodService.class, "IME_ACTIVE");
                int IME_VISIBLE = XposedHelpers.getStaticIntField(InputMethodService.class, "IME_VISIBLE");

                IBinder token = (IBinder) methodHookParam.args[0];
                int vis = (Integer) methodHookParam.args[1];
                int backDisposition = (Integer) methodHookParam.args[2];

                mInputMethodSwitchButton.setImeWindowStatus(token,
                        (vis & IME_ACTIVE) != 0);
                updateNotificationIcons();
                mInputMethodsPanel.setImeToken(token);

                boolean altBack = (backDisposition == InputMethodService.BACK_DISPOSITION_WILL_DISMISS)
                        || ((vis & IME_VISIBLE) != 0);

                setNavigationIconHints(
                        altBack ? (mNavigationIconHints | StatusBarManager.NAVIGATION_HINT_BACK_ALT)
                                : (mNavigationIconHints & ~StatusBarManager.NAVIGATION_HINT_BACK_ALT));

                if (FAKE_SPACE_BAR) {
                    mFakeSpaceBar.setVisibility(((vis & IME_VISIBLE) != 0)
                            ? View.VISIBLE : View.GONE);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "createAndAddWindows", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                addStatusBarWindow();
                addPanelWindows();
                return null;
            }
        });

        try {
            XposedHelpers.findAndHookMethod(tv, "getExpandedViewMaxHeight", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return getNotificationPanelHeight();
                }
            });
        }catch(NoSuchMethodError e){

        }

        XposedHelpers.findAndHookMethod(base, "onConfigurationChanged", Configuration.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TabletKatModule.recentsMod.onConfigurationChanged((Configuration) param.args[0]);

                if (!mIsTv){
                    return;
                }

                loadDimens();
                mNotificationPanelParams.height = getNotificationPanelHeight();
                mWindowManager.updateViewLayout(mNotificationPanel, mNotificationPanelParams);
                mShowSearchHoldoff = mContext.getResources().getInteger(
                        SystemR.integer.config_show_search_delay);
                updateSearchPanel();
            }
        });

        XposedHelpers.findAndHookMethod(tv, "refreshLayout", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                mNotificationPanel.refreshLayout((Integer)methodHookParam.args[0]);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "getStatusBarView", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return mStatusBarView;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "getSearchLayoutParams", ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                boolean opaque = false;
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        XposedHelpers.getStaticIntField(TabletKatModule.mWindowManagerLayoutParamsClass, "TYPE_NAVIGATION_BAR_PANEL"),
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                        (opaque ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT));
                if ((Boolean) XposedHelpers.callStaticMethod(ActivityManager.class, "isHighEndGfx")) {
                    lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                } else {
                    lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    lp.dimAmount = 0.7f;
                }
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                lp.setTitle("SearchPanel");

                lp.windowAnimations = XposedHelpers.getStaticIntField(TabletKatModule.mComAndroidInternalRStyleClass, "Animation_RecentApplications");
                lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
                return lp;
            }
        });

        try {
            XposedHelpers.findAndHookMethod(base, "updateSearchPanel", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (!mIsTv) {
                        return TabletKatModule.invokeOriginalMethod(methodHookParam);
                    }

                    View mSearchPanelView = (View) XposedHelpers.getObjectField(self, "mSearchPanelView");

                    // Search Panel
                    boolean visible = false;
                    if (mSearchPanelView != null) {
                        visible = (Boolean) XposedHelpers.callMethod(mSearchPanelView, "isShowing");
                        mWindowManager.removeView(mSearchPanelView);
                    }

                    // Provide SearchPanel with a temporary parent to allow layout params to work.
                    LinearLayout tmpRoot = new LinearLayout(mContext);

                    Configuration c = new Configuration(mContext.getResources().getConfiguration());
                    c.smallestScreenWidthDp = 720;
                    Context c2 = mContext.createConfigurationContext(c);
                    mSearchPanelView = LayoutInflater.from(c2).inflate(
                            SystemR.layout.status_bar_search_panel, tmpRoot, false);
                    mSearchPanelView.setOnTouchListener(
                            new TouchOutsideListener(MSG_CLOSE_SEARCH_PANEL, mSearchPanelView));
                    mSearchPanelView.setVisibility(View.GONE);

                    WindowManager.LayoutParams lp = getSearchLayoutParams(mSearchPanelView.getLayoutParams());

                    mWindowManager.addView(mSearchPanelView, lp);
                    XposedHelpers.callMethod(mSearchPanelView, "setBar", self);
                    if (visible) {
                        XposedHelpers.callMethod(mSearchPanelView, "show", true, false);
                    }

                    mStatusBarView.setDelegateView(mSearchPanelView);
                    XposedHelpers.setObjectField(self, "mSearchPanelView", mSearchPanelView);
                    return null;
                }
            });
        }catch (NoSuchMethodError e){}

        XposedHelpers.findAndHookMethod(base, "showSearchPanel", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsTv){
                    return;
                }

                WindowManager.LayoutParams lp =
                        (android.view.WindowManager.LayoutParams) mStatusBarView.getLayoutParams();
                lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                mWindowManager.updateViewLayout(mStatusBarView, lp);
            }
        });

        XposedHelpers.findAndHookMethod(base, "hideSearchPanel", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsTv){
                    return;
                }

                WindowManager.LayoutParams lp =
                        (android.view.WindowManager.LayoutParams) mStatusBarView.getLayoutParams();
                lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
                mWindowManager.updateViewLayout(mStatusBarView, lp);
            }
        });

        XposedHelpers.findAndHookMethod(h, "handleMessage", Message.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsTv){
                    return;
                }

                handleMessage((Message) param.args[0]);
            }
        });

        XposedHelpers.findAndHookMethod(tv, "tick", IBinder.class, StatusBarNotification.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                IBinder key = (IBinder) methodHookParam.args[0];
                StatusBarNotification n = (StatusBarNotification) methodHookParam.args[1];
                boolean firstTime = (Boolean) methodHookParam.args[2];

                // Don't show the ticker when the windowshade is open.
                if (mNotificationPanel.isShowing()) {
                    return null;
                }
                // If they asked for FLAG_ONLY_ALERT_ONCE, then only show this notification
                // if it's a new notification.
                if (!firstTime && (n.getNotification().flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0) {
                    return null;
                }

                // Don't show minimum priority notifications
                if (n.getNotification().priority == Notification.PRIORITY_MIN) {
                    return null;
                }
                // Show the ticker if one is requested. Also don't do this
                // until status bar window is attached to the window manager,
                // because...  well, what's the point otherwise?  And trying to
                // run a ticker without being attached will crash!
                if (hasTicker(n.getNotification()) && mStatusBarView.getWindowToken() != null) {
                    if (0 == (mDisabled & (StatusBarManager.DISABLE_NOTIFICATION_ICONS
                            | StatusBarManager.DISABLE_NOTIFICATION_TICKER))) {
                        mTicker.add(key, n);
                    }
                }
                return null;
            }
        });
/*
        XposedHelpers.findAndHookMethod(tv, "animateExpandSettingsPanel", XC_MethodReplacement.DO_NOTHING);
*/
        XposedHelpers.findAndHookMethod(tv, "setSystemUiVisibility", int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int vis = (Integer) methodHookParam.args[0];
                int mask = (Integer) methodHookParam.args[1];

                final int oldVal = mSystemUiVisibility;
                final int newVal = (oldVal & ~mask) | (vis & mask);
                final int diff = newVal ^ oldVal;

                if (diff != 0) {
                    mSystemUiVisibility = newVal;

                    if (0 != (diff & View.SYSTEM_UI_FLAG_LOW_PROFILE)) {
                        final boolean lightsOut = (vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0;
                        if (lightsOut) {
                            animateCollapsePanels();
//                            if (mTicking) {
//                                XposedHelpers.callMethod(self, "haltTicker");
//                            }
                        }

                        setAreThereNotifications();
                    }

                    final int sbMode = mStatusBarView == null ? -1 : computeBarMode(
                            oldVal, newVal, mStatusBarView.getBarTransitions(),
                            ViewConstants.NAVIGATION_BAR_TRANSIENT, ViewConstants.NAVIGATION_BAR_TRANSLUCENT);
                    final boolean sbModeChanged = sbMode != -1;

                    if (sbModeChanged && sbMode != mStatusBarMode) {
                        mStatusBarMode = sbMode;
                        checkBarModes = true;
                    }
                    if (checkBarModes) {
                        checkBarModes();
                    }
                    if (sbModeChanged) {
                        // update transient bar autohide
                        if (sbMode == BarTransitions.MODE_SEMI_TRANSPARENT) {
                            scheduleAutohide();
                        } else {
                            cancelAutohide();
                        }
                    }
                    // ready to unhide
                    if ((vis & ViewConstants.NAVIGATION_BAR_UNHIDE) != 0) {
                        mSystemUiVisibility &= ~ViewConstants.NAVIGATION_BAR_UNHIDE;
                    }

                    // send updated sysui visibility to window manager
                    notifyUiVisibilityChanged(mSystemUiVisibility);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "setHardKeyboardStatus", boolean.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                boolean available = (Boolean) methodHookParam.args[0];
                boolean enabled = (Boolean) methodHookParam.args[1];
                if (DEBUG) {
                    Log.d(TAG, "Set hard keyboard status: available=" + available
                            + ", enabled=" + enabled);
                }
                mInputMethodSwitchButton.setHardKeyboardStatus(available);
                updateNotificationIcons();
                mInputMethodsPanel.setHardKeyboardStatus(available, enabled);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "setAreThereNotifications", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mNotificationPanel != null) {
                    mNotificationPanel.setClearable(isDeviceProvisioned() && (Boolean) XposedHelpers.callMethod(mNotificationData, "hasClearableItems"));
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "updateNotificationIcons", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                // XXX: need to implement a new limited linear layout class
                // to avoid removing & readding everything

                if (mIconLayout == null) return null;

                // first, populate the main notification panel
                loadNotificationPanel();

                final LinearLayout.LayoutParams params
                        = new LinearLayout.LayoutParams(mIconSize + 2 * mIconHPadding, mNaturalBarHeight);

                // alternate behavior in DND mode
                if (mNotificationDNDMode) {
                    if (mIconLayout.getChildCount() == 0) {
                        final Notification dndNotification = new Notification.Builder(mContext)
                                .setContentTitle(mContext.getText(TkR.string.notifications_off_title))
                                .setContentText(mContext.getText(TkR.string.notifications_off_text))
                                .setSmallIcon(TkR.drawable.ic_notification_dnd)
                                .setOngoing(true)
                                .getNotification();

                        final ImageView iconView = (ImageView) XposedHelpers.newInstance(TabletKatModule.mStatusBarIconViewClass,
                                mLargeIconContext, "_dnd",
                                dndNotification);
                        iconView.setImageResource(TkR.drawable.ic_notification_dnd);
                        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        iconView.setPadding(mIconHPadding, 0, mIconHPadding, 0);

                        mNotificationDNDDummyEntry = XposedHelpers.newInstance(TabletKatModule.mNotificationDataEntryClass,
                                new Class<?>[]{IBinder.class, StatusBarNotification.class, TabletKatModule.mStatusBarIconViewClass},
                                null, new StatusBarNotification("", null, 0, "", 0, 0, Notification.PRIORITY_MAX,
                                dndNotification, android.os.Process.myUserHandle(), System.currentTimeMillis()), iconView);

                        mIconLayout.addView(iconView, params);
                    }

                    return null;
                } else if (0 != (mDisabled & StatusBarManager.DISABLE_NOTIFICATION_ICONS)) {
                    // if icons are disabled but we're not in DND mode, this is probably Setup and we should
                    // just leave the area totally empty
                    return null;
                }

                int N = (Integer) XposedHelpers.callMethod(mNotificationData, "size");

                if (DEBUG) {
                    Log.d(TAG, "refreshing icons: " + N + " notifications, mIconLayout=" + mIconLayout);
                }

                ArrayList<View> toShow = new ArrayList<View>();

                // Extra Special Icons
                // The IME switcher and compatibility mode icons take the place of notifications. You didn't
                // need to see all those new emails, did you?
                int maxNotificationIconsCount = mMaxNotificationIcons;
                if (mInputMethodSwitchButton.getVisibility() != View.GONE) maxNotificationIconsCount--;
                if (mCompatModeButton.getVisibility() != View.GONE) maxNotificationIconsCount--;

                final boolean provisioned = isDeviceProvisioned();
                // If the device hasn't been through Setup, we only show system notifications
                for (int i = 0; toShow.size() < maxNotificationIconsCount; i++) {
                    if (i >= N) break;
                    Object ent = XposedHelpers.callMethod(mNotificationData, "get", N - i - 1);
                    StatusBarNotification n = (StatusBarNotification) XposedHelpers.getObjectField(ent, "notification");
                    if ((provisioned && (Integer) XposedHelpers.callMethod(n, "getScore") >= HIDE_ICONS_BELOW_SCORE)
                            || showNotificationEvenIfUnprovisioned(n)) {
                        toShow.add((View) XposedHelpers.getObjectField(ent, "icon"));
                    }
                }

                ArrayList<View> toRemove = new ArrayList<View>();
                for (int i = 0; i < mIconLayout.getChildCount(); i++) {
                    View child = mIconLayout.getChildAt(i);
                    if (!toShow.contains(child)) {
                        toRemove.add(child);
                    }
                }

                for (View remove : toRemove) {
                    mIconLayout.removeView(remove);
                }

                for (int i = 0; i < toShow.size(); i++) {
                    View v = toShow.get(i);
                    v.setPadding(mIconHPadding, 0, mIconHPadding, 0);
                    if (v.getParent() == null) {
                        mIconLayout.addView(v, i, params);
                    }
                }
                return null;
            }
        });


        XposedHelpers.findAndHookMethod(base, "workAroundBadLayerDrawableOpacity", View.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                View v = (View) methodHookParam.args[0];
                Drawable bgd = v.getBackground();
                if (!(bgd instanceof LayerDrawable)) return null;

                LayerDrawable d = (LayerDrawable) bgd;
                v.setBackground(null);
                d.setOpacity(PixelFormat.TRANSLUCENT);
                v.setBackground(d);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(base, "isTopNotification", ViewGroup.class, TabletKatModule.mNotificationDataEntryClass, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                ViewGroup parent = (ViewGroup) methodHookParam.args[0];
                Object entry = methodHookParam.args[1];
                if (parent == null || entry == null) return false;

                Object row = XposedHelpers.getObjectField(entry, "row");
                int index = (Integer) XposedHelpers.callMethod(parent, "indexOfChild", row);
                return index == parent.getChildCount() - 1;
            }
        });

        try {
            XposedHelpers.findAndHookMethod(tv, "haltTicker", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    mTicker.halt();
                    return null;
                }
            });
        }catch (NoSuchMethodError e){}

        XposedHelpers.findAndHookMethod(tv, "updateExpandedViewPos", int.class, XC_MethodReplacement.DO_NOTHING);

        XposedHelpers.findAndHookMethod(tv, "shouldDisableNavbarGestures", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return mNotificationPanel.getVisibility() == View.VISIBLE
                        || (mDisabled & StatusBarManager.DISABLE_SEARCH) != 0;
            }
        });
/*        XposedHelpers.findAndHookMethod(tv, "onDestroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mContext.unregisterReceiver(mBroadcastReceiver);
            }
        });*/
        XposedHelpers.findAndHookMethod(tv, "addIcon", String.class, int.class, int.class, TabletKatModule.mStatusBarIconClass, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String slot = (String) methodHookParam.args[0];
                int viewIndex = 0; //(Integer) methodHookParam.args[2];
                Object icon = methodHookParam.args[3];

                if (DEBUG) Log.d(TAG, "addIcon(" + slot + ") -> " + icon);
//                Toast.makeText(mContext, "addIcon(" + slot + ") -> " + icon, Toast.LENGTH_SHORT).show();

                if (!allowIcon(slot)){
                    return null;
                }

                ImageView view = (ImageView) XposedHelpers.newInstance(TabletKatModule.mStatusBarIconViewClass,
                        mLargeIconContext, slot, null);
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                view.setPadding(0, 0, 0, 0);

                XposedHelpers.callMethod(view, "set", icon);
                mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(mIconSize, mIconSize));
                return null;
            };
        });

        XposedHelpers.findAndHookMethod(tv, "updateIcon", String.class, int.class, int.class, TabletKatModule.mStatusBarIconClass, TabletKatModule.mStatusBarIconClass, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String slot = (String) methodHookParam.args[0];
                int viewIndex = 0;//(Integer) methodHookParam.args[2];
                Object icon = methodHookParam.args[3];

                if (DEBUG) Log.d(TAG, "updateIcon(" + slot + ") -> " + icon);
//                Toast.makeText(mContext, "updateIcon(" + slot + ") -> " + icon, Toast.LENGTH_SHORT).show();

                if (!allowIcon(slot)){
                    return null;
                }

                ImageView view = (ImageView) mStatusIcons.getChildAt(viewIndex);
                XposedHelpers.callMethod(view, "set", icon);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "removeIcon", String.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String slot = (String) methodHookParam.args[0];
                int viewIndex = 0;//(Integer) methodHookParam.args[2];

                if (DEBUG) Log.d(TAG, "removeIcon(" + slot + ")");
//                Toast.makeText(mContext, "removeIcon(" + slot + ")", Toast.LENGTH_SHORT).show();

                if (!allowIcon(slot)){
                    return null;
                }

                mStatusIcons.removeViewAt(viewIndex);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "addNotification", IBinder.class, StatusBarNotification.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                IBinder key = (IBinder) methodHookParam.args[0];
                StatusBarNotification notification = (StatusBarNotification) methodHookParam.args[1];

                if (isNotificationBlacklisted(notification)){
                    return null;
                }

                if (DEBUG) Log.d(TAG, "addNotification(" + key + " -> " + notification + ")");
                Object shadeEntry = addNotificationViews(key, notification);

                if (XposedHelpers.getBooleanField(self, "mUseHeadsUp") && shouldInterrupt(notification)) {
                    if (DEBUG) Log.d(TAG, "launching notification in heads up mode");
                    Object interruptionCandidate = XposedHelpers.newInstance(
                            TabletKatModule.mNotificationDataEntryClass, key, notification, null);
                    if (inflateViews(interruptionCandidate, (ViewGroup)
                            XposedHelpers.callMethod(mHeadsUpNotificationView, "getHolder"))) {
                        XposedHelpers.setLongField(self, "mInterruptingNotificationTime", System.currentTimeMillis());
                        XposedHelpers.setObjectField(self, "mInterruptingNotificationEntry", interruptionCandidate);
                        XposedHelpers.callMethod(shadeEntry, "setInterruption");

                        // 1. Populate mHeadsUpNotificationView
                        XposedHelpers.callMethod(mHeadsUpNotificationView, "setNotification", interruptionCandidate);

                        // 2. Animate mHeadsUpNotificationView in
                        mHandler.sendEmptyMessage(MSG_SHOW_HEADS_UP);

                        // 3. Set alarm to age the notification off
                        resetHeadsUpDecayTimer();
                    }
                } else if (notification.getNotification().fullScreenIntent != null) {
                    // Stop screensaver if the notification has a full-screen intent.
                    // (like an incoming phone call)
                    awakenDreams();

                    // not immersive & a full-screen alert should be shown
                    Log.w(TAG, "Notification has fullScreenIntent and activity is not immersive;"
                            + " sending fullScreenIntent");
                    try {
                        notification.getNotification().fullScreenIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                    }
                } else {
                    tick(key, notification, true);
                }

                setAreThereNotifications();

                return null;
            }
        });

        try {
            XposedHelpers.findAndHookMethod(tv, "resetHeadsUpDecayTimer", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (XposedHelpers.getBooleanField(self, "mUseHeadsUp") && mHeadsUpNotificationDecay > 0
                            && (Boolean) XposedHelpers.callMethod(mHeadsUpNotificationView, "isClearable")) {
                        mHandler.removeMessages(MSG_HIDE_HEADS_UP);
                        mHandler.sendEmptyMessageDelayed(MSG_HIDE_HEADS_UP, mHeadsUpNotificationDecay);
                    }
                    return null;
                }
            });
        } catch (NoSuchMethodError e) {
            ENABLE_HEADS_UP = false;
        }

        XposedHelpers.findAndHookMethod(tv, "removeNotification", IBinder.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                IBinder key = (IBinder) methodHookParam.args[0];

                if (DEBUG) Log.d(TAG, "removeNotification(" + key + ")");
                StatusBarNotification old = removeNotificationViews(key);
                mTicker.remove(key);
                setAreThereNotifications();
                mNotificationPanel.updateClearButton();

                Object entry = XposedHelpers.getObjectField(self, "mInterruptingNotificationEntry");
                if (ENABLE_HEADS_UP && entry != null
                        && old == XposedHelpers.getObjectField(entry, "notification")) {
                    mHandler.sendEmptyMessage(MSG_HIDE_HEADS_UP);
                }
                if (NOTIFICATION_PEEK_ENABLED && key == mNotificationPeekKey) {
                    // must close the peek as well, since it's gone
                    mHandler.sendEmptyMessage(MSG_CLOSE_NOTIFICATION_PEEK);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "animateCollapsePanels", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int flags = (Integer) methodHookParam.args[0];
                if ((flags & CommandQueue.FLAG_EXCLUDE_NOTIFICATION_PANEL) == 0) {
                    mHandler.removeMessages(MSG_CLOSE_NOTIFICATION_PANEL);
                    mHandler.sendEmptyMessage(MSG_CLOSE_NOTIFICATION_PANEL);
                }
                if (NOTIFICATION_PEEK_ENABLED) {
                    mHandler.removeMessages(MSG_CLOSE_NOTIFICATION_PEEK);
                    mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
                    mHandler.sendEmptyMessage(MSG_CLOSE_NOTIFICATION_PEEK);
                }
                if ((flags & CommandQueue.FLAG_EXCLUDE_RECENTS_PANEL) == 0) {
                    mHandler.removeMessages(MSG_CLOSE_RECENTS_PANEL);
                    mHandler.sendEmptyMessage(MSG_CLOSE_RECENTS_PANEL);
                }
                if ((flags & CommandQueue.FLAG_EXCLUDE_SEARCH_PANEL) == 0) {
                    mHandler.removeMessages(MSG_CLOSE_SEARCH_PANEL);
                    mHandler.sendEmptyMessage(MSG_CLOSE_SEARCH_PANEL);
                }
                if ((flags & CommandQueue.FLAG_EXCLUDE_INPUT_METHODS_PANEL) == 0) {
                    mHandler.removeMessages(MSG_CLOSE_INPUT_METHODS_PANEL);
                    mHandler.sendEmptyMessage(MSG_CLOSE_INPUT_METHODS_PANEL);
                }
                if ((flags & CommandQueue.FLAG_EXCLUDE_COMPAT_MODE_PANEL) == 0) {
                    mHandler.removeMessages(MSG_CLOSE_COMPAT_MODE_PANEL);
                    mHandler.sendEmptyMessage(MSG_CLOSE_COMPAT_MODE_PANEL);
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "disable", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int state = (Integer) methodHookParam.args[0];

                int old = mDisabled;
                int diff = state ^ old;
                mDisabled = state;

                // act accordingly
                if ((diff & StatusBarManager.DISABLE_CLOCK) != 0) {
                    boolean show = (state & StatusBarManager.DISABLE_CLOCK) == 0;
                    Log.i(TAG, "DISABLE_CLOCK: " + (show ? "no" : "yes"));
                    showClock(show);
                }
                if ((diff & StatusBarManager.DISABLE_SYSTEM_INFO) != 0) {
                    boolean show = (state & StatusBarManager.DISABLE_SYSTEM_INFO) == 0;
                    Log.i(TAG, "DISABLE_SYSTEM_INFO: " + (show ? "no" : "yes"));
                    mNotificationTrigger.setVisibility(show ? View.VISIBLE : View.GONE);
                }
                if ((diff & StatusBarManager.DISABLE_EXPAND) != 0) {
                    if ((state & StatusBarManager.DISABLE_EXPAND) != 0) {
                        Log.i(TAG, "DISABLE_EXPAND: yes");
                        animateCollapsePanels();
                        visibilityChanged(false);
                    }
                }
                if ((diff & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
                    mNotificationDNDMode = Prefs.read(mContext)
                            .getBoolean(Prefs.DO_NOT_DISTURB_PREF, Prefs.DO_NOT_DISTURB_DEFAULT);

                    if ((state & StatusBarManager.DISABLE_NOTIFICATION_ICONS) != 0) {
                        Log.i(TAG, "DISABLE_NOTIFICATION_ICONS: yes" + (mNotificationDNDMode?" (DND)":""));
                        mTicker.halt();
                    } else {
                        Log.i(TAG, "DISABLE_NOTIFICATION_ICONS: no" + (mNotificationDNDMode?" (DND)":""));
                    }

                    // refresh icons to show either notifications or the DND message
                    reloadAllNotificationIcons();
                } else if ((diff & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
                    if ((state & StatusBarManager.DISABLE_NOTIFICATION_TICKER) != 0) {
                        mTicker.halt();
                    }
                }
                if ((diff & (StatusBarManager.DISABLE_RECENT
                        | StatusBarManager.DISABLE_BACK
                        | StatusBarManager.DISABLE_HOME
                        | StatusBarManager.DISABLE_SEARCH)) != 0) {
                    mStatusBarView.setDisabledFlags(state);

                    if ((state & StatusBarManager.DISABLE_RECENT) != 0) {
                        // close recents if it's visible
                        mHandler.removeMessages(MSG_CLOSE_RECENTS_PANEL);
                        mHandler.sendEmptyMessage(MSG_CLOSE_RECENTS_PANEL);
                    }
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "topAppWindowChanged", boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                if (mMenuButton == null) {
                    return null;
                }

                boolean showMenu = (Boolean) methodHookParam.args[0];

                if (DEBUG) {
                    Log.d(TAG, (showMenu?"showing":"hiding") + " the MENU button");
                }
                mMenuButton.setVisibility(showMenu ? View.VISIBLE : View.GONE);

                // See above re: lights-out policy for legacy apps.
                if (showMenu) setLightsOn(true);

                mCompatModeButton.refresh();
                if (mCompatModeButton.getVisibility() == View.VISIBLE) {
                    if (DEBUG_COMPAT_HELP
                            || ! Prefs.read(mContext).getBoolean(Prefs.SHOWN_COMPAT_MODE_HELP, false)) {
                        showCompatibilityHelp();
                    }
                } else {
                    hideCompatibilityHelp();
                    mCompatModePanel.closePanel();
                }

                return null;
            }
        });

        XposedHelpers.findAndHookMethod(tv, "animateExpandNotificationsPanel", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PANEL);
                mHandler.sendEmptyMessage(MSG_OPEN_NOTIFICATION_PANEL);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(base, "onShowSearchPanel", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                mStatusBarView.getBarTransitions().setContentVisible(false);
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(base, "onHideSearchPanel", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                mStatusBarView.getBarTransitions().setContentVisible(true);
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(base, "setInteracting", int.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }

                int barWindow = (Integer) methodHookParam.args[0];
                boolean interacting = (Boolean) methodHookParam.args[1];

                mInteractingWindows = interacting
                        ? (mInteractingWindows | barWindow)
                        : (mInteractingWindows & ~barWindow);
                if (mInteractingWindows != 0) {
                    suspendAutohide();
                } else {
                    resumeSuspendedAutohide();
                }
                checkBarModes();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(base, "destroy", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!mIsTv) {
                    return;
                }
                if (mStatusBarView != null) {
                    mWindowManager.removeViewImmediate(mStatusBarView);
                }
                if (mNotificationPanel != null) {
                    mWindowManager.removeViewImmediate(mNotificationPanel);
                }
                if (mInputMethodsPanel != null) {
                    mWindowManager.removeViewImmediate(mInputMethodsPanel);
                }
                if (mCompatModePanel != null) {
                    mWindowManager.removeViewImmediate(mCompatModePanel);
                }
                if (mCompatibilityHelpDialog != null) {
                    mWindowManager.removeViewImmediate(mCompatibilityHelpDialog);
                }
                if (mTicker != null) {
                    mTicker.halt();
                }
                mContext.unregisterReceiver(mBroadcastReceiver);
                reset();
            }
        });
        try {
            XposedHelpers.findAndHookMethod(base, "onHeadsUpDismissed", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Object mInterruptingNotificationEntry = XposedHelpers.getObjectField(self, "mInterruptingNotificationEntry");
                    if (mInterruptingNotificationEntry == null) return null;
                    mHandler.sendEmptyMessage(MSG_HIDE_HEADS_UP);
                    if ((Boolean) XposedHelpers.callMethod(mHeadsUpNotificationView, "isClearable")) {
                        try {
                            StatusBarNotification notification = (StatusBarNotification)
                                    XposedHelpers.getObjectField(mInterruptingNotificationEntry, "notification");
                            XposedHelpers.callMethod(mBarService, "onNotificationClear",
                                    notification.getPackageName(),
                                    notification.getTag(),
                                    notification.getId());
                        } catch (Exception ex) {
                            // oh well
                        }
                    }
                    return null;
                }
            });
        } catch (NoSuchMethodError e) {
            ENABLE_HEADS_UP = false;
        }
        XposedHelpers.findAndHookMethod(TabletKatModule.mDateViewClass, "updateClock", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!mIsTv){
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                TextView text = (TextView) methodHookParam.thisObject;

                final Context context = getContext();
                Date now = new Date();
                CharSequence dow = DateFormat.format("EEEE", now);
                CharSequence date = DateFormat.getLongDateFormat(context).format(now);
                text.setText(context.getString(TkR.string.status_bar_date_formatter, dow, date));
                return null;
            }
        });
    }

    @Override
    protected void updatePeek(IBinder key) {
        if (NOTIFICATION_PEEK_ENABLED && key == mNotificationPeekKey) {
            // must update the peek window
            Message peekMsg = mHandler.obtainMessage(MSG_OPEN_NOTIFICATION_PEEK);
            peekMsg.arg1 = mNotificationPeekIndex;
            mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
            mHandler.sendMessage(peekMsg);
        }
    }

    private boolean allowIcon(String slot) {
        return "location".equals(slot);
    }

    private void resumeSuspendedAutohide() {
        if (mAutohideSuspended) {
            scheduleAutohide();
            mHandler.postDelayed(mCheckBarModes, 500); // longer than home -> launcher
        }
    }

    private void suspendAutohide() {
        mHandler.removeCallbacks(mAutohide);
        mHandler.removeCallbacks(mCheckBarModes);
        mAutohideSuspended = (mSystemUiVisibility & ViewConstants.NAVIGATION_BAR_TRANSIENT) != 0;
    }

    private void cancelAutohide() {
        mAutohideSuspended = false;
        mHandler.removeCallbacks(mAutohide);
    }

    private void scheduleAutohide() {
        cancelAutohide();
        mHandler.postDelayed(mAutohide, AUTOHIDE_TIMEOUT_MS);
    }

    private void checkUserAutohide(View v, MotionEvent event) {
        if ((mSystemUiVisibility & ViewConstants.NAVIGATION_BAR_TRANSIENT) != 0  // a transient bar is revealed
                && event.getAction() == MotionEvent.ACTION_OUTSIDE // touch outside the source bar
                && event.getX() == 0 && event.getY() == 0  // a touch outside both bars
                ) {
            userAutohide();
        }
    }

    private void userAutohide() {
        cancelAutohide();
        mHandler.postDelayed(mAutohide, 350); // longer than app gesture -> flag clear
    }

    //We have an IME switcher button, so we don't want any notifications about that
    private boolean isNotificationBlacklisted(StatusBarNotification n) {
        if ("android".equals(n.getPackageName())) {
            String[] kind = (String[]) XposedHelpers.getObjectField(n.getNotification(), "kind");
            if (kind != null) {
                for (String aKind : kind) {
                    // IME switcher, created by InputMethodManagerService
                    if ("android.system.imeswitcher".equals(aKind)) return true;
                }
            }
        }
        return false;
    }

    public void animateExpandNotificationsPanel() {
        XposedHelpers.callMethod(self, "animateExpandNotificationsPanel");
    }

    @Override
    public void onHardKeyboardEnabledChange(boolean enabled) {
        try {
            XposedHelpers.callMethod(mBarService, "setHardKeyboardEnabled", enabled);
        } catch (Exception ex) {
        }
    }

    private int computeBarMode(int oldVis, int newVis, BarTransitions transitions, int transientFlag, int translucentFlag) {
        final int oldMode = barMode(oldVis, transientFlag, translucentFlag);
        final int newMode = barMode(newVis, transientFlag, translucentFlag);
        if (oldMode == newMode) {
            return -1; // no mode change
        }
        return newMode;
    }

    private int barMode(int vis, int transientFlag, int translucentFlag) {
        return (vis & transientFlag) != 0 ? BarTransitions.MODE_SEMI_TRANSPARENT
                : (vis & translucentFlag) != 0 ? BarTransitions.MODE_TRANSLUCENT
                : (vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0 ? BarTransitions.MODE_LIGHTS_OUT
                : BarTransitions.MODE_OPAQUE;
    }

    private void checkBarModes() {
        int sbMode = mStatusBarMode;
        if (TabletKatModule.recentsMod.isOverlayShowing()) {
            sbMode = BarTransitions.MODE_OPAQUE;
        }
        checkBarMode(sbMode, mStatusBarWindowState, mStatusBarView.getBarTransitions());
    }

    private void checkBarMode(int mode, int windowState, BarTransitions transitions) {
        final boolean anim = (mScreenOn == null || mScreenOn) && windowState != StatusBarManager.WINDOW_STATE_HIDDEN;
        transitions.transitionTo(mode, anim);
    }

    private void finishBarAnimations() {
        mStatusBarView.getBarTransitions().finishAnimations();
    }

    private final Runnable mAutohide = new Runnable() {
        @Override
        public void run() {
            int requested = mSystemUiVisibility & ~ViewConstants.NAVIGATION_BAR_TRANSIENT;
            if (mSystemUiVisibility != requested) {
                notifyUiVisibilityChanged(requested);
            }
        }};

    private final Runnable mCheckBarModes = new Runnable() {
        @Override
        public void run() {
            checkBarModes();
        }};

    @Override
    public void onStart() {
        if (!mIsTv) {
            return;
        }

        super.onStart();
        mHeadsUpObserver.onChange(true); // set up
        if (ENABLE_HEADS_UP) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(SETTING_HEADS_UP), true,
                    mHeadsUpObserver);
        }
    }

    private void replaceDimen(final XResources res, final XModuleResources res2, final int id, final String str, final boolean inline){
        final float orig = inline ? 0 :
                res.getDimension(res.getIdentifier(str, "dimen", TabletKatModule.SYSTEMUI_PACKAGE));
        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "dimen", str, new CustomDimenReplacement() {
                    @Override
                    protected float getValue() {
                        if (mIsTv) {
                            return res2.getDimension(id);
                        }
                        return !inline ? orig :
                                res.getDimension(res.getIdentifier(str, "dimen", TabletKatModule.SYSTEMUI_PACKAGE));
                    }
                }
        );
    }

    @Override
    public void initResources(final XResources res, final XModuleResources res2) {
        super.initResources(res, res2);

        try {
            res.hookLayout(TabletKatModule.SYSTEMUI_PACKAGE, "layout", "heads_up", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(LayoutInflatedParam param) throws Throwable {
                    if (!mIsTv) {
                        return;
                    }

                    ViewGroup v = (ViewGroup) param.view;
                    DisplayMetrics d = v.getResources().getDisplayMetrics();

                    int content_slider = param.res.getIdentifier("content_slider", "id", TabletKatModule.SYSTEMUI_PACKAGE);
                    View contentSlider = v.findViewById(content_slider);
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)
                            contentSlider.getLayoutParams();

                    params.gravity = Gravity.END;
                    params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 478, d);
                    params.setMarginStart(0);
                    params.setMarginEnd(0);

                    contentSlider.setLayoutParams(params);
                    }
            });
        } catch (Resources.NotFoundException e){
            ENABLE_HEADS_UP = false;
        }

        replaceDimen(res, res2, R.dimen.system_bar_icon_drawing_size, "status_bar_icon_drawing_size", false);
        replaceDimen(res, res2, R.dimen.navbar_search_panel_height, "navbar_search_panel_height", true);
        replaceDimen(res, res2, R.dimen.navbar_search_outerring_diameter, "navbar_search_outerring_diameter", true);
        replaceDimen(res, res2, R.dimen.navbar_search_outerring_radius, "navbar_search_outerring_radius", true);

        res.hookLayout(TabletKatModule.SYSTEMUI_PACKAGE, "layout", "status_bar_search_panel", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam param) throws Throwable {
                if (!mIsTv){
                    return;
                }

                ViewGroup v = (ViewGroup) param.view;

                DisplayMetrics dm = v.getContext().getResources().getDisplayMetrics();
                int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -150, dm);

                int glow_pad_view = param.res.getIdentifier("glow_pad_view", "id", TabletKatModule.SYSTEMUI_PACKAGE);
                View glowpad = v.findViewById(glow_pad_view);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) glowpad.getLayoutParams();
                params.setMarginStart(margin);
                params.gravity = Gravity.START | Gravity.BOTTOM;
                glowpad.setLayoutParams(params);
                XposedHelpers.setIntField(glowpad, "mGravity", Gravity.TOP | Gravity.END);
            }
        });
    }

    private class NotificationIconTouchListener implements View.OnTouchListener {
        VelocityTracker mVT;
        int mPeekIndex;
        float mInitialTouchX, mInitialTouchY;
        int mTouchSlop;
        public NotificationIconTouchListener() {
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }
        public boolean onTouch(View v, MotionEvent event) {
            boolean peeking = mNotificationPeekWindow.getVisibility() != View.GONE;
            boolean panelShowing = mNotificationPanel.isShowing();
            if (panelShowing) return false;
            int numIcons = mIconLayout.getChildCount();
            int newPeekIndex = (int)(event.getX() * numIcons / mIconLayout.getWidth());
            if (newPeekIndex > numIcons - 1) newPeekIndex = numIcons - 1;
            else if (newPeekIndex < 0) newPeekIndex = 0;
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mVT = VelocityTracker.obtain();
                    mInitialTouchX = event.getX();
                    mInitialTouchY = event.getY();
                    mPeekIndex = -1;
                    // fall through
                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_MOVE:
                    // peek and switch icons if necessary
                    if (newPeekIndex != mPeekIndex) {
                        mPeekIndex = newPeekIndex;
                        if (DEBUG) Log.d(TAG, "will peek at notification #" + mPeekIndex);
                        Message peekMsg = mHandler.obtainMessage(MSG_OPEN_NOTIFICATION_PEEK);
                        peekMsg.arg1 = mPeekIndex;
                        mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
                        if (peeking) {
                            // no delay if we're scrubbing left-right
                            mHandler.sendMessage(peekMsg);
                        } else {
                            // wait for fling
                            mHandler.sendMessageDelayed(peekMsg, NOTIFICATION_PEEK_HOLD_THRESH);
                        }
                    }
                    // check for fling
                    if (mVT != null) {
                        mVT.addMovement(event);
                        mVT.computeCurrentVelocity(1000); // pixels per second
                        // require a little more oomph once we're already in peekaboo mode
                        if (!panelShowing && (
                                (peeking && mVT.getYVelocity() < -mNotificationFlingVelocity*3)
                                        || (mVT.getYVelocity() < -mNotificationFlingVelocity))) {
                            mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
                            mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PANEL);
                            mHandler.sendEmptyMessage(MSG_CLOSE_NOTIFICATION_PEEK);
                            mHandler.sendEmptyMessage(MSG_OPEN_NOTIFICATION_PANEL);
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
                    if (!peeking) {
                        if (action == MotionEvent.ACTION_UP
                                // was this a sloppy tap?
                                && Math.abs(event.getX() - mInitialTouchX) < mTouchSlop
                                && Math.abs(event.getY() - mInitialTouchY) < (mTouchSlop / 3)
                                // dragging off the bottom doesn't count
                                && (int)event.getY() < v.getBottom()) {
                            Message peekMsg = mHandler.obtainMessage(MSG_OPEN_NOTIFICATION_PEEK);
                            peekMsg.arg1 = mPeekIndex;
                            mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PEEK);
                            mHandler.sendMessage(peekMsg);
                            v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                            v.playSoundEffect(SoundEffectConstants.CLICK);
                            peeking = true; // not technically true yet, but the next line will run
                        }
                    }
                    if (peeking) {
                        resetNotificationPeekFadeTimer();
                    }
                    mVT.recycle();
                    mVT = null;
                    return true;
            }
            return false;
        }
    }
}
