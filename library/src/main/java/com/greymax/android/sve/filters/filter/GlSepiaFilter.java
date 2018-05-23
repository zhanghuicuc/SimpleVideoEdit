package com.greymax.android.sve.filters.filter;

/**
 * Created by sudamasayuki on 2017/05/16.
 */

public class GlSepiaFilter extends GlFilter{

    private static final String FRAGMENT_SHADE_SEPIA =
            "precision mediump float;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform lowp sampler2D sTexture;" +

                    "uniform lowp int intensity;" +

                    "void main() {" +

                    "   vec4 FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "   // 点乘运算\n" +
                    "   if (intensity < 5) { \n" +
                    "   gl_FragColor.r = dot(FragColor.rgb, vec3(.393, .769, .189));\n" +
                    "   gl_FragColor.g = dot(FragColor.rgb, vec3(.349, .686, .168));\n" +
                    "   gl_FragColor.b = dot(FragColor.rgb, vec3(.272, .534, .131));\n" +
                    "   } else { \n" +
                    "   gl_FragColor.r = dot(FragColor.rgb, vec3(1.4027, -0.8266, 0.4240));\n" +
                    "   gl_FragColor.g = dot(FragColor.rgb, vec3(0.1740, 0.6429, 0.1832));\n" +
                    "   gl_FragColor.b = dot(FragColor.rgb, vec3(-0.2818, 0.5784, 0.7032));\n" +
                    "   }\n" +
                    "}";

    public GlSepiaFilter() {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADE_SEPIA);
    }
}