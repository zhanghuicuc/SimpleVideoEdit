package com.greymax.android.sve.filters.filter;

import android.opengl.GLES20;

import java.util.ArrayList;

/**
 * Created by thinkpad on 2018/4/18.
 */

public class GlColorMatrixFilter extends GlFilter {
    public static final String COLOR_MATRIX_FRAGMENT_SHADER = "" +
            "varying highp vec2 vTextureCoord;\n" +
            "\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "uniform lowp mat4 colorMatrix;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n" +
            "    lowp vec4 outputColor = textureColor * colorMatrix;\n" +
            "    \n" +
            "    gl_FragColor = outputColor;\n" +
            "}";

    private int mIntensity;
    private float[] mColorMatrix;
    private int mColorMatrixLocation;
    private int mIntensityLocation;

    private ArrayList<float[]> mColorMatrixList = new ArrayList<>();

    public GlColorMatrixFilter() {
        this(1, new float[] {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public GlColorMatrixFilter(final int intensity) {
        super(DEFAULT_VERTEX_SHADER, COLOR_MATRIX_FRAGMENT_SHADER);
        mColorMatrixList.add(new float[] {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
        mColorMatrixList.add(new float[] {
                0.3588f, 0.7044f, 0.1368f, 0.0f,
                0.2990f, 0.5870f, 0.1140f, 0.0f,
                0.2392f, 0.4696f, 0.0912f, 0.0f,
                0f, 0f, 0f, 1.0f
        });
        mIntensity = intensity;
        setIntensity(mIntensity);
    }

    public GlColorMatrixFilter(final int intensity, final float[] colorMatrix) {
        super(DEFAULT_VERTEX_SHADER, COLOR_MATRIX_FRAGMENT_SHADER);
        mColorMatrixList.add(new float[] {
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        });
        mColorMatrixList.add(new float[] {
                0.3588f, 0.7044f, 0.1368f, 0.0f,
                0.2990f, 0.5870f, 0.1140f, 0.0f,
                0.2392f, 0.4696f, 0.0912f, 0.0f,
                0f, 0f, 0f, 1.0f
        });
        mIntensity = intensity;
        setIntensity(mIntensity);
    }

    public void setIntensity(final int intValue) {
        mIntensity = intValue;
        if (mIntensity < 5) {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    GLES20.glUniformMatrix4fv(getHandle("colorMatrix"), 1, false, mColorMatrixList.get(0), 0);
                }
            });
        } else {
            runOnDraw(new Runnable() {
                @Override
                public void run() {
                    GLES20.glUniformMatrix4fv(getHandle("colorMatrix"), 1, false, mColorMatrixList.get(1), 0);
                }
            });
        }
    }
}
