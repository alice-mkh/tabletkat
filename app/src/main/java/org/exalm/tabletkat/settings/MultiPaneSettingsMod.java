package org.exalm.tabletkat.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.R;
import org.exalm.tabletkat.TabletKatModule;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MultiPaneSettingsMod implements IMod {
    private static int id_loading_container;
    private static int id_storage_color_bar;
    private static int id_tabs;

    @Override
    public void addHooks(ClassLoader cl) {
        Class dataUsageSummaryClass = XposedHelpers.findClass("com.android.settings.DataUsageSummary", cl);
        final Class homeSettingsClass = XposedHelpers.findClass("com.android.settings.HomeSettings", cl);
        Class manageApplicationsClass = XposedHelpers.findClass("com.android.settings.applications.ManageApplications", cl);
        Class manageApplicationsTabInfoClass = XposedHelpers.findClass("com.android.settings.applications.ManageApplications.TabInfo", cl);
        final Class settingsClass = XposedHelpers.findClass("com.android.settings.Settings", cl);

        XposedHelpers.findAndHookMethod(settingsClass , "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                a.setTheme(android.R.style.Theme_DeviceDefault);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Activity a = (Activity) param.thisObject;
                View v = a.findViewById(android.R.id.list);
                View v2 = (View) v.getParent();

                int top = v2.getPaddingTop();
                int bottom = v2.getPaddingBottom();
                v.setPaddingRelative(v.getPaddingStart(), top, v.getPaddingEnd(), bottom);
                v2.setPaddingRelative(v2.getPaddingStart(), 0, v2.getPaddingEnd(), 0);
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onApplyThemeResource",
                Resources.Theme.class, int.class, boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                if (!settingsClass.isInstance(a)){
                    return;
                }
                ActionBar ab = a.getActionBar();
                Context newContext = new ContextThemeWrapper(a, android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
                XposedHelpers.setObjectField(ab, "mThemedContext", newContext);
            }
        });

        XposedHelpers.findAndHookMethod(settingsClass, "isValidFragment", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean b = (Boolean) param.getResult();
                param.setResult(b || param.args[0].equals(homeSettingsClass.getName()));
            }
        });

        XposedHelpers.findAndHookMethod(settingsClass, "onIsMultiPane", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return isMultiPane((Resources) XposedHelpers.callMethod(methodHookParam.thisObject, "getResources"));
            }
        });

        XposedHelpers.findAndHookMethod(manageApplicationsClass, "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup rootView = (ViewGroup) param.getResult();

                int i = (int) rootView.getContext().getResources().getDimension(com.android.internal.R.dimen.preference_fragment_padding_side); //TODO

                View tabs = rootView.findViewById(id_tabs);
                tabs.setPadding(i, 0, i, 0);
            }
        });

        XposedHelpers.findAndHookMethod(manageApplicationsTabInfoClass, "build",
                LayoutInflater.class, ViewGroup.class, View.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ViewGroup mRootView = (ViewGroup) param.getResult();
                View v = mRootView.findViewById(id_loading_container);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();

                int i = (int) v.getContext().getResources().getDimension(com.android.internal.R.dimen.preference_fragment_padding_side); //TODO

                params.leftMargin = i;
                params.rightMargin = i;

                v.setLayoutParams(params);
                v.requestLayout();

                DisplayMetrics d = v.getContext().getResources().getDisplayMetrics();
                int end = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, d);
                int bottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, d);

                View v2 = mRootView.findViewById(id_storage_color_bar);
                if (v2 != null) {
                    v2.setPaddingRelative(v2.getPaddingStart(), v2.getPaddingTop(), end, bottom);
                }
            }
        });

        XposedHelpers.findAndHookMethod(dataUsageSummaryClass, "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                ListView mListView = (ListView) XposedHelpers.getObjectField(param.thisObject, "mListView");
                View mHeader = (View) XposedHelpers.getObjectField(param.thisObject, "mHeader");
                Object mAdapter = XposedHelpers.getObjectField(param.thisObject, "mAdapter");

                final boolean shouldInset = mListView.getScrollBarStyle()
                        == View.SCROLLBARS_OUTSIDE_OVERLAY;

                int mInsetSide = 0;
                if (shouldInset) {
                    mInsetSide = mListView.getResources().getDimensionPixelOffset(
                        com.android.internal.R.dimen.preference_fragment_padding_side); //TODO
                }
                XposedHelpers.setIntField(param.thisObject, "mInsetSide", mInsetSide);

                if (mInsetSide > 0) {
                    // inset selector and divider drawables
                    XposedHelpers.callMethod(param.thisObject, "insetListViewDrawables", mListView, mInsetSide);
                    mHeader.setPaddingRelative(mInsetSide, 0, mInsetSide, 0);
                }

                XposedHelpers.setIntField(mAdapter, "mInsetSide", mInsetSide);
            }
        });
}

    @Override
    public void initResources(XResources res, XModuleResources res2) {
        id_loading_container = res.getIdentifier("loading_container", "id", TabletKatModule.SETTINGS_PACKAGE);
        id_tabs = res.getIdentifier("tabs", "id", TabletKatModule.SETTINGS_PACKAGE);
        id_storage_color_bar = res.getIdentifier("storage_color_bar", "id", TabletKatModule.SETTINGS_PACKAGE);

        res.setReplacement(TabletKatModule.SETTINGS_PACKAGE, "dimen", "settings_side_margin",
                res2.fwd(com.android.internal.R.dimen.preference_fragment_padding_side)); //TODO
        res.setReplacement(TabletKatModule.SETTINGS_PACKAGE, "dimen", "pager_tabs_padding",
                res2.fwd(com.android.internal.R.dimen.preference_fragment_padding_side)); //TODO
    }

    private boolean isMultiPane(Resources r){
        return r.getBoolean(com.android.internal.R.bool.preferences_prefer_dual_pane); //TODO
    }
}
