package com.greymax.android.sve.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.greymax.android.sve.R;
import com.greymax.android.sve.filters.filter.GlBilateralFilter;
import com.greymax.android.sve.filters.filter.GlBitmapOverlayFilter;
import com.greymax.android.sve.filters.filter.GlBoxBlurFilter;
import com.greymax.android.sve.filters.filter.GlBulgeDistortionFilter;
import com.greymax.android.sve.filters.filter.GlCGAColorspaceFilter;
import com.greymax.android.sve.filters.filter.GlFilter;
import com.greymax.android.sve.filters.filter.GlFilterGroup;
import com.greymax.android.sve.filters.filter.GlGaussianBlurFilter;
import com.greymax.android.sve.filters.filter.GlGrayScaleFilter;
import com.greymax.android.sve.filters.filter.GlHazeFilter;
import com.greymax.android.sve.filters.filter.GlInvertFilter;
import com.greymax.android.sve.filters.filter.GlLookUpTableFilter;
import com.greymax.android.sve.filters.filter.GlMonochromeFilter;
import com.greymax.android.sve.filters.filter.GlSepiaFilter;
import com.greymax.android.sve.filters.filter.GlSharpenFilter;
import com.greymax.android.sve.filters.filter.GlSphereRefractionFilter;
import com.greymax.android.sve.filters.filter.GlToneCurveFilter;
import com.greymax.android.sve.filters.filter.GlVignetteFilter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thinkpad on 2018/5/9.
 */

public enum FilterType {
    DEFAULT(0),
    BILATERAL_BLUR(1),
    BOX_BLUR(2),
    TONE_CURVE_SAMPLE(3),
    LOOK_UP_TABLE_SAMPLE(4),
    BULGE_DISTORTION(5),
    CGA_COLORSPACE(6),
    GAUSSIAN_FILTER(7),
    GRAY_SCALE(8),
    HAZE(9),
    INVERT(10),
    MONOCHROME(11),
    SEPIA(12),
    SHARP(13),
    VIGNETTE(14),
    FILTER_GROUP_SAMPLE(15),
    SPHERE_REFRACTION(16),
    BITMAP_OVERLAY_SAMPLE(17);

    private int id;
    FilterType(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static List<FilterType> createFilterList() {
        List<FilterType> filters = new ArrayList<>();

        filters.add(DEFAULT);
        filters.add(SEPIA);
        filters.add(MONOCHROME);
        filters.add(TONE_CURVE_SAMPLE);
        filters.add(VIGNETTE);
        filters.add(INVERT);
        filters.add(HAZE);
        filters.add(LOOK_UP_TABLE_SAMPLE);
        filters.add(BITMAP_OVERLAY_SAMPLE);
        filters.add(GRAY_SCALE);
        filters.add(SPHERE_REFRACTION);
        filters.add(FILTER_GROUP_SAMPLE);
        filters.add(GAUSSIAN_FILTER);
        filters.add(BULGE_DISTORTION);
        filters.add(CGA_COLORSPACE);
        filters.add(SHARP);

        return filters;
    }

    public static GlFilter createGlFilter(FilterType filterType, Context context) {
        switch (filterType) {
            case DEFAULT:
                return new GlFilter();
            case SEPIA:
                return new GlSepiaFilter();
            case GRAY_SCALE:
                return new GlGrayScaleFilter();
            case INVERT:
                return new GlInvertFilter();
            case HAZE:
                return new GlHazeFilter();
            case MONOCHROME:
                return new GlMonochromeFilter();
            case BILATERAL_BLUR:
                return new GlBilateralFilter();
            case BOX_BLUR:
                return new GlBoxBlurFilter();
            case TONE_CURVE_SAMPLE:
                try {
                    InputStream is = context.getAssets().open("acv/tone_cuver_sample.acv");
                    return new GlToneCurveFilter(is);
                } catch (IOException e) {
                    Log.e("FilterType", "Error");
                }
                return new GlFilter();
            case BITMAP_OVERLAY_SAMPLE:
                return new GlBitmapOverlayFilter(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_round));
            case LOOK_UP_TABLE_SAMPLE:
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.lookup_sample);
                return new GlLookUpTableFilter(bitmap);
            case SPHERE_REFRACTION:
                return new GlSphereRefractionFilter();
            case VIGNETTE:
                return new GlVignetteFilter();
            case FILTER_GROUP_SAMPLE:
                return new GlFilterGroup(new GlSepiaFilter(), new GlVignetteFilter());
            case GAUSSIAN_FILTER:
                return new GlGaussianBlurFilter();
            case BULGE_DISTORTION:
                return new GlBulgeDistortionFilter();
            case CGA_COLORSPACE:
                return new GlCGAColorspaceFilter();
            case SHARP:
                GlSharpenFilter glSharpenFilter = new GlSharpenFilter();
                glSharpenFilter.setSharpness(4f);
                return glSharpenFilter;
            default:
                return new GlFilter();
        }
    }

    public static FilterType fromInteger(int x) {
        switch(x) {
            case 0:
                return DEFAULT;
            case 1:
                return BILATERAL_BLUR;
            case 2:
                return BOX_BLUR;
            case 3:
                return TONE_CURVE_SAMPLE;
            case 4:
                return LOOK_UP_TABLE_SAMPLE;
            case 5:
                return BULGE_DISTORTION;
            case 6:
                return CGA_COLORSPACE;
            case 7:
                return GAUSSIAN_FILTER;
            case 8:
                return GRAY_SCALE;
            case 9:
                return HAZE;
            case 10:
                return INVERT;
            case 11:
                return MONOCHROME;
            case 12:
                return SEPIA;
            case 13:
                return SHARP;
            case 14:
                return VIGNETTE;
            case 15:
                return FILTER_GROUP_SAMPLE;
            case 16:
                return SPHERE_REFRACTION;
            case 17:
                return BITMAP_OVERLAY_SAMPLE;
        }
        return null;
    }
}
