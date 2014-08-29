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
import android.app.ActivityManagerNative;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.IWindowManager;
import android.view.KeyEvent;
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

import org.exalm.tabletkat.R;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.ViewHelper;
import org.exalm.tabletkat.statusbar.BaseStatusBarMod;
import org.exalm.tabletkat.statusbar.CommandQueue;
import org.exalm.tabletkat.statusbar.DoNotDisturb;
import org.exalm.tabletkat.statusbar.phone.BarTransitions;
import org.exalm.tabletkat.statusbar.policy.CompatModeButton;
import org.exalm.tabletkat.statusbar.policy.EventHole;
import org.exalm.tabletkat.statusbar.policy.Prefs;
import org.exalm.tabletkat.statusbar.policy.TabletBluetoothController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
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
    private boolean mAltBackButtonEnabledForIme;

    private LinearLayout mStatusIcons;

    ViewGroup mFeedbackIconArea; // notification icons, IME icon, compat icon
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
        mAltBackButtonEnabledForIme = false;
        mStatusIcons = null;
        mFeedbackIconArea = null;
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
    }

    public Context getContext() {
        return mContext;
    }

    private Runnable mShowSearchPanel = new Runnable() {
        public void run() {
            showSearchPanel();
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
                    break;
            }
            return false;
        }
    };

    private void addStatusBarWindow() {
        final View sb = makeStatusBarView();

        mWindowManager = (WindowManager) XposedHelpers.getObjectField(self, "mWindowManager");

        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);

        if (ActivityManager.isHighEndGfx()) {
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
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
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
//        lp.windowAnimations = com.android.internal.R.style.Animation; // == no animation
        lp.windowAnimations = com.android.internal.R.style.Animation_ZoomButtons; // simple fade

        mWindowManager.addView(mNotificationPanel, lp);

        // Search Panel
        mStatusBarView.setBar(self);
        mHomeButton.setOnTouchListener(mHomeSearchActionListener);
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
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.setTitle("InputMethodsPanel");
        lp.windowAnimations = com.android.internal.R.style.Animation;
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
                250,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
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

        mPile = mNotificationPanel.findViewById(SystemR.id.content);
        XposedHelpers.setObjectField(self, "mPile", mPile);
        XposedHelpers.callMethod(mPile, "removeAllViews");
        XposedHelpers.callMethod(mPile, "setLongPressListener", getNotificationLongClicker());

        ScrollView scroller = (ScrollView) XposedHelpers.callMethod(mPile, "getParent");
        scroller.setFillViewport(true);
    }

    private int getNotificationPanelHeight() {
        final Resources res = mContext.getResources();
        final Display d = mWindowManager.getDefaultDisplay();
        final Point size = new Point();
        d.getRealSize(size);
        int y = res.getDimensionPixelSize(TkR.dimen.notification_panel_min_height);
        return Math.max(y, size.y);
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
        int newNavIconWidth = res.getDimensionPixelSize(TkR.dimen.system_bar_navigation_menu_key_width);
        int newMenuNavIconWidth = res.getDimensionPixelSize(TkR.dimen.system_bar_navigation_menu_key_width);

        if (mNavigationArea != null && newNavIconWidth != mNavIconWidth) {
            mNavIconWidth = newNavIconWidth;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    mNavIconWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            mBackButton.setLayoutParams(lp);
            mHomeButton.setLayoutParams(lp);
            mRecentButton.setLayoutParams(lp);
        }

        if (mNavigationArea != null && newMenuNavIconWidth != mMenuNavIconWidth) {
            mMenuNavIconWidth = newMenuNavIconWidth;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    mMenuNavIconWidth, ViewGroup.LayoutParams.MATCH_PARENT);
            mMenuButton.setLayoutParams(lp);
        }

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

    protected View makeStatusBarView() {
        mWindowManagerService = (IWindowManager) XposedHelpers.getObjectField(self, "mWindowManagerService");
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
        final TabletStatusBarView sb = (TabletStatusBarView) ViewHelper.replaceView(temp, new TabletStatusBarView(context));

        finalizeStatusBarView(sb);

        sb.getBarTransitions().init();

        mStatusBarView = sb;

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
        mNotificationArea.setOnTouchListener(new NotificationTriggerTouchListener());

        // the button to open the notification area
        mNotificationTrigger = sb.findViewById(TkR.id.notificationTrigger);

        // the more notifications icon
        mNotificationIconArea = (NotificationIconArea)sb.findViewById(SystemR.id.notificationIcons);

        mStatusIcons = (LinearLayout)sb.findViewById(SystemR.id.statusIcons);

        // where the icons go
        mIconLayout = (NotificationIconArea.IconLayout) sb.findViewById(TkR.id.icons);

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
        mNavigationArea = (ViewGroup) sb.findViewById(TkR.id.navigationArea);
        mHomeButton = mNavigationArea.findViewById(SystemR.id.home);
        mMenuButton = mNavigationArea.findViewById(SystemR.id.menu);
        mRecentButton = mNavigationArea.findViewById(SystemR.id.recent_apps);
        mRecentButton.setOnClickListener(mOnClickListener);

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

        // The bar contents buttons
        mFeedbackIconArea = (ViewGroup)sb.findViewById(TkR.id.feedbackIconArea);
        mInputMethodSwitchButton = (InputMethodButton) sb.findViewById(TkR.id.imeSwitchButton);
        // Overwrite the lister
        mInputMethodSwitchButton.setOnClickListener(mOnClickListener);

        mCompatModeButton = (CompatModeButton) sb.findViewById(TkR.id.compatModeButton);
        mCompatModeButton.setOnClickListener(mOnClickListener);
        mCompatModeButton.setVisibility(View.GONE);

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
                                mBarService.setSystemUiVisibility(0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
                            } catch (RemoteException ex) {
                                // system process dead
                            }
                        }
                        return false;
                    }
                });

        // tuning parameters
        final int LIGHTS_GOING_OUT_SYSBAR_DURATION = 750;
        final int LIGHTS_GOING_OUT_SHADOW_DURATION = 750;
        final int LIGHTS_GOING_OUT_SHADOW_DELAY    = 0;

        final int LIGHTS_COMING_UP_SYSBAR_DURATION = 200;
