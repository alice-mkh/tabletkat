/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.statusbar.phone.BarTransitions;

import de.robv.android.xposed.XposedHelpers;

public final class TabletStatusBarTransitions extends BarTransitions {
    private static final float ICON_ALPHA_WHEN_NOT_OPAQUE = 1;

    private static final float KEYGUARD_QUIESCENT_ALPHA = 0.5f;
    private static final int CONTENT_FADE_DURATION = 200;

    private final TabletStatusBarView mView;
    private final Object mBarService;

    private boolean mLightsOut;
    private final float mIconAlphaWhenOpaque;

    private View mFeedbackIconArea, mStatusIcons, mSignalCluster, mBattery, mBatteryText, mBluetooth, mClock;
    private Animator mCurrentAnimation;

    public TabletStatusBarTransitions(TabletStatusBarView view, Object barService) {
        super(view, SystemR.drawable.nav_background);
        mView = view;
        mBarService = barService;
        final Resources res = mView.getContext().getResources();
        mIconAlphaWhenOpaque = res.getFraction(SystemR.dimen.status_bar_icon_drawing_alpha, 1, 1);
    }

    public void init() {
        mFeedbackIconArea = mView.findViewById(TkR.id.feedbackIconArea);
        mStatusIcons = mView.findViewById(SystemR.id.statusIcons);
        mSignalCluster = mView.findViewById(SystemR.id.signal_cluster);
        mBattery = mView.findViewById(SystemR.id.battery);
        mBatteryText = mView.findViewById(TkR.id.battery_text);
        mBluetooth = mView.findViewById(TkR.id.bluetooth);
        mClock = mView.findViewById(SystemR.id.clock);
        applyModeBackground(-1, getMode(), false /*animate*/);
        applyMode(getMode(), false /*animate*/, true /*force*/);
    }

