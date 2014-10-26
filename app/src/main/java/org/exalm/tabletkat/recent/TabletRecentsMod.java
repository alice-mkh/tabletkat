package org.exalm.tabletkat.recent;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.exalm.tabletkat.IMod;
import org.exalm.tabletkat.OnPreferenceChangedListener;
import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.phone.BarTransitions;
import org.exalm.tabletkat.statusbar.tablet.TabletStatusBarMod;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setIntField;

public class TabletRecentsMod implements IMod {
    private BroadcastReceiver receiver;
    private boolean useTabletLayout;
    private boolean overlay;
    private RecentsPanel mRecentsPanel;
    private Object mBar;
    private TabletStatusBarMod mMod;
//    private View mRecentsTransitionBackground;
//    private ImageView mRecentsTransitionPlaceholderIcon;

    public void setBar(Object bar, TabletStatusBarMod mod) {
        mBar = bar;
        mMod = mod;
        if (mRecentsPanel != null) {
            mRecentsPanel.destroy();
        }
        mRecentsPanel = new RecentsPanel(bar, mod, useTabletLayout);
    }

    public void createPanel(Object bar, TabletStatusBarMod mod) {
        mBar = bar;
        mMod = mod;
        if (mRecentsPanel != null) {
            mRecentsPanel.destroy();
        }
        if (!overlay) {
            mRecentsPanel = null;
            return;
        }
        mRecentsPanel = new RecentsPanel(bar, mod, useTabletLayout);
    }

    public void destroy() {
        mBar = null;
        mMod = null;
        if (mRecentsPanel != null) {
            mRecentsPanel.destroy();
        }
        mRecentsPanel = null;
    }

    public void registerReceiver(Context c) {
        if (receiver != null) {
            return;
        }
        receiver = TabletKatModule.registerReceiver(c, new OnPreferenceChangedListener() {
            @Override
            public void onPreferenceChanged(String key, boolean value) {
                if (key.equals("enable_mod_recents")) {
                    useTabletLayout = value;
                    if (mRecentsPanel != null) {
                        mRecentsPanel.useTabletLayout = useTabletLayout;
                        mRecentsPanel.updateRecentsPanel();
                    }
                }
                if (key.equals("overlay_recents")) {
                    overlay = value;
                    createPanel(mBar, mMod);
                }
            }

            @Override
            public void onPreferenceChanged(String key, int value) {

            }

            @Override
            public void init(XSharedPreferences pref) {
                useTabletLayout = pref.getBoolean("enable_mod_recents", true);
                overlay = pref.getBoolean("overlay_recents", true);
            }
        });
    }

    public void onConfigurationChanged(Configuration c) {
        if (mRecentsPanel != null) {
            mRecentsPanel.updateRecentsPanel();
        }
    }

