package org.exalm.tabletkat;

import android.content.res.XResources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public abstract class CustomDimenReplacement extends XResources.DimensionReplacement {
    private final int mUnit;

    public CustomDimenReplacement() {
        super(0, TypedValue.COMPLEX_UNIT_PX);
        mUnit = TypedValue.COMPLEX_UNIT_PX;
    }

    protected abstract float getValue();

    @Override
    public float getDimension(DisplayMetrics metrics) {
        return TypedValue.applyDimension(mUnit, getValue(), metrics);
    }

    @Override
    public int getDimensionPixelOffset(DisplayMetrics metrics) {
        return (int) TypedValue.applyDimension(mUnit, getValue(), metrics);
    }

    @Override
    public int getDimensionPixelSize(DisplayMetrics metrics) {
        final float f = TypedValue.applyDimension(mUnit, getValue(), metrics);
        final int res = (int) (f + 0.5f);
        if (res != 0) return res;
        if (getValue() == 0) return 0;
        if (getValue() > 0) return 1;
        return -1;
    }
}
