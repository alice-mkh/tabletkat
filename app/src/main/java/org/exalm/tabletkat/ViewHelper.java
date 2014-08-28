package org.exalm.tabletkat;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ViewHelper {
    public static View replaceView(View v1, int id, View v2){
        return replaceView(v1.findViewById(id), v2);
    }

    public static View replaceView(View v1, View v2){
        ViewGroup parent = (ViewGroup) v1.getParent();
        int index = -1;
        if (parent != null) {
            int n = parent.getChildCount();
            for (int i = 0; i < n; i++) {
                if (parent.getChildAt(i) == v1) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                XposedBridge.log("Cannot replace view: " + v1.getClass() + " with " + v2.getClass());
                return v1;
            }
        }
        View[] children = null;
        if (v1 instanceof ViewGroup){
            int c = ((ViewGroup) v1).getChildCount();
            children = new View[c];
            for (int i = 0; i < c; i++) {
                children[i] = ((ViewGroup) v1).getChildAt(i);
            }
        }
        if (parent != null) {
            parent.removeViewAt(index);
            parent.addView(v2, index);
        }
        if (v1 instanceof ViewGroup && v2 instanceof ViewGroup){
            ((ViewGroup) v1).removeAllViews();
            for (View v : children) {
                ((ViewGroup) v2).addView(v);
            }
        }
        v2.setId(v1.getId());
        if (v1.getLayoutParams() != null) {
            v2.setLayoutParams(v1.getLayoutParams());
        }
        v2.setVisibility(v1.getVisibility());
        v2.setClickable(v1.isClickable());
        v2.setBackground(v1.getBackground());
        v2.setPaddingRelative(v1.getPaddingStart(), v1.getPaddingTop(), v1.getPaddingEnd(), v1.getPaddingBottom());
        v2.setAlpha(v1.getAlpha());
        if (v1 instanceof LinearLayout && v2 instanceof LinearLayout){
            ((LinearLayout) v2).setOrientation(((LinearLayout) v1).getOrientation());
        }
        if (v1 instanceof ImageView && v2 instanceof ImageView){
            ((ImageView) v2).setImageDrawable(((ImageView) v1).getDrawable());
        }
        if (v1 instanceof TextView && v2 instanceof TextView){
            ((TextView) v2).setTextColor(((TextView) v1).getCurrentTextColor());
            ((TextView) v2).setTextSize(TypedValue.COMPLEX_UNIT_PX, ((TextView)v1).getTextSize());
            ((TextView) v2).setTypeface(((TextView) v1).getTypeface());
            ((TextView) v2).setSingleLine(XposedHelpers.getBooleanField(v1, "mSingleLine"));
            ((TextView) v2).setGravity(((TextView) v1).getGravity());
        }
        return v2;
    }
}
