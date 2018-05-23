package com.greymax.android.sve.filters.filter;

import android.opengl.GLES20;

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;

/**
 * Created by sudamasayuki on 2017/05/16.
 */

public class GlPreviewFilter extends GlFilter {

    public static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    /*
    * (-1, 1) ------------- (1, 1)
    *    |                     |
    *    |                     |
    *    |                     |
    *    |                     |
    * (-1, -1)--------------(1, -1)
    * Default opengl coordinate system.
    * On square screen, opengl is able to draw graphics perfectly, and draws those coordinates onto your typically
    * non-square screen as if it is perfectly square. This will cause opengl to draw things wrong on non-square screen.
    *
    * To solve this problem, you can apply
    * OpenGL projection modes
    * and camera views
    * to transform coordinates so your graphic objects have the correct proportions on any display.
    *
    * In order to apply projection and camera views, you create
    * a projection matrix
    * and a camera view matrix
    * and apply them to the OpenGL rendering pipeline.
    *
    * The projection matrix recalculates the coordinates of your graphics so that they map correctly to Android device screens.
    * A projection transformation typically only has to be calculated when the proportions of the OpenGL view are established
    * or changed in the onSurfaceChanged() method of your renderer
    *
    * The camera view matrix creates a transformation that renders objects from a specific eye position.
    * A camera view transformation might be calculated only once when you establish your GLSurfaceView,
    * or might change dynamically based on user actions or your application’s function.
    *
    * In the ES 2.0 and 3.0 APIs, you apply projection and camera view by first adding a matrix member to the vertex shaders
    * of your graphics objects.
    * With this matrix member added, you can then generate and apply projection and camera viewing matrices to your objects.
    * */

    /*
    * First Step: Add matrix to vertex shaders - Create a variable for the view projection matrix and include it as a
    * multiplier of the shader's position. In the following example vertex shader code, the included
    * uMVPMatrix member allows you to apply projection and camera viewing matrices to the coordinates of objects
    * that use this shader.
    * */
    private static final String VERTEX_SHADER =
            "// This matrix member variable provides a hook to manipulate \n" +
            "// the coordinates of objects that use this vertex shader. \n" +
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "uniform float uCRatio;\n" +
					"//vec4 is a a vector consisting of four components. In the context of a position,\n" +
					"//we can think of the four components as the position’s x, y, z, and w coordinates.\n" +
					"//x, y, and z correspond to a 3D position, while w is a special coordinate.\n" +
					"//If unspecified, OpenGL’s default behavior is to set the first three\n" +
					"//coordinates of a vector to 0 and the last coordinate to 1.\n" +
					
					"//This vertex shader will receive the current vertex’s position in the a_Position\n" +
					"//attribute, which is defined to be a vec4.\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying highp vec2 vTextureCoord;\n" +

                    "void main() {\n" +
                    "vec4 scaledPos = aPosition;\n" +
                    "scaledPos.x = scaledPos.x * uCRatio;\n" +
                    "// The matrix must be included as part of gl_Position\n" +
                    "// Note that the uMVPMatrix factor *must be first* in order\n" +
                    "// for the matrix multiplication product to be correct.\n" +
					
					"// gl_Position is a special output variable\n" +
					"// shader must write something to gl_Position. OpenGL will use the value stored\n" +
					"// in gl_Position as the final position for the current vertex and start assembling\n" +
					"//vertices into points, lines, and triangles.\n" +
                    "gl_Position = uMVPMatrix * scaledPos;\n" +
                    "vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private final int texTarget;

    public GlPreviewFilter(final int texTarget) {
        super(VERTEX_SHADER, createFragmentShaderSourceOESIfNeed(texTarget));
        this.texTarget = texTarget;
    }

    private static String createFragmentShaderSourceOESIfNeed(final int texTarget) {
        if (texTarget == GL_TEXTURE_EXTERNAL_OES) {
            return new StringBuilder()
                    .append("#extension GL_OES_EGL_image_external : require\n")
                    .append(DEFAULT_FRAGMENT_SHADER.replace("sampler2D", "samplerExternalOES"))
                    .toString();
        }
        return DEFAULT_FRAGMENT_SHADER;
    }

    public void draw(final int texName, final float[] mvpMatrix, final float[] stMatrix, final float aspectRatio) {
        useProgram();

        /*
        * Projection and camera view in OpenGL ES 2.0: Third Step:
        * Second Step is in EPlayerRenderer.java where we get the projection matrix and camera viewing matrix
        * and combine them together.
        * Apply the combined projection and camera view transformations, in this way we access
        * the matrix variable defined in the vertex shader above.
        * */

        // get handle to shape's transformation matrix and Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(getHandle("uMVPMatrix"), 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(getHandle("uSTMatrix"), 1, false, stMatrix, 0);
        GLES20.glUniform1f(getHandle("uCRatio"), aspectRatio);

        GLES20.glBindBuffer(GL_ARRAY_BUFFER, getVertexBufferName());
        GLES20.glEnableVertexAttribArray(getHandle("aPosition"));
        GLES20.glVertexAttribPointer(getHandle("aPosition"), VERTICES_DATA_POS_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET);
        GLES20.glEnableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glVertexAttribPointer(getHandle("aTextureCoord"), VERTICES_DATA_UV_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET);

        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glBindTexture(texTarget, texName);
        GLES20.glUniform1i(getHandle(DEFAULT_UNIFORM_SAMPLER), 0);

        GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(getHandle("aPosition"));
        GLES20.glDisableVertexAttribArray(getHandle("aTextureCoord"));
        GLES20.glBindBuffer(GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);
    }
}
