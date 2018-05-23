package com.greymax.android.sve.filters.filter;

import android.opengl.GLES20;

/**
 * Created by sudamasayuki on 2017/05/18.
 */

public class GlMonochromeFilter extends GlFilter {

    private static final String FRAGMENT_SHADER1 =
            "precision lowp float;" +

                    "varying highp vec2 vTextureCoord;" +
                    "uniform lowp sampler2D sTexture;" +
                    "uniform float intensity;" +
                    "uniform vec3 filterColor;" +

                    "const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);" +

                    "void main() {" +

                    "lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);" +
                    "float luminance = dot(textureColor.rgb, luminanceWeighting);" +

                    "lowp vec4 desat = vec4(vec3(luminance), 1.0);" +

                    "lowp vec4 outputColor = vec4(" +
                    "(desat.r < 0.5 ? (2.0 * desat.r * filterColor.r) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - filterColor.r)))," +
                    "(desat.g < 0.5 ? (2.0 * desat.g * filterColor.g) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - filterColor.g)))," +
                    "(desat.b < 0.5 ? (2.0 * desat.b * filterColor.b) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - filterColor.b)))," +
                    "1.0" +
                    ");" +

                    "gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);" +
                    "}";

    private static final String FRAGMENT_SHADER_GrenBlindOrifinal =
            "precision mediump float;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform lowp sampler2D sTexture;" +
                    "const highp vec3 weight = vec3(0.2125, 0.7154, 0.0721);" +

                    "void main() {" +

                    "   vec4 FragColor = texture2D(sTexture, vTextureCoord);\n" +

                    "//convert to xyz\n" +
                    "    vec3 tmp;\n" +
                    "    tmp.x = ( FragColor.r > 0.04045 ) ? pow( ( FragColor.r + 0.055 ) / 1.055, 2.4 ) : FragColor.r / 12.92;\n" +
                    "    tmp.y = ( FragColor.g > 0.04045 ) ? pow( ( FragColor.g + 0.055 ) / 1.055, 2.4 ) : FragColor.g / 12.92,\n" +
                    "    tmp.z = ( FragColor.b > 0.04045 ) ? pow( ( FragColor.b + 0.055 ) / 1.055, 2.4 ) : FragColor.b / 12.92;\n" +
                    "    mat3 matter = mat3(\n" +
                    "\t\t0.4124, 0.3576, 0.1805,\n" +
                    "        0.2126, 0.7152, 0.0722,\n" +
                    "        0.0193, 0.1192, 0.9505 \n" +
                    "\t);\n" +
                    "    vec3 xyz = 100.0 * tmp * matter;\n" +

                    "//convert to cielab\n" +
                    "vec3 n = xyz / vec3(95.047, 100, 108.883);\n" +
                    "    vec3 v;\n" +
                    "    v.x = ( n.x > 0.008856 ) ? pow( n.x, 1.0 / 3.0 ) : ( 7.787 * n.x ) + ( 16.0 / 116.0 );\n" +
                    "    v.y = ( n.y > 0.008856 ) ? pow( n.y, 1.0 / 3.0 ) : ( 7.787 * n.y ) + ( 16.0 / 116.0 );\n" +
                    "    v.z = ( n.z > 0.008856 ) ? pow( n.z, 1.0 / 3.0 ) : ( 7.787 * n.z ) + ( 16.0 / 116.0 );\n" +
                    "    vec3 cieLab = vec3(( 116.0 * v.y ) - 16.0, 500.0 * ( v.x - v.y ), 200.0 * ( v.y - v.z ));" +

