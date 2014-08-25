package org.exalm.tabletkat;

import android.content.res.XModuleResources;
import android.content.res.XResources;

public interface IMod {
    public void addHooks(ClassLoader cl);

    public void initResources(XResources res, XModuleResources res2);
}