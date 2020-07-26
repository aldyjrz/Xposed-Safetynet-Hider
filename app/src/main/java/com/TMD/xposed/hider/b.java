package com.TMD.xposed.hider;

import android.content.ContentResolver;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class b extends XC_MethodHook{
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (param != null) {
            if (param.args[1].equals("install_non_market_apps")) {
                param.setResult(0);
            }
        }
    }

}
