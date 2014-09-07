package org.exalm.tabletkat.recent;

import android.app.Activity;
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
        final Class recentsActivityClass = findClass("com.android.systemui.recent.RecentsActivity", cl);
        final Class recentsPanelViewClass = findClass("com.android.systemui.recent.RecentsPanelView", cl);
        final Class recentsPanelViewTaskDescriptionAdapterClass = findClass("com.android.systemui.recent.RecentsPanelView.TaskDescriptionAdapter", cl);
        final Class recentTasksLoaderClass = findClass("com.android.systemui.recent.RecentTasksLoader", cl);

        XposedHelpers.findAndHookConstructor(recentsPanelViewClass, Context.class, AttributeSet.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!shouldUseTabletRecents()){
                    return;
                }
                setIntField(param.thisObject, "mRecentItemLayoutId", TkR.layout.system_bar_recent_item);
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (!shouldUseTabletRecents()){
                    return;
                }
                FrameLayout f = (FrameLayout) param.thisObject;
                ((TextView) f.findViewById(TkR.id.no_recent_apps)).setText(SystemR.string.status_bar_no_recent_apps);
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "updateValuesFromResources", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (!shouldUseTabletRecents()){
                    return;
                }
                Object self = param.thisObject;
                Resources res = ((Context) getObjectField(self, "mContext")).getResources();
                setIntField(self, "mThumbnailWidth", Math.round(res.getDimension(TkR.dimen.system_bar_recents_thumbnail_width)));
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewTaskDescriptionAdapterClass, "createView", ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (!shouldUseTabletRecents()){
                    return;
                }
                View convertView = (View) param.getResult();
                FrameLayout f = (FrameLayout) convertView.findViewById(SystemR.id.app_thumbnail);
                Resources res = convertView.getContext().getResources();

                f.setBackground(res.getDrawable(SystemR.drawable.recents_thumbnail_bg));
                f.setForeground(res.getDrawable(SystemR.drawable.recents_thumbnail_fg));
            }
        });
        try {
            XposedHelpers.findAndHookMethod(recentTasksLoaderClass, "getFirstTask", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                    if (!shouldUseTabletRecents()){
                        return TabletKatModule.invokeOriginalMethod(methodHookParam);
                    }
                    return null;
                }
            });
        }catch (NoSuchMethodError e){}
        XposedHelpers.findAndHookMethod(Activity.class, "setContentView", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!recentsActivityClass.isInstance(param.thisObject)) {
                    return;
                }
                if (!shouldUseTabletRecents()){
                    return;
                }
                param.args[0] = TkR.layout.system_bar_recent_panel;
            }
        });
    }

    private boolean shouldUseTabletRecents(){
        return TabletKatModule.shouldUseTabletRecents();
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
    }
}