                    "//multiply to get err xyz\n" +
                    "    mat3 XYZ2BRAD = mat3(\n" +
                    "\t\t0.8951, 0.2664, -0.1614,\n" +
                    "        -0.7502, 1.7135, 0.0367,\n" +
                    "        0.0389, -0.0685, 1.0296 \n" +
                    "\t);\n" +
                    "    mat3 BRAD2XYZ = mat3(\n" +
                    "\t\t0.9870, -0.1471, 0.1600,\n" +
                    "       0.4323, 0.5184, 0.0493,\n" +
                    "        -0.0085, 0.0400, 0.9685 \n" +
                    "\t);\n" +
                    "    mat3 D652E = mat3(\n" +
                    "\t\t1.0521, 0, 0,\n" +
                    "        0, 1, 0,\n" +
                    "        0, 0, 0.9184 \n" +
                    "\t);\n" +
                    "    mat3 E2D65 = mat3(\n" +
                    "\t\t0.9505, 0, 0,\n" +
                    "        0, 1, 0,\n" +
                    "        0, 0, 1.0888 \n" +
                    "\t);\n" +
                    "    mat3 simulate = mat3(\n" +
                    "\t\t1, 0, 0,\n" +
                    "    0.7, 0, 0.3,\n" +
                    "    0, 0, 1 \n" +
                    "\t);\n" +
                    "    mat3 errm = mat3(\n" +
                    "\t\t0.7958, 0.2163, -0.0204,\n" +
                    "        0.7572, 0.1978, 0.0758,\n" +
                    "        0.0637, -0.0675, 1.0064 \n" +
                    "\t);\n" +
                    "    vec3 xyz2 = xyz * errm;/*(E2D65 * BRAD2XYZ * simulate * XYZ2BRAD * D652E)*/\n" +

                    "//convert xyz2 to cielab\n" +
                    "n = xyz2 / vec3(95.047, 100, 108.883);\n" +
                    "    v.x = ( n.x > 0.008856 ) ? pow( n.x, 1.0 / 3.0 ) : ( 7.787 * n.x ) + ( 16.0 / 116.0 );\n" +
                    "    v.y = ( n.y > 0.008856 ) ? pow( n.y, 1.0 / 3.0 ) : ( 7.787 * n.y ) + ( 16.0 / 116.0 );\n" +
                    "    v.z = ( n.z > 0.008856 ) ? pow( n.z, 1.0 / 3.0 ) : ( 7.787 * n.z ) + ( 16.0 / 116.0 );\n" +
                    "    vec3 cieLab2 = vec3(( 116.0 * v.y ) - 16.0, 500.0 * ( v.x - v.y ), 200.0 * ( v.y - v.z ));" +

                    "//find the error in cielab\n" +
                    "  vec3 err = cieLab - cieLab2;" +

                    "//distribute err\n" +
                    "    mat3 distribute = mat3(\n" +
                    "\t\t1, 0.5, 0,\n" +
                    "        0, 0, 0,\n" +
                    "        0, 1, 1 \n" +
                    "\t);\n" +
                    "    cieLab = cieLab + err*distribute;\n" +

                    "//convet cielab back to xyz\n" +
                    "float fy = ( cieLab.x + 16.0 ) / 116.0;\n" +
                    "    float fx = cieLab.y / 500.0 + fy;\n" +
                    "    float fz = fy - cieLab.z / 200.0;\n" +
                    "    xyz = vec3(\n" +
                    "         95.047 * (( fx > 0.206897 ) ? fx * fx * fx : ( fx - 16.0 / 116.0 ) / 7.787),\n" +
                    "        100.000 * (( fy > 0.206897 ) ? fy * fy * fy : ( fy - 16.0 / 116.0 ) / 7.787),\n" +
                    "        108.883 * (( fz > 0.206897 ) ? fz * fz * fz : ( fz - 16.0 / 116.0 ) / 7.787)\n" +
                    "    );" +

                    "//convet xyz back to rgb\n" +
                    "\tmatter = mat3(\n" +
                    "        3.2406, -1.5372, -0.4986,\n" +
                    "        -0.9689, 1.8758, 0.0415,\n" +
                    "        0.0557, -0.2040, 1.0570\n" +
                    "\t);\n" +
                    "    v = (xyz / vec3(100)) * matter;\n" +
                    "    vec3 r;\n" +
                    "    r.x = ( v.r > 0.0031308 ) ? (( 1.055 * pow( v.r, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.r;\n" +
                    "    r.y = ( v.g > 0.0031308 ) ? (( 1.055 * pow( v.g, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.g;\n" +
                    "    r.z = ( v.b > 0.0031308 ) ? (( 1.055 * pow( v.b, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.b;\n" +
                    "    FragColor.rgb = r;\n" +

                    "   gl_FragColor = FragColor;\n" +
                    "}";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "varying vec2 vTextureCoord;" +
                    "uniform lowp sampler2D sTexture;" +
                    "const highp vec3 weight = vec3(0.2125, 0.7154, 0.0721);" +

