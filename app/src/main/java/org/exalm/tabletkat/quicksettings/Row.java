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

import org.exalm.tabletkat.TkR;

public abstract class Row {
    protected Context mContext;

    public Row(Context c){
        mContext = c;
    }

    protected abstract int getLabel();

    protected abstract int getIcon();

    protected boolean hasSwitch() {
        return false;
    }

    protected View.OnClickListener getOnClickListener(){
        return null;
    }

    protected int getLayout(){
        return TkR.layout.system_bar_settings_row;
    }

    protected void registerControllers(ImageView icon, TextView label, Switch checkbox, View custom){
    }

    public void releaseControllers(){
    }

    protected View getCustomView(){
        return null;
    }

    public View getView(){
        LinearLayout view = new LinearLayout(mContext);

        DisplayMetrics d = mContext.getResources().getDisplayMetrics();
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, d);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, d);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        LinearLayout row = (LinearLayout) View.inflate(mContext, TkR.layout.system_bar_settings_row, view);
        row = (LinearLayout) row.getChildAt(0);
        TextView label = (TextView) row.findViewById(TkR.id.row_label);
        ImageView icon = (ImageView) row.findViewById(TkR.id.row_icon);
        Switch checkbox = (Switch) row.findViewById(TkR.id.row_checkbox);
        row.setPaddingRelative(0, 0, padding, 0);

        if (getIcon() > 0) {
            icon.setImageResource(getIcon());
        } else {
            icon.setVisibility(View.GONE);
        }
        if (getLabel() > 0) {
            label.setText(getLabel());
        } else {
            label.setVisibility(View.GONE);
        }
        if (!hasSwitch()) {
            checkbox.setVisibility(View.GONE);
        }
        View.OnClickListener clickListener = getOnClickListener();
        if (clickListener != null) {
            row.setOnClickListener(clickListener);
        }
        View custom = getCustomView();
        if (custom != null){
            row.addView(custom);
        }
        registerControllers(icon, label, checkbox,  custom);

        return view;
    }

    public View getSeparator() {
        View v = new View(mContext);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
        return v;
    }

    protected Object getStatusBarManager() {
        return mContext.getSystemService("statusbar");
    }
}
