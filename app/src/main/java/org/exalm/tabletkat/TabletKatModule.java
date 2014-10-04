package org.exalm.tabletkat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.DisplayInfo;

import org.exalm.tabletkat.launcher.LauncherMod;
import org.exalm.tabletkat.recent.TabletRecentsMod;
import org.exalm.tabletkat.settings.MultiPaneSettingsMod;
import org.exalm.tabletkat.statusbar.tablet.TabletStatusBarMod;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class TabletKatModule implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {
    public static final String TAG = "TabletKatModule";
    public static final boolean DEBUG = true;

    public static final String SYSTEMUI_PACKAGE = "com.android.systemui";
    public static final String SETTINGS_PACKAGE = "com.android.settings";

    private static TabletStatusBarMod statusBarMod;
    public static TabletRecentsMod recentsMod;
    private static IMod settingsMod;
    private static LauncherMod launcherMod;

    public static Class mActivityManagerNativeClass;
    public static Class mBaseStatusBarClass;
    public static Class mBaseStatusBarHClass;
    public static Class mBatteryControllerClass;
    public static Class mBatteryMeterViewClass;
    public static Class mBluetoothControllerClass;
    public static Class mBrightnessControllerClass;
    public static Class mClockClass;
    public static Class mComAndroidInternalRDrawableClass;
    public static Class mComAndroidInternalRStyleClass;
    public static Class mDateViewClass;
    public static Class mDelegateViewHelperClass;
    public static Class mExpandHelperClass;
    public static Class mExpandHelperCallbackClass;
    public static Class mGlowPadViewClass;
    public static Class mKeyButtonViewClass;
    public static Class mLocationControllerClass;
    public static Class mNetworkControllerClass;
    public static Class mNotificationDataEntryClass;
    public static Class mNotificationRowLayoutClass;
    public static Class mPhoneStatusBarClass;
    public static Class mPhoneStatusBarPolicyClass;
    public static Class mRecentTasksLoaderClass;
    public static Class mStatusBarIconClass;
    public static Class mStatusBarIconViewClass;
    public static Class mStatusBarManagerClass;
    public static Class mSystemUIClass;
    public static Class mToggleSliderClass;
    public static Class mTvStatusBarClass;
    public static Class mWindowManagerGlobalClass;
    public static Class mWindowManagerLayoutParamsClass;

    public static final String ACTION_PREFERENCE_CHANGED = "org.exalm.tabletkat.PREFERENCE_CHANGED";

    private static String MODULE_PATH = null;
    private static XSharedPreferences pref;
    private static boolean mIsTabletConfiguration = true;
    private static Boolean mHasNavigationBar;

    private static Object mSystemUI;

    public static TabletKatModule self;
    private static BroadcastReceiver mReceiver;
    private static Configuration mConfiguration;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        self = this;
        MODULE_PATH = startupParam.modulePath;
        pref = new XSharedPreferences("org.exalm.tabletkat");
        pref.makeWorldReadable();

        Class c = findClass("com.android.internal.policy.impl.PhoneWindowManager", null);

        findAndHookMethod(c, "getNonDecorDisplayWidth", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                setNavigationBarProperties(methodHookParam, shouldUseTabletUI(null));
                if (shouldUseTabletUI(null)) {
                    int fullWidth = (Integer) methodHookParam.args[0];
                    return fullWidth;
                }
                return invokeOriginalMethod(methodHookParam);
            }
        });
        findAndHookMethod(c, "getNonDecorDisplayHeight", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                setNavigationBarProperties(methodHookParam, shouldUseTabletUI(null));
                if (shouldUseTabletUI(null)){
                    int fullHeight = (Integer) methodHookParam.args[1];
                    int rotation = getIntField(methodHookParam.thisObject, "mSeascapeRotation");
                    int[] mNavigationBarHeightForRotation = (int[]) getObjectField(methodHookParam.thisObject, "mNavigationBarHeightForRotation");
                    return fullHeight - mNavigationBarHeightForRotation[rotation];
                }
                return invokeOriginalMethod(methodHookParam);
            }
        });
        findAndHookMethod(c, "getConfigDisplayHeight", int.class, int.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (shouldUseTabletUI(null)){
                    int fullWidth = (Integer) methodHookParam.args[0];
                    int fullHeight = (Integer) methodHookParam.args[1];
                    int rotation = (Integer) methodHookParam.args[2];
                    return callMethod(methodHookParam.thisObject, "getNonDecorDisplayHeight", fullWidth, fullHeight, rotation);
                }
                return invokeOriginalMethod(methodHookParam);
            }
        });

        XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, null);
        try {
            XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker", res2.fwd(R.layout.status_bar_latest_event_ticker));
            XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker_large_icon", res2.fwd(R.layout.status_bar_latest_event_ticker_large_icon));
        }catch (Resources.NotFoundException e){}
    }

    private void setNavigationBarProperties(XC_MethodHook.MethodHookParam param, boolean b) {
        int width = (Integer) param.args[0];
        int height = (Integer) param.args[1];
        int shortSize = width;
        if (width > height){
            shortSize = height;
        }
        Display d = (Display) getObjectField(param.thisObject, "mDisplay");
        if (d == null){
            return;
        }
        DisplayInfo info = new DisplayInfo();
        d.getDisplayInfo(info);
        int density = info.logicalDensityDpi;

        int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / density;

        setBooleanField(param.thisObject, "mNavigationBarCanMove", shortSizeDp < 600 && !b);

        if (mHasNavigationBar == null) {
            mHasNavigationBar = getBooleanField(param.thisObject, "mHasNavigationBar");
        }
        setBooleanField(param.thisObject, "mHasNavigationBar", b || mHasNavigationBar);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals(SETTINGS_PACKAGE)){
            if (settingsMod == null){
                settingsMod = new MultiPaneSettingsMod();
            }
            if (isModEnabled("settings")) {
                settingsMod.addHooks(loadPackageParam.classLoader);
            }
            return;
        }
        if (LauncherMod.isSupported(loadPackageParam.packageName)) {
            if (isModEnabled("launcher")) {
                if (launcherMod == null || !loadPackageParam.packageName.equals(launcherMod.getPackage())) {
                    launcherMod = new LauncherMod();
                }
                launcherMod.addHooks(loadPackageParam.packageName, loadPackageParam.classLoader);
            }
            return;
        }
        if (!loadPackageParam.packageName.equals(SYSTEMUI_PACKAGE)){
            return;
        }
        debug("Loaded SystemUI");

        if (statusBarMod == null){
            statusBarMod = new TabletStatusBarMod();
        }
        if (recentsMod == null){
            recentsMod = new TabletRecentsMod();
        }
        mActivityManagerNativeClass = findClass("android.app.ActivityManagerNative", loadPackageParam.classLoader);
        mBaseStatusBarClass = findClass("com.android.systemui.statusbar.BaseStatusBar", loadPackageParam.classLoader);
        mBaseStatusBarHClass = findClass("com.android.systemui.statusbar.BaseStatusBar.H", loadPackageParam.classLoader);
        mBatteryControllerClass = findClass("com.android.systemui.statusbar.policy.BatteryController", loadPackageParam.classLoader);
        //Xperia custom battery meter
        try{
//            Class c = findClass("com.sonymobile.systemui.statusbar.BatteryImage", loadPackageParam.classLoader);
//            mBatteryMeterViewClass = c;
            mBatteryMeterViewClass = findClass("com.android.systemui.BatteryMeterView", loadPackageParam.classLoader);
        }catch (ClassNotFoundError e){
            XposedBridge.log(e);
            //Ok, it's not Xperia
            mBatteryMeterViewClass = findClass("com.android.systemui.BatteryMeterView", loadPackageParam.classLoader);
        }
        mBluetoothControllerClass = findClass("com.android.systemui.statusbar.policy.BluetoothController", loadPackageParam.classLoader);
        mBrightnessControllerClass = findClass("com.android.systemui.settings.BrightnessController", loadPackageParam.classLoader);
        mClockClass = findClass("com.android.systemui.statusbar.policy.Clock", loadPackageParam.classLoader);
        mComAndroidInternalRDrawableClass = findClass("com.android.internal.R.drawable", loadPackageParam.classLoader);
        mComAndroidInternalRStyleClass = findClass("com.android.internal.R.style", loadPackageParam.classLoader);
        mDateViewClass = findClass("com.android.systemui.statusbar.policy.DateView", loadPackageParam.classLoader);
        mDelegateViewHelperClass = findClass("com.android.systemui.statusbar.DelegateViewHelper", loadPackageParam.classLoader);
        mExpandHelperClass = findClass("com.android.systemui.ExpandHelper", loadPackageParam.classLoader);
        mExpandHelperCallbackClass = findClass("com.android.systemui.ExpandHelper.Callback", loadPackageParam.classLoader);
        mGlowPadViewClass = findClass("com.android.internal.widget.multiwaveview.GlowPadView", loadPackageParam.classLoader);
        mKeyButtonViewClass = findClass("com.android.systemui.statusbar.policy.KeyButtonView", loadPackageParam.classLoader);
        mLocationControllerClass = findClass("com.android.systemui.statusbar.policy.LocationController", loadPackageParam.classLoader);
        mNetworkControllerClass = findClass("com.android.systemui.statusbar.policy.NetworkController", loadPackageParam.classLoader);
        mNotificationDataEntryClass = findClass("com.android.systemui.statusbar.NotificationData.Entry", loadPackageParam.classLoader);
        mNotificationRowLayoutClass = findClass("com.android.systemui.statusbar.policy.NotificationRowLayout", loadPackageParam.classLoader);
        mPhoneStatusBarClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBar", loadPackageParam.classLoader);
        mPhoneStatusBarPolicyClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", loadPackageParam.classLoader);
        mStatusBarIconClass = findClass("com.android.internal.statusbar.StatusBarIcon", loadPackageParam.classLoader);
        mRecentTasksLoaderClass = findClass("com.android.systemui.recent.RecentTasksLoader", loadPackageParam.classLoader);
        mStatusBarIconViewClass = findClass("com.android.systemui.statusbar.StatusBarIconView", loadPackageParam.classLoader);
        mStatusBarManagerClass = findClass("android.app.StatusBarManager", loadPackageParam.classLoader);
        mSystemUIClass = findClass("com.android.systemui.SystemUI", loadPackageParam.classLoader);
        mToggleSliderClass = findClass("com.android.systemui.settings.ToggleSlider", loadPackageParam.classLoader);
        mTvStatusBarClass = findClass("com.android.systemui.statusbar.tv.TvStatusBar", loadPackageParam.classLoader);
        mWindowManagerGlobalClass = findClass("android.view.WindowManagerGlobal", loadPackageParam.classLoader);
        mWindowManagerLayoutParamsClass = findClass("android.view.WindowManager.LayoutParams", loadPackageParam.classLoader);

        statusBarMod.addHooks(loadPackageParam.classLoader);
        recentsMod.addHooks(loadPackageParam.classLoader);

        final Class systemBarsClass = findClass("com.android.systemui.statusbar.SystemBars", loadPackageParam.classLoader);
        findAndHookMethod(systemBarsClass, "createStatusBarFromConfig", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Object self = methodHookParam.thisObject;
                Context mContext = (Context) getObjectField(self, "mContext");
                Object mComponents = getObjectField(self, "mComponents");

                mSystemUI = self;
                mConfiguration = mContext.getResources().getConfiguration();
                refreshReceiver(mContext);

                debug("createStatusBarFromConfig");
                String clsName = "com.android.systemui.statusbar.tv.TvStatusBar";

                if (!shouldUseTabletUI(mContext.getResources().getConfiguration())) {
                    clsName = "com.android.systemui.statusbar.phone.PhoneStatusBar";
                }

                setObjectField(self, "mStatusBar", createStatusBar(clsName, mContext, mComponents));

                return null;
            }
        });

        findAndHookMethod(systemBarsClass, "onConfigurationChanged", Configuration.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                mConfiguration = (Configuration) methodHookParam.args[0];
                final Object self = methodHookParam.thisObject;
                Object mStatusBar = getObjectField(self, "mStatusBar");

                if (mStatusBar != null) {
                    boolean isTabletUI = mTvStatusBarClass.isInstance(mStatusBar);
                    boolean shouldUseTabletUI = shouldUseTabletUI(mConfiguration);
                    if (isTabletUI != shouldUseTabletUI){
                        refreshSystemUI(shouldUseTabletUI);
                    }else {
                        callMethod(mStatusBar, "onConfigurationChanged", mConfiguration);
                    }
                }
                return null;
            }
        });
    }

    private boolean shouldUseTabletUI(Configuration conf) {
        if (conf != null) {
            mIsTabletConfiguration = true;//conf.orientation == Configuration.ORIENTATION_LANDSCAPE;
        }
        if (!mIsTabletConfiguration) {
            return false;
        }
        pref.reload();
        return pref.getBoolean("enable_tablet_ui", true);
    }

    private static boolean isModEnabled(String id) {
        pref.reload();
        return pref.getBoolean("enable_mod_" + id, true);
    }

    private Object createStatusBar(String clsName, Context mContext, Object mComponents) {
        Class<?> cls;
        try {
            cls = mContext.getClassLoader().loadClass(clsName);
        } catch (Throwable t) {
            clsName = "com.android.systemui.statusbar.phone.PhoneStatusBar";
            return createStatusBar(clsName, mContext, mComponents);
        }
        Object mStatusBar;
        try {
            mStatusBar = cls.newInstance();
        } catch (Throwable t) {
            clsName = "com.android.systemui.statusbar.phone.PhoneStatusBar";
            return createStatusBar(clsName, mContext, mComponents);
        }
        setObjectField(mStatusBar, "mContext", mContext);
        setObjectField(mStatusBar, "mComponents", mComponents);
        if (mStatusBar != null) {
            statusBarMod.init(mStatusBar);
            recentsMod.setBar(mStatusBar);
            recentsMod.registerReceiver(mContext);

            callMethod(mStatusBar, "start");
            statusBarMod.onStart();
            recentsMod.createPanel(mStatusBar);
            debug("started " + mStatusBar.getClass().getSimpleName());
        }
        return mStatusBar;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (initPackageResourcesParam.packageName.equals(SETTINGS_PACKAGE)){
            if (settingsMod == null){
                settingsMod = new MultiPaneSettingsMod();
            }
            XResources res = initPackageResourcesParam.res;
            XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, initPackageResourcesParam.res);

            SystemR.init(res, res2);
            TkR.init(res, res2);

            if (isModEnabled("settings")) {
                settingsMod.initResources(res, res2);
            }
            return;
        }
        if (LauncherMod.isSupported(initPackageResourcesParam.packageName)) {
            if (isModEnabled("launcher")) {
                if (launcherMod == null || !initPackageResourcesParam.packageName.equals(launcherMod.getPackage())) {
                    launcherMod = new LauncherMod();
                }
                XResources res = initPackageResourcesParam.res;
                XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, initPackageResourcesParam.res);

                SystemR.init(res, res2);
                TkR.init(res, res2);

                launcherMod.initResources(res, res2);
            }
            return;
        }
        if (!initPackageResourcesParam.packageName.equals(SYSTEMUI_PACKAGE)){
            return;
        }

        if (statusBarMod == null){
            statusBarMod = new TabletStatusBarMod();
        }
        if (recentsMod == null){
            recentsMod = new TabletRecentsMod();
        }
        XResources res = initPackageResourcesParam.res;
        XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, initPackageResourcesParam.res);

        debug("Replacing SystemUI resources");
        SystemR.init(res, res2);
        TkR.init(res, res2);

        statusBarMod.initResources(res, res2);
        recentsMod.initResources(res, res2);
    }

    public static boolean shouldUseTabletRecents(){
        return isModEnabled("recents");
    }

    public static void debug(String s){
        if (!DEBUG){
            return;
        }
        XposedBridge.log(TAG + ": " + s);
    }

    public static BroadcastReceiver registerReceiver(Context c, final OnPreferenceChangedListener l){
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_PREFERENCE_CHANGED);
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String key = intent.getStringExtra("key");
                if (intent.hasExtra("boolValue")){
                    boolean boolValue = intent.getBooleanExtra("boolValue", false);
                    l.onPreferenceChanged(key, boolValue);
                }
                if (intent.hasExtra("intValue")){
                    int intValue = intent.getIntExtra("intValue", 0);
                    l.onPreferenceChanged(key, intValue);
                }
            }
        };
        pref.reload();
        l.init(pref);
        c.registerReceiver(r, f);
        return r;
    }

    private void refreshReceiver(Context c){
        if (mReceiver == null){
            mReceiver = registerReceiver(c, new OnPreferenceChangedListener(){
                @Override
                public void onPreferenceChanged(String key, boolean value) {
                    if (key.equals("enable_tablet_ui")){
                        refreshSystemUI(value);
                    }
                }

                @Override
                public void onPreferenceChanged(String key, int value) {

                }

                @Override
                public void init(XSharedPreferences pref) {

                }
            });
        }
    }

    public static void refreshSystemUI(boolean flag){
        if (mSystemUI == null) {
            return;
        }

        long l = (Long) callMethod(mSystemUI, "onServiceStartAttempt");
        if (l < 500L) {
            l = 500L;
        }
        Handler h = new Handler(){};
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                callMethod(mSystemUI, "createStatusBarFromConfig");
            }
        }, l);
    }

    public static Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws IllegalAccessException, InvocationTargetException{
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }
}
