package org.exalm.tabletkat.quicksettings;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.exalm.tabletkat.SystemR;
import org.exalm.tabletkat.TabletKatModule;
import org.exalm.tabletkat.TkR;

import de.robv.android.xposed.XposedHelpers;

public class RowBrightness extends Row {
    private Object mBrightness;

    public RowBrightness(Context c) {
        super(c);
    }

    @Override
    protected int getLabel() {
        return 0;
    }

    @Override
    protected int getIcon() {
        return TkR.drawable.ic_sysbar_brightness;
    }

    @Override
    protected View getCustomView(){
        DisplayMetrics d = mContext.getResources().getDisplayMetrics();
        View slider = (View) XposedHelpers.newInstance(TabletKatModule.mToggleSliderClass, mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.FILL_PARENT);
        lp.weight = 1;
        lp.setMarginEnd((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, d));
        slider.setLayoutParams(lp);
        ((TextView) XposedHelpers.getObjectField(slider, "mLabel")).setText(SystemR.string.status_bar_settings_auto_brightness_label);
        return slider;
    }

    @Override
    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom) {
        mBrightness = XposedHelpers.newInstance(TabletKatModule.mBrightnessControllerClass, mContext, icon, custom);
    }
}
