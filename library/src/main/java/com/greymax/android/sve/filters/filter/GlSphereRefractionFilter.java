package com.greymax.android.sve.filters.filter;

import android.opengl.GLES20;

/**
 * Created by sudamasayuki on 2017/05/18.
 */

public class GlSphereRefractionFilter extends GlFilter {

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +

                    "varying vec2 vTextureCoord;" +
                    "uniform lowp sampler2D sTexture;" +
                    "uniform highp vec2 center;" +
                    "uniform highp float radius;" +
                    "uniform highp float aspectRatio;" +
                    "uniform highp float refractiveIndex;" +

                    "void main() {" +
                    "//归一化坐标空间需要考虑屏幕是一个单位宽和一个单位长。\n" +
                    "highp vec2 textureCoordinateToUse = vec2(vTextureCoord.x, (vTextureCoord.y * aspectRatio + 0.5 - 0.5 * aspectRatio));" +
                    "//计算特定像素点距离球形的中心有多远。使用GLSL内建的distance()函数，用勾股定律计算出中心坐标和长宽比矫正过的纹理坐标的距离\n" +
                    "highp float distanceFromCenter = distance(center, textureCoordinateToUse);" +
                    "//计算片段是否在球体内 \n" +
                    "lowp float checkForPresenceWithinSphere = step(distanceFromCenter, radius);" +
                    "//标准化到球心的距离，重新设置distanceFromCenter\n" +
                    "distanceFromCenter = distanceFromCenter / radius;" +
                    "//模拟一个玻璃球，需要计算球的“深度”是多少\n" +
                    "highp float normalizedDepth = radius * sqrt(1.0 - distanceFromCenter * distanceFromCenter);" +
                    "//归一化 \n" +
                    "highp vec3 sphereNormal = normalize(vec3(textureCoordinateToUse - center, normalizedDepth));" +
                    "//GLSL的refract()函数以刚才创建的球法线和折射率来计算当光线通过球时从任意一个点看起来如何。\n" +
                    "highp vec3 refractedVector = refract(vec3(0.0, 0.0, -1.0), sphereNormal, refractiveIndex);" +
                    "//最后凑齐所有计算需要的颜色信息。\n" +
                    "gl_FragColor = texture2D(sTexture, (refractedVector.xy + 1.0) * 0.5) * checkForPresenceWithinSphere;" +
                    "}";

    private float centerX = 0.5f;
    private float centerY = 0.5f;
    private float radius = 0.5f;
    private float aspectRatio = 1.0f;
    private float refractiveIndex = 0.71f;

    public GlSphereRefractionFilter() {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    public void setCenterX(float centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(float centerY) {
        this.centerY = centerY;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public void setRefractiveIndex(float refractiveIndex) {
        this.refractiveIndex = refractiveIndex;
    }

    //////////////////////////////////////////////////////////////////////////

    @Override
    public void onDraw() {
        GLES20.glUniform2f(getHandle("center"), centerX, centerY);
        GLES20.glUniform1f(getHandle("radius"), radius);
        GLES20.glUniform1f(getHandle("aspectRatio"), aspectRatio);
        GLES20.glUniform1f(getHandle("refractiveIndex"), refractiveIndex);
    }

    public String getFragmentShaderSource() {
        return FRAGMENT_SHADER;
    }
}
