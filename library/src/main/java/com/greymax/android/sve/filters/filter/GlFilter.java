package com.greymax.android.sve.filters.filter;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import com.greymax.android.sve.filters.EFramebufferObject;
import com.greymax.android.sve.filters.EglUtil;

import java.util.HashMap;
import java.util.LinkedList;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUseProgram;

/**
 * Created by sudamasayuki on 2017/05/16.
 */

public class GlFilter {

    public static final String DEFAULT_UNIFORM_SAMPLER = "sTexture";

    /*
    * Drawing a defined shape using OpenGL ES 2.0 requires a significant amount of code,
    * because you must provide a lot of details to the graphics rendering pipeline.
    * Specifically, you must define the following:
    *
    * Vertex Shader - OpenGL ES graphics code for rendering the vertices of a shape.
    * Fragment Shader - OpenGL ES code for rendering the face of a shape with colors or textures.
    * Program - An OpenGL ES object that contains the shaders you want to use for drawing one or more shapes.
    * You need at least one vertex shader to draw a shape and one fragment shader to color that shape.
    * These shaders must be compiled and then added to an OpenGL ES program, which is then used to draw the shape
    *
    * Shaders contain OpenGL Shading Language (GLSL) code that must be compiled prior to using it in the OpenGL ES environment.
     * To compile this code, create a loadShader method in your renderer class, see setUp() and EglUtil.java
     *
     * In order to draw your shape, you must compile the shader code, add them to a OpenGL ES program object
      * and then link the program. Do this in your drawn object’s constructor, so it is only done once.
     * Note: Compiling OpenGL ES shaders and linking programs is expensive in terms of CPU cycles and processing time,
     * so you should avoid doing this more than once. If you do not know the content of your shaders at runtime,
     * you should build your code such that they only get created once and then cached for later use.
     * see setUp()
    * */
    protected static final String DEFAULT_VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "//a_Position is defined as a vec4, which has\n" +
                    "//four components. If a component is not specified, OpenGL\n" +
                    "//will set the first three components to 0 and the last\n" +
                    "//component to 1 by default. \n" +
                    "attribute vec4 aTextureCoord;\n" +

                    "// Varyings：用来在Vertex shader和Fragment shader之间传递信息的，" +
                    "比如在Vertex shader中写入varying值，" +
                    "然后就可以在Fragment shader中读取和处理\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "gl_Position = aPosition;\n" +
                    "vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    protected static final String DEFAULT_FRAGMENT_SHADER =
			"// defines the default precision for all floating\n" +
			"// point data types in the fragment shader\n" +
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
					
					"// Unlike an attribute that is set on each vertex,\n" + 
					"// a uniform keeps the same value for all vertices until we change it again.\n" +
                    "// So if you have several colors in one surface, you need to set uniform colors multiple times. \n" +