    @Override
    public void addHooks(ClassLoader cl) {
        final Class baseStatusBarClass = TabletKatModule.mBaseStatusBarClass;
        final Class recentsActivityClass = findClass("com.android.systemui.recent.RecentsActivity", cl);
        final Class recentsPanelViewClass = findClass("com.android.systemui.recent.RecentsPanelView", cl);
        final Class recentsPanelViewTaskDescriptionAdapterClass = findClass("com.android.systemui.recent.RecentsPanelView.TaskDescriptionAdapter", cl);
        final Class recentTasksLoaderClass = findClass("com.android.systemui.recent.RecentTasksLoader", cl);
        final Class fadedEdgeDrawHelperClass = findClass("com.android.systemui.recent.FadedEdgeDrawHelper", cl);

        XposedHelpers.findAndHookMethod(baseStatusBarClass, "toggleRecentsActivity", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.toggleRecentsActivity();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(baseStatusBarClass, "preloadRecentTasksList", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.preloadRecentTasksList();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(baseStatusBarClass, "cancelPreloadingRecentTasksList", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.cancelPreloadingRecentTasksList();
                return null;
            }
        });
        XposedHelpers.findAndHookMethod(baseStatusBarClass, "closeRecents", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.closeRecents();
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "onTasksLoaded", ArrayList.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                Object self = methodHookParam.thisObject;
                ArrayList tasks = (ArrayList) methodHookParam.args[0];
                boolean firstScreenful = (Boolean) methodHookParam.args[1];
                ArrayList mRecentTaskDescriptions = (ArrayList) XposedHelpers.getObjectField(self, "mRecentTaskDescriptions");

                if (mRecentTaskDescriptions == null) {
                    mRecentTaskDescriptions = new ArrayList(tasks);
                    XposedHelpers.setObjectField(self, "mRecentTaskDescriptions", mRecentTaskDescriptions);
                } else {
                    mRecentTaskDescriptions.addAll(tasks);
                }
                if (overlay && !mRecentTaskDescriptions.isEmpty() && firstScreenful) {
                    ActivityManager am = (ActivityManager) mRecentsPanel.mContext.getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                    ComponentName componentInfo = taskInfo.get(0).topActivity;
                    Object loader = XposedHelpers.callStaticMethod(TabletKatModule.mRecentTasksLoaderClass, "getInstance", mRecentsPanel.mContext);
                    boolean b = (Boolean) XposedHelpers.callMethod(loader, "isCurrentHomeActivity", componentInfo, null);

                    if (!b) {
                        mRecentTaskDescriptions.remove(0);
                    }
                }
                Context c = (Context) XposedHelpers.getObjectField(self, "mContext");
                boolean refresh = true;
                if (recentsActivityClass.isInstance(c)) {
                    refresh = (Boolean) XposedHelpers.callMethod(c, "isActivityShowing");
                }
                if (refresh) {
                    XposedHelpers.callMethod(self, "refreshViews");
                }
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "dismiss", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.closeRecents();
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "dismissAndGoBack", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (mRecentsPanel == null) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                mRecentsPanel.closeRecents();
                return null;
            }
        });

        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "showImpl", boolean.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mRecentsPanel == null) {
                    return;
                }
                mRecentsPanel.setVisibility((Boolean) param.args[0]);
            }
/*
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(mRecentsPanel, "mCallUiHiddenBeforeNextReload", false);
            }*/
        });

        XposedHelpers.findAndHookConstructor(recentsPanelViewClass, Context.class, AttributeSet.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!useTabletLayout){
                    return;
                }
                setIntField(param.thisObject, "mRecentItemLayoutId", TkR.layout.system_bar_recent_item);
            }
        });
        XposedHelpers.findAndHookMethod(recentsPanelViewClass, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                FrameLayout f = (FrameLayout) param.thisObject;
/*
                ImageView v2 = new ImageView(f.getContext());
                v2.setVisibility(View.INVISIBLE);
                ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ((ViewGroup) f.getChildAt(0)).addView(v2, 0, params2);
                mRecentsTransitionPlaceholderIcon = v2;

                View v = new View(f.getContext());
                v.setVisibility(View.INVISIBLE);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                f.addView(v, 0, params);
                v.setBackgroundColor(f.getResources().getColor(android.R.color.black));
                mRecentsTransitionBackground = v;
*/
                if (!shouldUseTabletRecents()) {
                    return;
                }
                ((TextView) f.findViewById(TkR.id.no_recent_apps)).setText(SystemR.string.status_bar_no_recent_apps);
                if (overlay) {
                    f.findViewById(TkR.id.no_recent_apps).setPadding(0, 0, 0, 0);
                }
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

        XposedHelpers.findAndHookMethod(View.class, "onKeyUp", int.class, KeyEvent.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                int keyCode = (Integer) methodHookParam.args[0];
                KeyEvent event = (KeyEvent) methodHookParam.args[1];
                if (overlay && recentsPanelViewClass.isInstance(methodHookParam.thisObject) &&
                        keyCode == KeyEvent.KEYCODE_BACK && !event.isCanceled()) {
                    XposedHelpers.callMethod(methodHookParam.thisObject, "show", false);
                    return true;
                }
                return TabletKatModule.invokeOriginalMethod(methodHookParam);

            }
        });
        try {
            XposedHelpers.findAndHookMethod(recentTasksLoaderClass, "getFirstTask", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable {
                    if (!shouldUseTabletRecents() || overlay){
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
                if (!shouldUseTabletRecents()) {
                    return;
                }
                param.args[0] = TkR.layout.system_bar_recent_panel;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity a = (Activity) param.thisObject;
                View v = a.findViewById(SystemR.id.recents_root);
//                Toast.makeText(a, "Null? "+(v==null), Toast.LENGTH_SHORT).show();
            }
        });
        XposedHelpers.findAndHookMethod(fadedEdgeDrawHelperClass, "create",
                Context.class, AttributeSet.class, View.class, boolean.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!shouldUseTabletRecents()) {
                    return TabletKatModule.invokeOriginalMethod(methodHookParam);
                }
                return null;
            }
        });

