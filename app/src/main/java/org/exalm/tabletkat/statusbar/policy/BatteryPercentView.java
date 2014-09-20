package org.exalm.tabletkat.statusbar.policy;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.exalm.tabletkat.TabletKatModule;

import de.robv.android.xposed.XposedHelpers;

public class BatteryPercentView extends TextView {
    private static final String PERCENT_SYMBOL = "%";

    private View mBatteryMeterView;
    private boolean mShowOnFull;

    public BatteryPercentView(Context context) {
        this(context, null);
    }

    public BatteryPercentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mShowOnFull = false;
    }

    public void attach(View v){
        if (!TabletKatModule.mBatteryMeterViewClass.isInstance(v)){
            return;
        }
        mBatteryMeterView = v;
    }

    public void setShowOnFull(boolean b){
        mShowOnFull = b;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String str = "";
        if (mBatteryMeterView != null) {
            str = getPercents();
        }
        if (!str.equals(getText())){
            setText(str);
        }
        super.onDraw(canvas);
    }

    public String getPercents() {
        boolean demoMode = XposedHelpers.getBooleanField(mBatteryMeterView, "mDemoMode");
        Object tracker = XposedHelpers.getObjectField(mBatteryMeterView, demoMode ? "mDemoTracker" : "mTracker");
        int level = XposedHelpers.getIntField(tracker, "level");

        if (level > 100){
            level = 100;
            if (!mShowOnFull){
                return "";
            }
        }
        if (level < 0){
            return "";
        }

        return level + PERCENT_SYMBOL;
    }
}