                    "void main() {" +

                    "   vec4 FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "   // 点乘运算\n" +
                    "   //gl_FragColor.r = dot(FragColor.rgb, vec3(.393, .769, .189));\n" +
                    "   //gl_FragColor.g = dot(FragColor.rgb, vec3(.349, .686, .168));\n" +
                    "   //gl_FragColor.b = dot(FragColor.rgb, vec3(.272, .534, .131));\n" +
                    "   //gl_FragColor.r = dot(FragColor.rgb, vec3(1.2935, -0.2935, 0));\n" +
                    "   //gl_FragColor.g = dot(FragColor.rgb, vec3(0, 1, 0));\n" +
                    "   //gl_FragColor.b = dot(FragColor.rgb, vec3(-0.2485, 0.2485, 1));\n" +

                    "//convert to xyz\n" +
                    "    //lowp vec3 tmp;\n" +
                    "    //tmp.x = ( FragColor.r > 0.04045 ) ? pow( ( FragColor.r + 0.055 ) / 1.055, 2.4 ) : FragColor.r / 12.92;\n" +
                    "    //tmp.y = ( FragColor.g > 0.04045 ) ? pow( ( FragColor.g + 0.055 ) / 1.055, 2.4 ) : FragColor.g / 12.92,\n" +
                    "    //tmp.z = ( FragColor.b > 0.04045 ) ? pow( ( FragColor.b + 0.055 ) / 1.055, 2.4 ) : FragColor.b / 12.92;\n" +
                    "    lowp mat3 matter = mat3(\n" +
                    "\t\t0.4124, 0.3576, 0.1805,\n" +
                    "        0.2126, 0.7152, 0.0722,\n" +
                    "        0.0193, 0.1192, 0.9505 \n" +
                    "\t);\n" +
                    "    lowp vec3 xyz = 100.0 * FragColor.rgb * matter;\n" +

                    "//convert to cielab\n" +
                    "lowp vec3 n = xyz / vec3(95.047, 100, 108.883);\n" +
                    "    lowp vec3 v;\n" +
                    "    v.x = ( n.x > 0.008856 ) ? pow( n.x, 1.0 / 3.0 ) : ( 7.787 * n.x ) + ( 16.0 / 116.0 );\n" +
                    "    v.y = ( n.y > 0.008856 ) ? pow( n.y, 1.0 / 3.0 ) : ( 7.787 * n.y ) + ( 16.0 / 116.0 );\n" +
                    "    v.z = ( n.z > 0.008856 ) ? pow( n.z, 1.0 / 3.0 ) : ( 7.787 * n.z ) + ( 16.0 / 116.0 );\n" +
                    "    lowp vec3 cieLab = vec3(( 116.0 * v.y ) - 16.0, 500.0 * ( v.x - v.y ), 200.0 * ( v.y - v.z ));\n" +

                    "//multiply to get err xyz\n" +
                    "    mat3 XYZ2BRAD = mat3(\n" +
                    "\t\t0.8951, 0.2664, -0.1614,\n" +
                    "        -0.7502, 1.7135, 0.0367,\n" +
                    "        0.0389, -0.0685, 1.0296 \n" +
                    "\t);\n" +
                    "    mat3 BRAD2XYZ = mat3(\n" +
                    "\t\t0.9870, -0.1471, 0.1600,\n" +
                    "       0.4323, 0.5184, 0.0493,\n" +
                    "        -0.0085, 0.0400, 0.9685 \n" +
                    "\t);\n" +
                    "    mat3 D652E = mat3(\n" +
                    "\t\t1.0521, 0, 0,\n" +
                    "        0, 1, 0,\n" +
                    "        0, 0, 0.9184 \n" +
                    "\t);\n" +
                    "    mat3 E2D65 = mat3(\n" +
                    "\t\t0.9505, 0, 0,\n" +
                    "        0, 1, 0,\n" +
                    "        0, 0, 1.0888 \n" +
                    "\t);\n" +
                    "    mat3 simulate = mat3(\n" +
                    "\t\t1, 0, 0,\n" +
                    "    0.7, 0, 0.3,\n" +
                    "    0, 0, 1 \n" +
                    "\t);\n" +


