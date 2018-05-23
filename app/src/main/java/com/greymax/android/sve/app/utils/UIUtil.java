package com.greymax.android.sve.app.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import static android.content.Context.WINDOW_SERVICE;


public class UIUtil {
    public static int dip2px(Context context, int dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5F);
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        return metric.heightPixels;
    }
}
