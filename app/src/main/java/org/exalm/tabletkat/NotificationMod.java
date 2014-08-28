package org.exalm.tabletkat;

import android.content.res.XModuleResources;
import android.content.res.XResources;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class NotificationMod implements IMod {
    @Override
    public void addHooks(ClassLoader cl) {
        Class notification = XposedHelpers.findClass("android.app.Notification.Builder", cl);
        XposedHelpers.findAndHookMethod(notification, "buildUnstyled", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {

            }
        });
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
    }
}
