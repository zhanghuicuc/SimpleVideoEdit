package com.greymax.android.sve.transcode;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.greymax.android.sve.filters.EFramebufferObject;
import com.greymax.android.sve.filters.EglUtil;
import com.greymax.android.sve.filters.filter.GlFilter;
import com.greymax.android.sve.filters.filter.GlPreviewFilter;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glViewport;


public class FrameBufferObjectRenderer {

    private EFramebufferObject framebufferObject;
    private GlFilter normalShader;

    private static final String TAG = FrameBufferObjectRenderer.class.getSimpleName();

    private int texName;

    //Model View Projection Matrix
    private float[] MVPMatrix = new float[16];
    private float[] ProjMatrix = new float[16];
    private float[] MMatrix = new float[16];
    private float[] VMatrix = new float[16];
    private float[] STMatrix = new float[16];


    private EFramebufferObject filterFramebufferObject;
    private GlPreviewFilter previewFilter;

    private GlFilter glFilter;
    private boolean isNewFilter;

    private float aspectRatio = 1f;

    public FrameBufferObjectRenderer(GlFilter filter) {
        glFilter = filter;
        //设置stmatrix为单位矩阵
        //stmatrix stand for scale and translation缩放和平移
        Matrix.setIdentityM(STMatrix, 0);
    }

    public final void surfaceCreated() {
        framebufferObject = new EFramebufferObject();
        normalShader = new GlFilter();
        normalShader.setup();
        onSurfaceCreated();
    }

    public final void surfaceChanged(final int width, final int height) {
        framebufferObject.setup(width, height);
        normalShader.setFrameSize(width, height);
        onSurfaceChanged(width, height);
    }

    public final void drawFrame(SurfaceTexture previewTexture) {
        framebufferObject.enable();
        GLES20.glViewport(0, 0, framebufferObject.getWidth(), framebufferObject.getHeight());

        onDrawFrame(previewTexture, framebufferObject);
        GLES20.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, framebufferObject.getWidth(), framebufferObject.getHeight());
        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        normalShader.draw(framebufferObject.getTexName(), null);
    }

    protected void finalize() throws Throwable {

    }

    public int getTextureId() {
        return texName;
    }

    public void onSurfaceCreated() {
        // Set the background frame color， these params are red, green, blue and alpha
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        final int[] args = new int[1];

        GLES20.glGenTextures(args.length, args, 0);
        texName = args[0];

        GLES20.glBindTexture(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES, texName);
        // GL_TEXTURE_EXTERNAL_OES
        EglUtil.setupSampler(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES, GL_LINEAR, GL_NEAREST);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        filterFramebufferObject = new EFramebufferObject();
        // GL_TEXTURE_EXTERNAL_OES
        previewFilter = new GlPreviewFilter(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES);
        //initialize shapes
        previewFilter.setup();

        /*
        * Projection and camera view in OpenGL ES 2.0: Second Step:
        * First Step is in GlPreviewFilter.java where we declare the vertex shader with uMVPMatrix
        * Create a camera view matrix
        * */
        Matrix.setLookAtM(VMatrix, 0,
                0.0f, 0.0f, 5.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        );

        if (glFilter != null) {
            isNewFilter = true;
        }

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);
    }

    public void onSurfaceChanged(int width, int height){
        Log.d(TAG, "onSurfaceChanged width = " + width + "  height = " + height);
        filterFramebufferObject.setup(width, height);
        previewFilter.setFrameSize(width, height);
        if (glFilter != null) {
            glFilter.setFrameSize(width, height);
        }

        /*
        * Projection and camera view in OpenGL ES 2.0: Second Step:
        * First Step is in GlPreviewFilter.java where we declare the vertex shader with uMVPMatrix
        * create a projection matrix from device screen geometry
        * */
        aspectRatio = (float) width / height;
        Matrix.frustumM(ProjMatrix, 0, -aspectRatio, aspectRatio, -1, 1, 5, 7);
        Matrix.setIdentityM(MMatrix, 0);
    }

    public void onDrawFrame(SurfaceTexture previewTexture, EFramebufferObject fbo){

        previewTexture.getTransformMatrix(STMatrix);
        if (isNewFilter) {
            if (glFilter != null) {
                //initialize shapes
                glFilter.setup();
                glFilter.setFrameSize(fbo.getWidth(), fbo.getHeight());
            }
            isNewFilter = false;
        }
        if (glFilter != null) {
            filterFramebufferObject.enable();
            //glViewport tells OpenGL the size of the surface it has available for rendering
            glViewport(0, 0, filterFramebufferObject.getWidth(), filterFramebufferObject.getHeight());
        }
		/*
		* We clear the screen in onDrawFrame() with a call to glClear(GL_COLOR_BUFFER_BIT).
		* This will wipe out all colors on the screen and fill the screen with the color
		* previously defined by our call to glClearColor().
		* In short, we will not preserve previous screen content.
		*/
        GLES20.glClear(GL_COLOR_BUFFER_BIT);
        /*
        * Projection and camera view in OpenGL ES 2.0: Second Step:
        * First Step is in GlPreviewFilter.java where we declare the vertex shader with uMVPMatrix
        * Combine the projection and camera view matrices
        * */
        Matrix.multiplyMM(MVPMatrix, 0, VMatrix, 0, MMatrix, 0);
        Matrix.multiplyMM(MVPMatrix, 0, ProjMatrix, 0, MVPMatrix, 0);
        //Here we go to Projection and camera view in OpenGL ES 2.0: Third Step
        previewFilter.draw(texName, MVPMatrix, STMatrix, aspectRatio);

        if (glFilter != null) {
            fbo.enable();
            GLES20.glClear(GL_COLOR_BUFFER_BIT);
            //draw the shape(square)
            glFilter.draw(filterFramebufferObject.getTexName(), fbo);
        }
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }
}
