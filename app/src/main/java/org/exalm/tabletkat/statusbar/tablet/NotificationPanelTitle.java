/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;
import org.exalm.tabletkat.ViewHelper;
import org.exalm.tabletkat.statusbar.policy.BatteryPercentView;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;


public class NotificationPanelTitle extends RelativeLayout implements View.OnClickListener {
    private NotificationPanel mPanel;
    private ArrayList<View> buttons;
    private View mSettingsButton;
    private final Context mLargeIconContext;

    public NotificationPanelTitle(Context context, Context largeContext, AttributeSet attrs) {
        super(context, attrs);
        mLargeIconContext = largeContext;
        buttons = new ArrayList<View>();
        setOnClickListener(this);
    }

    public void setPanel(NotificationPanel p) {
        mPanel = p;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        buttons.add(mSettingsButton = findViewById(SystemR.id.settings_button));
        buttons.add(findViewById(SystemR.id.notification_button));

        setBackgroundResource(SystemR.drawable.system_bar_notification_header_bg);

        TextView clock = (TextView) XposedHelpers.newInstance(TabletKatModule.mClockClass, getContext(), null);
        clock.setSingleLine();
        ViewHelper.replaceView(this, SystemR.id.clock, clock);

        TextView date = (TextView) XposedHelpers.newInstance(TabletKatModule.mDateViewClass, getContext(), null);
        ViewHelper.replaceView(this, SystemR.id.date, date);
        ((TextView) findViewById(SystemR.id.date)).setAllCaps(true);

        ViewHelper.replaceView(this, SystemR.id.battery, (View) XposedHelpers.newInstance(TabletKatModule.mBatteryMeterViewClass, mLargeIconContext));

        ViewHelper.replaceView(this, TkR.id.battery_text, new BatteryPercentView(getContext()));
        BatteryPercentView v = (BatteryPercentView) findViewById(TkR.id.battery_text);
        v.attach(findViewById(SystemR.id.battery));
        v.setShowOnFull(true);

        ((TextView) findViewById(TkR.id.network_text)).setText(SystemR.string.status_bar_settings_settings_button);
        mSettingsButton.setContentDescription(getResources().getString(SystemR.string.accessibility_desc_quick_settings));
        findViewById(SystemR.id.notification_button).setContentDescription(getResources().getString(SystemR.string.accessibility_notifications_button));
        ((ImageView) findViewById(SystemR.id.clear_all_button)).setImageResource(SystemR.drawable.ic_notify_clear);
        findViewById(SystemR.id.clear_all_button).setContentDescription(getResources().getString(SystemR.string.accessibility_clear_all));

        getChildAt(1).setPadding(0, (int)getResources().getDimension(SystemR.dimen.notification_panel_header_padding_top), 0, 0);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        for (View button : buttons) {
            if (button != null) {
                button.setPressed(pressed);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!mSettingsButton.isEnabled())
            return false;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                break;
            case MotionEvent.ACTION_MOVE:
                final int x = (int) e.getX();
                final int y = (int) e.getY();
                setPressed(x > 0 && x < getWidth() && y > 0 && y < getHeight());
                break;
            case MotionEvent.ACTION_UP:
                if (isPressed()) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    mPanel.swapPanels();
                    setPressed(false);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (mSettingsButton.isEnabled() && v == this) {
            mPanel.swapPanels();
        }
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEvent(child, event)) {
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }
}