                    "    lowp mat3 errm = mat3(\n" +
                    "\t\t0.7958, 0.2163, -0.0204,\n" +
                    "        0.7572, 0.1978, 0.0758,\n" +
                    "        0.0637, -0.0675, 1.0064 \n" +
                    "\t);\n" +
                    "    //errm = mat3(\n" +
                    "\t\t//-0.4681, 1.0232, 0.3418,\n" +
                    "        //-0.6766, 1.4715, 0.1575,\n" +
                    "        //0.0145, -0.0101, 0.9966 \n" +
                    "\t//);\n" +
                    "    lowp vec3 xyz2 = xyz * errm;  /*(E2D65 * BRAD2XYZ * simulate * XYZ2BRAD * D652E)*/\n" +

                    "//convert xyz2 to cielab\n" +
                    "n = xyz2 / vec3(95.047, 100, 108.883);\n" +
                    "    v.x = ( n.x > 0.008856 ) ? pow( n.x, 1.0 / 3.0 ) : ( 7.787 * n.x ) + ( 16.0 / 116.0 );\n" +
                    "    v.y = ( n.y > 0.008856 ) ? pow( n.y, 1.0 / 3.0 ) : ( 7.787 * n.y ) + ( 16.0 / 116.0 );\n" +
                    "    v.z = ( n.z > 0.008856 ) ? pow( n.z, 1.0 / 3.0 ) : ( 7.787 * n.z ) + ( 16.0 / 116.0 );\n" +
                    "    lowp vec3 cieLab2 = vec3(( 116.0 * v.y ) - 16.0, 500.0 * ( v.x - v.y ), 200.0 * ( v.y - v.z ));" +

                    "//find the error in cielab\n" +
                    "  lowp vec3 err = cieLab - cieLab2;" +

                    "//distribute err\n" +
                    "    lowp mat3 distribute = mat3(\n" +
                    "\t\t1, 0.5, 0,\n" +
                    "        0, 0, 0,\n" +
                    "        0, 1, 1 \n" +
                    "\t);\n" +
                    "    cieLab = cieLab + err*distribute;\n" +

                    "//convert cielab back to xyz\n" +
                    "float fy = ( cieLab.x + 16.0 ) / 116.0;\n" +
                    "    float fx = cieLab.y / 500.0 + fy;\n" +
                    "    float fz = fy - cieLab.z / 200.0;\n" +
                    "    xyz = vec3(\n" +
                    "         95.047 * (( fx > 0.206897 ) ? fx * fx * fx : ( fx - 16.0 / 116.0 ) / 7.787),\n" +
                    "        100.000 * (( fy > 0.206897 ) ? fy * fy * fy : ( fy - 16.0 / 116.0 ) / 7.787),\n" +
                    "        108.883 * (( fz > 0.206897 ) ? fz * fz * fz : ( fz - 16.0 / 116.0 ) / 7.787)\n" +
                    "    );" +

                    "//convet xyz back to rgb\n" +
                    "\tmatter = mat3(\n" +
                    "        3.2406, -1.5372, -0.4986,\n" +
                    "        -0.9689, 1.8758, 0.0415,\n" +
                    "        0.0557, -0.2040, 1.0570\n" +
                    "\t);\n" +
                    "    v = (xyz / vec3(100)) * matter;\n" +
                    "    //lowp vec3 r;\n" +
                    "    //r.x = ( v.r > 0.0031308 ) ? (( 1.055 * pow( v.r, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.r;\n" +
                    "    //r.y = ( v.g > 0.0031308 ) ? (( 1.055 * pow( v.g, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.g;\n" +
                    "    //r.z = ( v.b > 0.0031308 ) ? (( 1.055 * pow( v.b, ( 1.0 / 2.4 ))) - 0.055 ) : 12.92 * v.b;\n" +
                    "    FragColor.rgb = v;//r;\n" +

                    "   gl_FragColor = FragColor;\n" +
                    "}";

    private float intensity = 1.0f;
    private float[] filterColor = new float[]{0.6f, 0.45f, 0.3f};

    public GlMonochromeFilter() {
        super(DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER1);
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    @Override
    public void onDraw() {
        GLES20.glUniform1f(getHandle("intensity"), intensity);
        GLES20.glUniform3fv(getHandle("filterColor"), 0, filterColor, 0);
    }

    public String getFragmentShaderSource() {
        return FRAGMENT_SHADER;
    }

}
