package org.exalm.tabletkat.recent;

import android.animation.Animator;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.tablet.TabletStatusBarMod;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class RecentsPanel {
    public static final boolean DEBUG = false;
    public static final String TAG = "RecentsPanel";

    protected static final int MSG_CLOSE_RECENTS_PANEL = 1021;

    protected FrameLayout mRecentsPanel;
    protected Choreographer mChoreo;
    protected Object mRecentTasksLoader;
    protected Context mContext;
    protected WindowManager mWindowManager;
    protected Object mBar;
    protected TabletStatusBarMod mMod;
    boolean useTabletLayout;
    private boolean mVisible;

    public RecentsPanel(Object bar, TabletStatusBarMod mod, boolean useTabletLayout) {
        mBar = bar;
        mMod = mod;
        mContext = (Context) XposedHelpers.getObjectField(mBar, "mContext");
        mWindowManager = (WindowManager) XposedHelpers.getObjectField(mBar, "mWindowManager");
        mRecentTasksLoader = XposedHelpers.callStaticMethod(TabletKatModule.mRecentTasksLoaderClass, "getInstance", mContext);
        this.useTabletLayout = useTabletLayout;
        updateRecentsPanel();
    }

    public void destroy() {
        mWindowManager.removeView(mRecentsPanel);
    }

    protected WindowManager.LayoutParams getRecentsLayoutParams(
            ViewGroup.LayoutParams layoutParams) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                useTabletLayout ? (int) mContext.getResources().getDimension(TkR.dimen.system_bar_recents_width) :
                        layoutParams.width,
                useTabletLayout ? WindowManager.LayoutParams.MATCH_PARENT : layoutParams.height,

                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                PixelFormat.TRANSLUCENT);

        if (useTabletLayout || ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        } else {
            lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            lp.dimAmount = 0.75f;
        }

        lp.gravity = Gravity.BOTTOM | Gravity.START;
        lp.setTitle("RecentsPanel");
        lp.windowAnimations = com.android.internal.R.style.Animation_RecentApplications;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;

        return lp;
    }

    protected void updateRecentsPanel(int recentsResId) {
        // Recents Panel
        boolean visible = false;
        ArrayList recentTasksList = null;
        boolean firstScreenful = false;
        if (mRecentsPanel != null) {
            visible = mRecentsPanel.getVisibility() == View.VISIBLE;
            mWindowManager.removeView(mRecentsPanel);
            recentTasksList = (ArrayList) XposedHelpers.getObjectField(mRecentsPanel, "mRecentTaskDescriptions");
            firstScreenful = XposedHelpers.getBooleanField(mRecentTasksLoader, "mFirstScreenful");
        }

        // Provide RecentsPanelView with a temporary parent to allow layout params to work.
        LinearLayout tmpRoot = new LinearLayout(mContext);
        mRecentsPanel = (FrameLayout) LayoutInflater.from(mContext).inflate(
                recentsResId, tmpRoot, false);
        createChoreo();
        XposedHelpers.setObjectField(mRecentsPanel, "mRecentTasksLoader", mRecentTasksLoader);
        XposedHelpers.callMethod(mRecentTasksLoader, "cancelLoadingThumbnailsAndIcons");
        XposedHelpers.callMethod(mRecentTasksLoader, "setRecentsPanel", mRecentsPanel, mRecentsPanel);
        mRecentsPanel.setOnTouchListener(
                new TouchOutsideListener(MSG_CLOSE_RECENTS_PANEL, mRecentsPanel));
        if (!visible) {
            mRecentsPanel.setVisibility(View.GONE);
        }

        WindowManager.LayoutParams lp = getRecentsLayoutParams(mRecentsPanel.getLayoutParams());

        mWindowManager.addView(mRecentsPanel, lp);
        if (visible) {
            XposedHelpers.callMethod(mRecentsPanel, "show", true, recentTasksList, firstScreenful, false);
        } else {
            XposedHelpers.callMethod(mRecentsPanel, "refreshRecentTasksList", recentTasksList, firstScreenful);
        }

    }

    public void updateRecentsPanel() {
        if (useTabletLayout) {
            updateRecentsPanel(TkR.layout.system_bar_recent_panel);
            return;
        }

        updateRecentsPanel(SystemR.layout.status_bar_recent_panel);
        // Make .03 alpha the minimum so you always see the item a bit-- slightly below
        // .03, the item disappears entirely (as if alpha = 0) and that discontinuity looks
        // a bit jarring
        XposedHelpers.callMethod(mRecentsPanel, "setMinSwipeAlpha", 0.03f);
    }

    public void toggleRecentsActivity() {
        if (mRecentsPanel != null) {
            boolean b = mRecentsPanel.getVisibility() == View.VISIBLE;
            //TODO: Somehow handle the situation when the task list is null
            XposedHelpers.callMethod(mRecentsPanel, "show", !b, null, true, true);
        }
    }

    public void preloadRecentTasksList() {
        XposedHelpers.callMethod(mRecentTasksLoader, "preloadRecentTasksList");
    }

    public void cancelPreloadingRecentTasksList() {
        XposedHelpers.callMethod(mRecentTasksLoader, "cancelPreloadingRecentTasksList");
    }

    public void closeRecents() {
        if (mRecentsPanel != null) {
            XposedHelpers.callMethod(mRecentsPanel, "show", false);
        }
    }

    public void setVisibility(boolean show, boolean animate) {
        if (animate) {
            if (mVisible != show) {
                mVisible = show;
                if (show) {
                    mRecentsPanel.setVisibility(View.VISIBLE);
                }
                mChoreo.startAnimation(show);
            }
        } else {
            mVisible = show;
            mRecentsPanel.setVisibility(show ? View.VISIBLE : View.GONE);
            mChoreo.jumpTo(show);
            XposedHelpers.callMethod(mRecentsPanel, "onAnimationEnd",
                    new Class[]{Animator.class}, new Object[]{null});
        }
        if (mMod != null) {
            mMod.setOverlayRecentsVisible(mVisible);
        }
        if (TabletKatModule.mPhoneStatusBarClass.isInstance(mBar)) {
            XposedHelpers.callMethod(mBar, "checkBarModes");
        }
    }

    public boolean isVisible() {
        return mVisible;
    }

    public void onLayout() {
        View v = (View) XposedHelpers.getObjectField(mRecentsPanel, "mRecentsContainer");
        mChoreo.setPanelHeight(v.getHeight());
    }

    public void createChoreo() {
        View mRecentsScrim = (View) XposedHelpers.getObjectField(mRecentsPanel, "mRecentsScrim");
        View mRecentsContainer = (View) XposedHelpers.getObjectField(mRecentsPanel, "mRecentsContainer");
        View mRecentsNoApps = (View) XposedHelpers.getObjectField(mRecentsPanel, "mRecentsNoApps");
        mChoreo = new Choreographer(mRecentsPanel, mRecentsScrim, mRecentsContainer, mRecentsNoApps,
                (Animator.AnimatorListener) mRecentsPanel, useTabletLayout);
    }

    public class TouchOutsideListener implements View.OnTouchListener {
        private int mMsg;
        private View mPanel;

        public TouchOutsideListener(int msg, View panel) {
            mMsg = msg;
            mPanel = panel;
        }

        public boolean onTouch(View v, MotionEvent ev) {
            final int action = ev.getAction();
            if (action == MotionEvent.ACTION_OUTSIDE
                    || (action == MotionEvent.ACTION_DOWN
                    && !(Boolean) XposedHelpers.callMethod(mPanel, "isInContentArea", (int)ev.getX(), (int)ev.getY()))) {
                Handler mHandler = (Handler) XposedHelpers.getObjectField(mBar, "mHandler");
                mHandler.removeMessages(mMsg);
                mHandler.sendEmptyMessage(mMsg);
                return true;
            }
            return false;
        }
    }
}