    public ObjectAnimator animateTransitionTo(View v, float toAlpha) {
        return ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(), toAlpha);
    }

    private float getIconAlphaFor(int mode) {
        return !isOpaque(mode) ? ICON_ALPHA_WHEN_NOT_OPAQUE : mIconAlphaWhenOpaque;
    }

    private boolean isOpaque(int mode) {
        return !(mode == MODE_SEMI_TRANSPARENT || mode == MODE_TRANSLUCENT);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate, false /*force*/);
    }

    private void applyMode(int mode, boolean animate, boolean force) {
        // apply to key buttons
        final float alpha = alphaForMode(mode);
        setKeyButtonViewQuiescentAlpha(mView.getBackButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(mView.getHomeButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(mView.getRecentsButton(), alpha, animate);
        setKeyButtonViewQuiescentAlpha(mView.getMenuButton(), alpha, animate);

        setKeyButtonViewQuiescentAlpha(mView.getSearchLight(), KEYGUARD_QUIESCENT_ALPHA, animate);
//        setKeyButtonViewQuiescentAlpha(mView.getCameraButton(), KEYGUARD_QUIESCENT_ALPHA, animate);

        // apply to lights out
        applyLightsOut(mode == MODE_LIGHTS_OUT, animate, force);

        if (mFeedbackIconArea == null) return; // pre-init
        float newAlpha = getIconAlphaFor(mode);
        if (mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }
        if (animate) {
            AnimatorSet anims = new AnimatorSet();
            anims.playTogether(
                    animateTransitionTo(mFeedbackIconArea, newAlpha),
                    animateTransitionTo(mStatusIcons, newAlpha),
                    animateTransitionTo(mSignalCluster, newAlpha),
                    animateTransitionTo(mBattery, newAlpha),
                    animateTransitionTo(mBatteryText, newAlpha),
                    animateTransitionTo(mBluetooth, newAlpha),
                    animateTransitionTo(mClock, newAlpha)
            );
            if (mode == MODE_LIGHTS_OUT) {
                anims.setDuration(LIGHTS_OUT_DURATION);
            }
            anims.start();
            mCurrentAnimation = anims;
        } else {
            mFeedbackIconArea.setAlpha(newAlpha);
            mStatusIcons.setAlpha(newAlpha);
            mSignalCluster.setAlpha(newAlpha);
            mBattery.setAlpha(newAlpha);
            mBatteryText.setAlpha(newAlpha);
            mBluetooth.setAlpha(newAlpha);
            mClock.setAlpha(newAlpha);
        }
    }

    private float alphaForMode(int mode) {
        final boolean isOpaque = mode == MODE_OPAQUE || mode == MODE_LIGHTS_OUT;
        return isOpaque ? 0.7f : 1f;
    }

    public void applyBackButtonQuiescentAlpha(int mode, boolean animate) {
        float backAlpha = 0;
        backAlpha = maxVisibleQuiescentAlpha(backAlpha, mView.getSearchLight());
//        backAlpha = maxVisibleQuiescentAlpha(backAlpha, mView.getCameraButton());
        backAlpha = maxVisibleQuiescentAlpha(backAlpha, mView.getHomeButton());
        backAlpha = maxVisibleQuiescentAlpha(backAlpha, mView.getRecentsButton());
        backAlpha = maxVisibleQuiescentAlpha(backAlpha, mView.getMenuButton());
        if (backAlpha > 0) {
            setKeyButtonViewQuiescentAlpha(mView.getBackButton(), backAlpha, animate);
        }
    }

    private static float maxVisibleQuiescentAlpha(float max, View v) {
        if (v.isShown()) {
            try {
                return Math.max(max, (Float) XposedHelpers.callMethod(v, "getQuiescentAlpha"));
            } catch (ClassCastException e) {
            }
        }
        return max;
    }

    @Override
    public void setContentVisible(boolean visible) {
        final float alpha = visible ? 1 : 0;
//        fadeContent(mView.getCameraButton(), alpha);
        fadeContent(mView.getSearchLight(), alpha);
    }

    private void fadeContent(View v, float alpha) {
        if (v != null) {
            v.animate().alpha(alpha).setDuration(CONTENT_FADE_DURATION);
        }
    }

    private void setKeyButtonViewQuiescentAlpha(View button, float alpha, boolean animate) {
        try {
            XposedHelpers.callMethod(button, "setQuiescentAlpha", alpha, animate);
        }catch (ClassCastException e){}
    }

    private void applyLightsOut(boolean lightsOut, boolean animate, boolean force) {
        if (!force && lightsOut == mLightsOut) return;

        mLightsOut = lightsOut;

        final View statusBar = mView.findViewById(TkR.id.bar_contents);
        final View lowLights = mView.findViewById(TkR.id.bar_shadow);

        // ok, everyone, stop it right there
        statusBar.animate().cancel();
        lowLights.animate().cancel();

        final float navButtonsAlpha = lightsOut ? 0f : 1f;
        final float lowLightsAlpha = lightsOut ? 1f : 0f;

        if (!animate) {
            statusBar.setAlpha(navButtonsAlpha);
            lowLights.setAlpha(lowLightsAlpha);
            lowLights.setVisibility(lightsOut ? View.VISIBLE : View.GONE);
        } else {
            final int duration = lightsOut ? LIGHTS_OUT_DURATION : LIGHTS_IN_DURATION;
            statusBar.animate()
                    .alpha(navButtonsAlpha)
                    .setDuration(duration)
                    .start();

            lowLights.setOnTouchListener(mLightsOutListener);
            if (lowLights.getVisibility() == View.GONE) {
                lowLights.setAlpha(0f);
                lowLights.setVisibility(View.VISIBLE);
            }
            lowLights.animate()
                    .alpha(lowLightsAlpha)
                    .setDuration(duration)
                    .setInterpolator(new AccelerateInterpolator(2.0f))
                    .setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator _a) {
                            lowLights.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private final View.OnTouchListener mLightsOutListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                // even though setting the systemUI visibility below will turn these views
                // on, we need them to come up faster so that they can catch this motion
                // event
                applyLightsOut(false, false, false);

                try {
                    XposedHelpers.callMethod(mBarService, "setSystemUiVisibility", 0, View.SYSTEM_UI_FLAG_LOW_PROFILE);
                } catch (Exception ex) {
                }
            }
            return false;
        }
    };
}