//        final int LIGHTS_COMING_UP_SYSBAR_DELAY    = 50;
        final int LIGHTS_COMING_UP_SHADOW_DURATION = 0;

        LayoutTransition xition = new LayoutTransition();
        xition.setAnimator(LayoutTransition.APPEARING,
                ObjectAnimator.ofFloat(null, "alpha", 0.5f, 1f));
        xition.setDuration(LayoutTransition.APPEARING, LIGHTS_COMING_UP_SYSBAR_DURATION);
        xition.setStartDelay(LayoutTransition.APPEARING, 0);
        xition.setAnimator(LayoutTransition.DISAPPEARING,
                ObjectAnimator.ofFloat(null, "alpha", 1f, 0f));
        xition.setDuration(LayoutTransition.DISAPPEARING, LIGHTS_GOING_OUT_SYSBAR_DURATION);
        xition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        ((ViewGroup)sb.findViewById(TkR.id.bar_contents_holder)).setLayoutTransition(xition);

        xition = new LayoutTransition();
        xition.setAnimator(LayoutTransition.APPEARING,
                ObjectAnimator.ofFloat(null, "alpha", 0f, 1f));
        xition.setDuration(LayoutTransition.APPEARING, LIGHTS_GOING_OUT_SHADOW_DURATION);
        xition.setStartDelay(LayoutTransition.APPEARING, LIGHTS_GOING_OUT_SHADOW_DELAY);
        xition.setAnimator(LayoutTransition.DISAPPEARING,
                ObjectAnimator.ofFloat(null, "alpha", 1f, 0f));
        xition.setDuration(LayoutTransition.DISAPPEARING, LIGHTS_COMING_UP_SHADOW_DURATION);
        xition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        ((ViewGroup)sb.findViewById(TkR.id.bar_shadow_holder)).setLayoutTransition(xition);

        // set the initial view visibility
        setAreThereNotifications();

        // receive broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mBroadcastReceiver, filter);

        return sb;
    }

    public void setClockFont(TextView clock){
        //TODO: Make an option
        boolean useOldFont = false;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) clock.getLayoutParams();
        if (useOldFont){
            params.setMarginStart(8);
            params.bottomMargin = 3;
            clock.setTypeface(Typeface.createFromFile("/system/fonts/AndroidClock_Solid.ttf"));
            clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
        }else{
            params.setMarginStart(6);
            params.bottomMargin = 0;
            clock.setTypeface(Typeface.DEFAULT);
            clock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        }
        clock.setLayoutParams(params);
        clock.requestLayout();
    }

    //TODO: Refactor
    private void finalizeStatusBarView(TabletStatusBarView v) {
        TextView clock = (TextView) XposedHelpers.newInstance(TabletKatModule.mClockClass, mContext);
        ViewHelper.replaceView(v, SystemR.id.clock, clock);
        clock.setTextColor(mContext.getResources().getColor(SystemR.color.status_bar_clock_color));
        setClockFont(clock);

        FrameLayout f = (FrameLayout) v.findViewById(SystemR.id.signal_cluster);
        ViewStub cluster = new ViewStub(mLargeIconContext);
        ViewHelper.replaceView(f.getChildAt(0), cluster);
        cluster.setLayoutResource(SystemR.layout.signal_cluster_view);
        cluster.inflate();

        ViewHelper.replaceView(v, SystemR.id.battery, (View) XposedHelpers.newInstance(TabletKatModule.mBatteryMeterViewClass, mLargeIconContext));

        ViewGroup view = (ViewGroup)v.findViewById(TkR.id.navigationArea);

        view.removeAllViews();

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
            int width = ids[i] == SystemR.id.menu ? mMenuNavIconWidth : mNavIconWidth;
            View button = (View) XposedHelpers.newInstance(TabletKatModule.mKeyButtonViewClass, mContext, null);
            button.setId(ids[i]);
            button.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));

            ((ImageView) button).setImageResource(src[i]);
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

        ((ImageView) v.findViewById(TkR.id.dot0)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);
        ((ImageView) v.findViewById(TkR.id.dot1)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_large);
        ((ImageView) v.findViewById(TkR.id.dot2)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);
        ((ImageView) v.findViewById(TkR.id.dot3)).setImageResource(SystemR.drawable.ic_sysbar_lights_out_dot_small);
    }

    public int getStatusBarHeight() {
        return mStatusBarView != null ? mStatusBarView.getHeight()
                : mContext.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.navigation_bar_height);
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
                    XposedHelpers.callMethod(icon, "setBackgroundColor", 0x20FFFFFF);
                }

                mNotificationPeekIndex = -1;
                mNotificationPeekKey = null;
                break;
            case MSG_OPEN_NOTIFICATION_PANEL:
                if (DEBUG) Log.d(TAG, "opening notifications panel");
                if (!mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(true, true);
                    mNotificationArea.setVisibility(View.INVISIBLE);
                    mTicker.halt();
                }
                break;
            case MSG_CLOSE_NOTIFICATION_PANEL:
                if (DEBUG) Log.d(TAG, "closing notifications panel");
                if (mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(false, true);
                    mNotificationArea.setVisibility(View.VISIBLE);
                }
                break;
            case MSG_OPEN_INPUT_METHODS_PANEL:
                if (DEBUG) Log.d(TAG, "opening input methods panel");
                if (mInputMethodsPanel != null) mInputMethodsPanel.openPanel();
                break;
            case MSG_CLOSE_INPUT_METHODS_PANEL:
                if (DEBUG) Log.d(TAG, "closing input methods panel");
                if (mInputMethodsPanel != null) mInputMethodsPanel.closePanel(false);
                break;
            case MSG_OPEN_COMPAT_MODE_PANEL:
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

    private void setNavigationVisibility(int visibility) {
        boolean disableHome = ((visibility & StatusBarManager.DISABLE_HOME) != 0);
        boolean disableRecent = ((visibility & StatusBarManager.DISABLE_RECENT) != 0);
        boolean disableBack = ((visibility & StatusBarManager.DISABLE_BACK) != 0);

        mBackButton.setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
        mHomeButton.setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
        mRecentButton.setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);

        mInputMethodSwitchButton.setScreenLocked(
                (visibility & StatusBarManager.DISABLE_SYSTEM_INFO) != 0);
    }

    private boolean hasTicker(Notification n) {
        return n.tickerView != null || !TextUtils.isEmpty(n.tickerText);
    }

    // called by TabletTicker when it's done with all queued ticks
    public void doneTicking() {
        mFeedbackIconArea.setVisibility(View.VISIBLE);
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

        checkBarModes();

        mBackButton.setImageResource(
                (0 != (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT))
                        ? SystemR.drawable.ic_sysbar_back_ime
                        : SystemR.drawable.ic_sysbar_back);
    }

    private void notifyUiVisibilityChanged(int vis) {
        try {
            mWindowManagerService.statusBarVisibilityChanged(vis);
        } catch (RemoteException ex) {
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
        lp.windowAnimations = com.android.internal.R.style.Animation_ZoomButtons; // simple fade

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
            return ActivityManagerNative.getDefault().isTopActivityImmersive();
            //Log.d(TAG, "Top activity is " + (immersive?"immersive":"not immersive"));
        } catch (RemoteException ex) {
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

    private class NotificationTriggerTouchListener implements View.OnTouchListener {
        VelocityTracker mVT;
        float mInitialTouchX, mInitialTouchY;
        int mTouchSlop;

        public NotificationTriggerTouchListener() {
            mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        private Runnable mHiliteOnR = new Runnable() { public void run() {
            mNotificationArea.setBackgroundResource(
                    com.android.internal.R.drawable.list_selector_pressed_holo_dark);
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
            mBarService.onClearAllNotifications();
        } catch (RemoteException ex) {
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
                animateCollapsePanels(flags);
            }
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOn = false;
                // no waiting!
                finishBarAnimations();
            }
            else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOn = true;
            }
        }
    };

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
                IBinder token = (IBinder) methodHookParam.args[0];
                int vis = (Integer) methodHookParam.args[1];
                int backDisposition = (Integer) methodHookParam.args[2];

                mInputMethodSwitchButton.setImeWindowStatus(token,
                        (vis & InputMethodService.IME_ACTIVE) != 0);
                updateNotificationIcons();
                mInputMethodsPanel.setImeToken(token);

                boolean altBack = (backDisposition == InputMethodService.BACK_DISPOSITION_WILL_DISMISS)
                        || ((vis & InputMethodService.IME_VISIBLE) != 0);
                mAltBackButtonEnabledForIme = altBack;

                setNavigationIconHints(
                        altBack ? (mNavigationIconHints | StatusBarManager.NAVIGATION_HINT_BACK_ALT)
                                : (mNavigationIconHints & ~StatusBarManager.NAVIGATION_HINT_BACK_ALT));

                if (FAKE_SPACE_BAR) {
                    mFakeSpaceBar.setVisibility(((vis & InputMethodService.IME_VISIBLE) != 0)
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
/*
        XposedHelpers.findAndHookMethod(tv, "getRecentsLayoutParams", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        (int) mTkContext.getResources().getDimension(TabletKatModule.mDimenStatusBarRecentsWidth),
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                lp.setTitle("RecentsPanel");
                lp.windowAnimations = com.android.internal.R.style.Animation_RecentApplications;
                lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;

                return lp;
            }
        });
*/
        XposedHelpers.findAndHookMethod(tv, "getSearchLayoutParams", ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                boolean opaque = false;
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                        (opaque ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT));
                if (ActivityManager.isHighEndGfx()) {
                    lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                } else {
                    lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                    lp.dimAmount = 0.7f;
                }
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                lp.setTitle("SearchPanel");

                lp.windowAnimations = com.android.internal.R.style.Animation_RecentApplications;
                lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
                return lp;
            }
        });

        try {
            XposedHelpers.findAndHookMethod(base, "updateSearchPanel", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!mIsTv) {
                        return;
                    }

                    mSearchPanelView = XposedHelpers.getObjectField(self, "mSearchPanelView");

                    mStatusBarView.setDelegateView((View) mSearchPanelView);
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
                // Show the ticker if one is requested. Also don't do this
                // until status bar window is attached to the window manager,
                // because...  well, what's the point otherwise?  And trying to
                // run a ticker without being attached will crash!
                if (hasTicker(n.getNotification()) && mStatusBarView.getWindowToken() != null) {
                    if (0 == (mDisabled & (StatusBarManager.DISABLE_NOTIFICATION_ICONS
                            | StatusBarManager.DISABLE_NOTIFICATION_TICKER))) {
                        mTicker.add(key, n);
                        mFeedbackIconArea.setVisibility(View.GONE);
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
                            View.NAVIGATION_BAR_TRANSIENT, View.NAVIGATION_BAR_TRANSLUCENT);
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
                    if ((vis & View.NAVIGATION_BAR_UNHIDE) != 0) {
                        mSystemUiVisibility &= ~View.NAVIGATION_BAR_UNHIDE;
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
                                null, new StatusBarNotification("", 0, "", 0, 0, Notification.PRIORITY_MAX,
                                dndNotification, android.os.Process.myUserHandle()), iconView);

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
                    if ((provisioned && n.getScore() >= HIDE_ICONS_BELOW_SCORE)
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
                        || (mDisabled & StatusBarManager.DISABLE_HOME) != 0;
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
                int viewIndex = (Integer) methodHookParam.args[2];
                Object icon = methodHookParam.args[3];

                if (DEBUG) Log.d(TAG, "addIcon(" + slot + ") -> " + icon);
//                Toast.makeText(mContext, "addIcon(" + slot + ") -> " + icon, Toast.LENGTH_SHORT).show();

                if (!allowIcon(slot)){
                    return null;
                }

                ImageView view = (ImageView) XposedHelpers.newInstance(TabletKatModule.mStatusBarIconViewClass,
                        mLargeIconContext, slot, null);

                XposedHelpers.callMethod(view, "set", icon);
                mStatusIcons.addView(view, viewIndex, new LinearLayout.LayoutParams(mIconSize, mIconSize));
                return null;
            };
        });

        XposedHelpers.findAndHookMethod(tv, "updateIcon", String.class, int.class, int.class, TabletKatModule.mStatusBarIconClass, TabletKatModule.mStatusBarIconClass, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                String slot = (String) methodHookParam.args[0];
                int viewIndex = (Integer) methodHookParam.args[2];
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
                int viewIndex = (Integer) methodHookParam.args[2];

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
                addNotificationViews(key, notification);

                final boolean immersive = isImmersive();
                if (false && immersive) {
                    // TODO: immersive mode popups for tablet
                } else if (notification.getNotification().fullScreenIntent != null) {
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

        XposedHelpers.findAndHookMethod(tv, "removeNotification", IBinder.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                IBinder key = (IBinder) methodHookParam.args[0];

                if (DEBUG) Log.d(TAG, "removeNotification(" + key + ")");
                removeNotificationViews(key);
                mTicker.remove(key);
                setAreThereNotifications();
                mNotificationPanel.updateClearButton();
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
                        | StatusBarManager.DISABLE_HOME)) != 0) {
                    setNavigationVisibility(state);

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
                mContext.unregisterReceiver(mBroadcastReceiver);
            }
        });
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
        mAutohideSuspended = (mSystemUiVisibility & View.NAVIGATION_BAR_TRANSIENT) != 0;
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
        if ((mSystemUiVisibility & View.NAVIGATION_BAR_TRANSIENT) != 0  // a transient bar is revealed
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
            if (n.getNotification().kind != null) {
                for (String aKind : n.getNotification().kind) {
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
            mBarService.setHardKeyboardEnabled(enabled);
        } catch (RemoteException ex) {
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
            int requested = mSystemUiVisibility & ~View.NAVIGATION_BAR_TRANSIENT;
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
    public void initResources(XResources res, final XModuleResources res2) {
        super.initResources(res, res2);

        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "dimen", "status_bar_icon_drawing_size", res2.fwd(R.dimen.system_bar_icon_drawing_size));

        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "dimen", "navbar_search_panel_height", res2.fwd(R.dimen.navbar_search_panel_height));
        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "dimen", "navbar_search_outerring_diameter", res2.fwd(R.dimen.navbar_search_outerring_diameter));
        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "dimen", "navbar_search_outerring_radius", res2.fwd(R.dimen.navbar_search_outerring_radius));

        //Fixing Google Now ring. We have to rotate it for landscape handsets and to move to the left for everything
        res.hookLayout(TabletKatModule.SYSTEMUI_PACKAGE, "layout", "status_bar_search_panel", new XC_LayoutInflated() {
            @Override
            public void handleLayoutInflated(LayoutInflatedParam param) throws Throwable {
                ViewGroup v = (ViewGroup) param.view;
                int glow_pad_view = param.res.getIdentifier("glow_pad_view", "id", TabletKatModule.SYSTEMUI_PACKAGE);
                int search_bg_protect = param.res.getIdentifier("search_bg_protect", "id", TabletKatModule.SYSTEMUI_PACKAGE);

                DisplayMetrics displayMetrics = v.getContext().getResources().getDisplayMetrics();
                int margin = (int) (-150F * displayMetrics.density);

                View glowpad = v.findViewById(glow_pad_view);
                RelativeLayout l2 = (RelativeLayout) v.findViewById(search_bg_protect);

                if (l2 != null){
                    View l = l2.getChildAt(0);
                    RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lparams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    l.setLayoutParams(lparams);

                    FrameLayout.LayoutParams lparams2 = (FrameLayout.LayoutParams) l2.getLayoutParams();
                    lparams2.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
                    lparams2.setMarginStart(margin);
                    l2.setLayoutParams(lparams2);

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) glowpad.getLayoutParams();
                    if (params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                        int h = params.height;
                        params.height = params.width;
                        params.width = h;

                        List mDirectionDescriptions = (List) XposedHelpers.getObjectField(glowpad, "mDirectionDescriptions");
                        swapItems((List) XposedHelpers.getObjectField(glowpad, "mTargetDrawables"), 1, 2);
                        swapItems((List) XposedHelpers.getObjectField(glowpad, "mTargetDescriptions"), 1, 2);
                        swapItems(mDirectionDescriptions, 1, 2);
                        if (mDirectionDescriptions != null) {
                            String str = res2.getString(res2.fwd(R.string.up).getId());
                            mDirectionDescriptions.set(1, str);
                        }
                    }
                    XposedHelpers.setIntField(glowpad, "mGravity", Gravity.CENTER_HORIZONTAL | Gravity.TOP);
                    glowpad.setLayoutParams(params);
               }else{
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) glowpad.getLayoutParams();
                    params.setMarginStart(margin);
                    glowpad.setLayoutParams(params);
               }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void swapItems(List l, int i1, int i2){
        if (l == null){
            return;
        }
        Object o1 = l.get(i1);
        l.set(i1, l.get(i2));
        l.set(i2, o1);
    }
}