                    "// 接收一个图片的引用，当做2D的纹理，这个数据类型就是smpler2D \n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "void main() {\n" +
					
					"// gl_FragColor is a special output value\n" +
					"// shader must write something to gl_FragColor. OpenGL will use this color\n" +
					"// as the final color for the current fragment.\n" +

                    "// 根据坐标取样图片颜色信息\n" +
                    "gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";


    //顶点的坐标, uv是纹理坐标，代表纹理怎么铺到图形上
    private static final float[] VERTICES_DATA = new float[]{
            // X, Y, Z, U, V
            //问题：为什么这里定义的顺序不是逆时针的？答案：因为后面用的是GL_TRIANGLE_STRIP
            //see http://blog.csdn.net/xiajun07061225/article/details/7455283
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f
    };

    private static final int FLOAT_SIZE_BYTES = 4;
    protected static final int VERTICES_DATA_POS_SIZE = 3;
    protected static final int VERTICES_DATA_UV_SIZE = 2;
    protected static final int VERTICES_DATA_STRIDE_BYTES = (VERTICES_DATA_POS_SIZE + VERTICES_DATA_UV_SIZE) * FLOAT_SIZE_BYTES;
    protected static final int VERTICES_DATA_POS_OFFSET = 0 * FLOAT_SIZE_BYTES;
    protected static final int VERTICES_DATA_UV_OFFSET = VERTICES_DATA_POS_OFFSET + VERTICES_DATA_POS_SIZE * FLOAT_SIZE_BYTES;

    private final String vertexShaderSource;
    private final String fragmentShaderSource;

    private final LinkedList<Runnable> mRunOnDraw;

    private int program;

    private int vertexShader;
    private int fragmentShader;

    private int vertexBufferName;

    private final HashMap<String, Integer> handleMap = new HashMap<String, Integer>();

    public GlFilter() {
        this(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
    }

    public GlFilter(final Resources res, final int vertexShaderSourceResId, final int fragmentShaderSourceResId) {
        this(res.getString(vertexShaderSourceResId), res.getString(fragmentShaderSourceResId));
    }

    public GlFilter(final String vertexShaderSource, final String fragmentShaderSource) {
        mRunOnDraw = new LinkedList<Runnable>();
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    public void setup() {
        //release();为什么要release,为什么去掉就好了
        checkGlError("A");
        vertexShader = EglUtil.loadShader(vertexShaderSource, GL_VERTEX_SHADER);
        fragmentShader = EglUtil.loadShader(fragmentShaderSource, GL_FRAGMENT_SHADER);
        program = EglUtil.createProgram(vertexShader, fragmentShader);
        //create vertex byte buffer for shape coordinates
        vertexBufferName = EglUtil.createBuffer(VERTICES_DATA);

        /*
        * At this point, you are ready to add the actual calls that draw your shape.
        * Drawing shapes with OpenGL ES requires that you specify several parameters to tell the rendering pipeline
        * what you want to draw and how to draw it. Since drawing options can vary by shape,
        * it's a good idea to have your shape classes contain their own drawing logic.
        *
        * Create a draw() method for drawing the shape. This code sets the position and color values to the shape’s
        * vertex shader and fragment shader, and then executes the drawing function.
        * */
    }

    public void setFrameSize(final int width, final int height) {
    }

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public void setIntensity(final int intValue) {
    }

    public void release() {
        GLES20.glDeleteProgram(program);
        program = 0;
        GLES20.glDeleteShader(vertexShader);
        vertexShader = 0;
        GLES20.glDeleteShader(fragmentShader);
        fragmentShader = 0;
        GLES20.glDeleteBuffers(1, new int[]{vertexBufferName}, 0);
        vertexBufferName = 0;

        handleMap.clear();
    }

    public void draw(final int texName, final EFramebufferObject fbo) {
        // Add program to OpenGL ES environment, tell OpenGL to use the program defined here when drawing anything to the screen.
        useProgram();

        runPendingOnDrawTasks();
        
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferName);
        //enable the attribute with a call to glEnableVertexAttribArray() before we can start drawing
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"));
        //Prepare the triangle coordinate data
        /*
        * We then call glVertexAttribPointer() to tell OpenGL that it can find the data for
        * a_Position in the buffer vertexData. This is a very important function.
        * Since we used glBindBuffer before, so there is some difference from regular call, see opengl es reference for details
        * */
        GLES20.glVertexAttribPointer(getHandle("aPosition"), VERTICES_DATA_POS_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET);

        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), VERTICES_DATA_UV_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texName);
        //get handle to fragment shader's sTexture member, set color
        GLES20.glUniform1i(getHandle("sTexture"), 0);

        //Set color for drawing the square according to different filters
        onDraw();

        //Draw the sqaure
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(getHandle("aPosition"));
        GLES20.glDisableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        /*
        * Once you have all this code in place, drawing this object
        * just requires a call to the draw() method from within your renderer’s onDrawFrame() method
        * */
    }

    protected void onDraw() {
    }

    protected final void useProgram() {
        glUseProgram(program);
    }

    protected final int getVertexBufferName() {
        return vertexBufferName;
    }

    protected final int getHandle(final String name) {
        final Integer value = handleMap.get(name);
        if (value != null) {
            return value.intValue();
        }

        /*
        * When OpenGL links our shaders into a program, it will
        * actually associate each uniform/attribute defined in the vertex shader with a location
        * number. These location numbers are used to send data to the shader, and
        * we’ll need these locations so that we can set the color/vertices when we’re about to draw.
        *
        * locations don’t get specified beforehand, so we’ll need to
        * query the location once the program’s been successfully
        * linked. A uniform’s location is unique to a program object: even if
        * we had the same uniform name in two different programs, that doesn’t mean
        * that they’ll share the same location.
        * */

        //We call glGetAttribLocation() to get the location of our attribute. With this location,
        // we’ll be able to tell OpenGL where to find the data for this attribute.
        int location = glGetAttribLocation(program, name);
        if (location == -1) {
            //We call glGetUniformLocation() to get the location of our uniform, and we store
            // that location. We’ll use that when we want to update the value
            // of this uniform later on.
            location = glGetUniformLocation(program, name);
        }
        if (location == -1) {
            throw new IllegalStateException("Could not get attrib or uniform location for " + name);
        }
        handleMap.put(name, Integer.valueOf(location));
        return location;
    }

    public String getVertexShaderSource() {
        return DEFAULT_VERTEX_SHADER;
    }

    public String getFragmentShaderSource() {
        return DEFAULT_FRAGMENT_SHADER;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GlFilter", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

}
