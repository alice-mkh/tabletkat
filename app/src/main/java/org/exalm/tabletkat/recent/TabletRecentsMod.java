package org.exalm.tabletkat.recent;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.R;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;

public class TabletRecentsMod implements IMod {
    @Override
    public void addHooks(ClassLoader cl) {
        final Class recentsPanelViewClass = findClass("com.android.systemui.recent.RecentsPanelView", cl);
        final Class recentsPanelViewTaskDescriptionAdapterClass = findClass("com.android.systemui.recent.RecentsPanelView.TaskDescriptionAdapter", cl);
        final Class recentTasksLoaderClass = findClass("com.android.systemui.recent.RecentTasksLoader", cl);

        XposedHelpers.findAndHookConstructor(recentsPanelViewClass, Context.class, AttributeSet.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                setIntField(param.thisObject, "mRecentItemLayoutId", TkR.layout.system_bar_recent_item);
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                FrameLayout f = (FrameLayout) param.thisObject;
                ((TextView) f.findViewById(TkR.id.no_recent_apps)).setText(SystemR.string.status_bar_no_recent_apps);
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "updateValuesFromResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                Object self = param.thisObject;
                final Resources res = ((Context) getObjectField(self, "mContext")).getResources();
                setIntField(self, "mThumbnailWidth", Math.round(res.getDimension(TkR.dimen.status_bar_recents_thumbnail_width)));
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewTaskDescriptionAdapterClass, "createView", ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                View convertView = (View) param.getResult();
                FrameLayout f = (FrameLayout) convertView.findViewById(SystemR.id.app_thumbnail);
                final Resources res = convertView.getContext().getResources();

                f.setBackground(res.getDrawable(SystemR.drawable.recents_thumbnail_bg));
                f.setForeground(res.getDrawable(SystemR.drawable.recents_thumbnail_fg));
            }
        });
        XposedHelpers.findAndHookMethod(recentTasksLoaderClass, "getFirstTask", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                return null;
            }
        });
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
        res.setReplacement(TabletKatModule.SYSTEMUI_PACKAGE, "layout", "status_bar_recent_panel", res2.fwd(R.layout.system_bar_recent_panel));
    }
}
