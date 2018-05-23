package com.greymax.android.sve.filters;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.greymax.android.sve.filters.filter.GlFilter;
import com.greymax.android.sve.filters.filter.GlLookUpTableFilter;
import com.greymax.android.sve.filters.filter.GlPreviewFilter;
import com.greymax.android.sve.filters.filter.GlSepiaFilter;

import javax.microedition.khronos.egl.EGLConfig;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_FRONT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_MAX_TEXTURE_SIZE;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glViewport;

/*
* This package provides the interface to OpenGL ES 2.0 and is available starting with Android 2.2 (API level 8).
* The OpenGL ES 3.x API is backwards-compatible with the 2.0 API, which means you can be more flexible with your
* implementation of OpenGL ES in your application. By declaring the OpenGL ES 2.0 API as a requirement in your manifest,
* you can use that API version as a default,
* check for the availability of the 3.x API at run time and then use OpenGL ES 3.x features if the device supports it
* */

/**
 * Created by sudamasayuki on 2017/05/16.
 * This interface defines the methods required for drawing graphics in a GLSurfaceView.
 * You must provide an implementation of this interface as
 * a separate class and attach it to your GLSurfaceView instance using GLSurfaceView.setRenderer().
 */

class EPlayerRenderer extends EFrameBufferObjectRenderer implements SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = EPlayerRenderer.class.getSimpleName();

    private SurfaceTexture previewTexture;
    private boolean updateSurface = false;

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
    private final EPlayerView glPreview;

    private float aspectRatio = 1f;

    private MediaPlayer simplePlayer;

    EPlayerRenderer(EPlayerView glPreview) {
        super();
        //设置stmatrix为单位矩阵
        //stmatrix stand for scale and translation缩放和平移
        Matrix.setIdentityM(STMatrix, 0);
        this.glPreview = glPreview;
    }

    void setGlFilter(final GlFilter filter) {
		/*
		* Since Android’s GLSurfaceView does rendering in a background thread, we must be
		* careful to call OpenGL only within the rendering thread, and Android UI calls only
		* within Android’s main thread. We can call queueEvent() on our instance of GLSurfaceView
		* to post a Runnable on the background rendering thread. From within the rendering
		* thread, we can call runOnUIThread() on our activity to post events on the main thread.
		*/
        glPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (glFilter != null) {
                    glFilter.release();
                    if (glFilter instanceof GlLookUpTableFilter) {
                        ((GlLookUpTableFilter) glFilter).releaseLutBitmap();
                    }
                    glFilter = null;
                }
                glFilter = filter;
                isNewFilter = true;
                glPreview.requestRender();
            }
        });
    }

    public void setGlFilterIntensity(int intensity) {
        if (glFilter instanceof GlSepiaFilter) {
            glFilter.setIntensity(intensity);
        } else {
            return;
        }
    }

    public GlFilter getGlFilter() {
        return glFilter;
    }

    /**
     * onSurfaceCreated(): The system calls this method once, when creating the GLSurfaceView. Use this method to perform
     * actions that need to happen only once, such as setting OpenGL environment parameters or initializing OpenGL graphic objects.
     */
    @Override
    public void onSurfaceCreated(final EGLConfig config) {
        // Set the background frame color， these params are red, green, blue and alpha
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        // enable face culling feature
        GLES20.glEnable(GL_CULL_FACE);
        // specify which faces to not draw
        GLES20.glCullFace(GL_FRONT);

        final int[] args = new int[1];

        GLES20.glGenTextures(args.length, args, 0);
        texName = args[0];


        previewTexture = new SurfaceTexture(texName);
        previewTexture.setOnFrameAvailableListener(this);


        GLES20.glBindTexture(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES, texName);
        // GL_TEXTURE_EXTERNAL_OES
        EglUtil.setupSampler(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES, GL_LINEAR, GL_NEAREST);
        GLES20.glBindTexture(GL_TEXTURE_2D, 0);

        filterFramebufferObject = new EFramebufferObject();
        // GL_TEXTURE_EXTERNAL_OES
        previewFilter = new GlPreviewFilter(GlPreviewFilter.GL_TEXTURE_EXTERNAL_OES);
        //initialize shapes
        previewFilter.setup();

        Surface surface = new Surface(previewTexture);
        this.simplePlayer.setSurface(surface);

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

        synchronized (this) {
            updateSurface = false;
        }

        if (glFilter != null) {
            isNewFilter = true;
        }

        GLES20.glGetIntegerv(GL_MAX_TEXTURE_SIZE, args, 0);

    }

    /*
    * onSurfaceChanged(): The system calls this method when the GLSurfaceView geometry changes,
    * including changes in size of the GLSurfaceView or orientation of the device screen.
    * For example, the system calls this method when
    * the device changes from portrait to landscape orientation.
    * Use this method to respond to changes in the GLSurfaceView container
    * */
    @Override
    public void onSurfaceChanged(final int width, final int height) {
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

    /*
    * onDrawFrame(): The system calls this method on each redraw of the GLSurfaceView.
    * Use this method as the primary execution point for drawing (and re-drawing) graphic objects.
    * */
    @Override
    public void onDrawFrame(final EFramebufferObject fbo) {

        synchronized (this) {
            if (updateSurface) {
                previewTexture.updateTexImage();
                previewTexture.getTransformMatrix(STMatrix);
                updateSurface = false;
            }
        }

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

    @Override
    public synchronized void onFrameAvailable(final SurfaceTexture previewTexture) {
        updateSurface = true;
        /*
        * requestRender() tell the renderer that it is time to render the frame.
        * However, it does not have any impact on efficiency unless you also request that the renderer
        * only redraw when the data changes using the setRenderMode() method
        * */
        glPreview.requestRender();
    }

    void setPlayer(MediaPlayer simplePlayer) {
        this.simplePlayer = simplePlayer;
    }

}