/*        XposedHelpers.findAndHookMethod(panel, "handleOnClick", View.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.args[0];

                View holder = (View) view.getTag();
                ImageView thumbnailViewImage = (ImageView) XposedHelpers.getObjectField(holder,
                        "thumbnailViewImage");
                Drawable thumbnailViewDrawable = (Drawable) XposedHelpers.getObjectField(holder,
                        "thumbnailViewDrawable");

                mRecentsTransitionBackground.setVisibility(View.VISIBLE);
                mRecentsTransitionPlaceholderIcon.setVisibility(View.VISIBLE);

                Bitmap bm = null;
                boolean usingDrawingCache = true;
                if (thumbnailViewDrawable instanceof BitmapDrawable) {
                    bm = ((BitmapDrawable) thumbnailViewDrawable).getBitmap();
                    if (bm.getWidth() == thumbnailViewImage.getWidth() &&
                            bm.getHeight() == thumbnailViewImage.getHeight()) {
                        usingDrawingCache = false;
                    }
                }
                if (usingDrawingCache) {
                    thumbnailViewImage.setDrawingCacheEnabled(true);
                    bm = thumbnailViewImage.getDrawingCache();
                }

                if (!usingDrawingCache) {
                    mRecentsTransitionPlaceholderIcon.setImageBitmap(bm);
                } else {
                    Bitmap b2 = bm.copy(bm.getConfig(), true);
                    mRecentsTransitionPlaceholderIcon.setImageBitmap(b2);
                }

                Rect r = new Rect();
                thumbnailViewImage.getGlobalVisibleRect(r);
                mRecentsTransitionPlaceholderIcon.setTranslationX(r.left);
                mRecentsTransitionPlaceholderIcon.setTranslationY(r.top);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedHelpers.callMethod(param.thisObject, "show", true);
            }
        });*/

        XposedHelpers.findAndHookMethod(TabletKatModule.mPhoneStatusBarClass, "checkBarModes", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int i = XposedHelpers.getIntField(param.thisObject, "mNavigationBarMode");
                if (isOverlayShowing()) {
                    XposedHelpers.setIntField(param.thisObject, "mNavigationBarMode", BarTransitions.MODE_OPAQUE);
                    param.setObjectExtra("mode", i);
                }
                param.setObjectExtra("overlay", isOverlayShowing());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if ((Boolean) param.getObjectExtra("overlay")) {
                    int i = (Integer) param.getObjectExtra("mode");
                    XposedHelpers.setIntField(param.thisObject, "mNavigationBarMode", i);
                }
            }
        });
    }

    private boolean shouldUseTabletRecents(){
        return TabletKatModule.shouldUseTabletRecents();
    }

    @Override
    public void initResources(XResources res, XModuleResources res2) {
    }

    public boolean isOverlayShowing() {
        return mRecentsPanel != null && mRecentsPanel.isVisible();
    }
}
