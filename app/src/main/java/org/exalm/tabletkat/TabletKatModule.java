package org.exalm.tabletkat;

import android.content.Context;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.util.Log;
import android.view.Display;

import org.exalm.tabletkat.recent.TabletRecentsMod;
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

public class TabletKatModule implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {
    public static final String TAG = "TabletKatModule";
    public static final boolean DEBUG = true;

    public static final String SYSTEMUI_PACKAGE = "com.android.systemui";

    private static TabletStatusBarMod statusBarMod;
    private static TabletRecentsMod recentsMod;

    public static Class mBaseStatusBarClass;
    public static Class mBaseStatusBarHClass;
    public static Class mBatteryControllerClass;
    public static Class mBatteryMeterViewClass;
    public static Class mBluetoothControllerClass;
    public static Class mBrightnessControllerClass;
    public static Class mClockClass;
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
    public static Class mPhoneStatusBarPolicyClass;
    public static Class mStatusBarIconClass;
    public static Class mStatusBarIconViewClass;
    public static Class mSystemUIClass;
    public static Class mToggleSliderClass;
    public static Class mTvStatusBarClass;

    private static String MODULE_PATH = null;
    private static XSharedPreferences pref;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        pref = new XSharedPreferences("org.exalm.tabletkat");
        pref.makeWorldReadable();

        if (checkIsDisabled()){
            return;
        }

        Class c = findClass("com.android.internal.policy.impl.PhoneWindowManager", null);
        findAndHookMethod(c, "setInitialDisplaySize", Display.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, "mHasNavigationBar", true);
                setBooleanField(param.thisObject, "mNavigationBarCanMove", false);
            }
        });

        XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, null);
        XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker", res2.fwd(R.layout.status_bar_latest_event_ticker));
        XResources.setSystemWideReplacement("android", "layout", "status_bar_latest_event_ticker_large_icon", res2.fwd(R.layout.status_bar_latest_event_ticker_large_icon));
    }

    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
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
        mBaseStatusBarClass = findClass("com.android.systemui.statusbar.BaseStatusBar", loadPackageParam.classLoader);
        mBaseStatusBarHClass = findClass("com.android.systemui.statusbar.BaseStatusBar.H", loadPackageParam.classLoader);
        mBatteryControllerClass = findClass("com.android.systemui.statusbar.policy.BatteryController", loadPackageParam.classLoader);
        mBatteryMeterViewClass = findClass("com.android.systemui.BatteryMeterView", loadPackageParam.classLoader);
        mBluetoothControllerClass = findClass("com.android.systemui.statusbar.policy.BluetoothController", loadPackageParam.classLoader);
        mBrightnessControllerClass = findClass("com.android.systemui.settings.BrightnessController", loadPackageParam.classLoader);
        mClockClass = findClass("com.android.systemui.statusbar.policy.Clock", loadPackageParam.classLoader);
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
        mPhoneStatusBarPolicyClass = findClass("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", loadPackageParam.classLoader);
        mStatusBarIconClass = findClass("com.android.internal.statusbar.StatusBarIcon", loadPackageParam.classLoader);
        mStatusBarIconViewClass = findClass("com.android.systemui.statusbar.StatusBarIconView", loadPackageParam.classLoader);
        mSystemUIClass = findClass("com.android.systemui.SystemUI", loadPackageParam.classLoader);
        mToggleSliderClass = findClass("com.android.systemui.settings.ToggleSlider", loadPackageParam.classLoader);
        mTvStatusBarClass = findClass("com.android.systemui.statusbar.tv.TvStatusBar", loadPackageParam.classLoader);

        if (checkIsDisabled()){
            return;
        }

        statusBarMod.addHooks(loadPackageParam.classLoader);
        recentsMod.addHooks(loadPackageParam.classLoader);

        final Class systemBarsClass = findClass("com.android.systemui.statusbar.SystemBars", loadPackageParam.classLoader);
        findAndHookMethod(systemBarsClass, "createStatusBarFromConfig", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Object self = methodHookParam.thisObject;
                Context mContext = (Context) getObjectField(self, "mContext");
                Object mComponents = getObjectField(self, "mComponents");

                if (DEBUG) Log.d(TAG, "createStatusBarFromConfig");
                String clsName = "com.android.systemui.statusbar.tv.TvStatusBar";

                if (clsName == null || clsName.length() == 0) {
                    clsName = "com.android.systemui.statusbar.phone.PhoneStatusBar";
                    setObjectField(self, "mStatusBar", createStatusBar(clsName, mContext, mComponents));
                    return null;
                }

                setObjectField(self, "mStatusBar", createStatusBar(clsName, mContext, mComponents));
                return null;
            }
        });

    }

    private boolean checkIsDisabled() {
        pref.reload();
        return !pref.getBoolean("enable_tablet_ui", false);
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
        statusBarMod.init(mStatusBar);
        callMethod(mStatusBar, "start");
        if (DEBUG) Log.d(TAG, "started " + mStatusBar.getClass().getSimpleName());
        return mStatusBar;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam initPackageResourcesParam) throws Throwable {
        if (!initPackageResourcesParam.packageName.equals(SYSTEMUI_PACKAGE)){
            return;
        }

        if (checkIsDisabled()){
            return;
        }

        if (statusBarMod == null){
            statusBarMod = new TabletStatusBarMod();
        }
        if (recentsMod == null){
            recentsMod = new TabletRecentsMod();
        }
        final XResources res = initPackageResourcesParam.res;
        debug("Replacing SystemUI resources");
        SystemR.init(res);

        XModuleResources res2 = XModuleResources.createInstance(MODULE_PATH, initPackageResourcesParam.res);

        TkR.init(res, res2);

        statusBarMod.initResources(res, res2);
        recentsMod.initResources(res, res2);
    }

    public static void debug(String s){
        if (!DEBUG){
            return;
        }
        XposedBridge.log(TAG + ": " + s);
    }

    public static Object invokeOriginalMethod(XC_MethodHook.MethodHookParam param) throws IllegalAccessException, InvocationTargetException{
        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
    }
}
