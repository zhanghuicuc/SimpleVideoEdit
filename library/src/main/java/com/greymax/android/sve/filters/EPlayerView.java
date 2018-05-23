package com.greymax.android.sve.filters;

import android.content.Context;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.greymax.android.sve.filters.chooser.EConfigChooser;
import com.greymax.android.sve.filters.contextfactory.EContextFactory;
import com.greymax.android.sve.filters.filter.GlFilter;

/**
 * Created by sudamasayuki on 2017/05/16.
 * GLSurfaceView is just one way to incorporate OpenGL ES graphics into your application.
 * For a full-screen or near-full screen graphics view, it is a reasonable choice.
 * Developers who want to incorporate OpenGL ES graphics in a small portion of their layouts should take a look at TextureView.
 * For real, do-it-yourself developers, it is also possible to build up an OpenGL ES view using SurfaceView, but this requires writing quite a bit of additional code.
 */
public class EPlayerView extends GLSurfaceView {

    private final static String TAG = EPlayerView.class.getSimpleName();

    private final EPlayerRenderer renderer;
    private MediaPlayer player;

    public EPlayerView(Context context) {
        this(context, null);
    }

    public EPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create an OpenGL ES 2.0 context
        setEGLContextFactory(new EContextFactory());
        setEGLConfigChooser(new EConfigChooser());

        renderer = new EPlayerRenderer(this);
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer);
        //在两个视频连续播放，并且切换滤镜时，使用continuously可以避免最后一帧滤镜错误
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public EPlayerView setPlayer(MediaPlayer player) {
        if (this.player != null) {
            this.player.release();
            this.player = null;
        }
        this.player = player;
        this.renderer.setPlayer(player);
        return this;
    }

    public void setGlFilter(GlFilter glFilter) {
        renderer.setGlFilter(glFilter);
    }

    public void setGlFilterIntensity(int intensity) {
        renderer.setGlFilterIntensity(intensity);
    }

    public GlFilter getGlFilter() {
        return renderer.getGlFilter();
    }
}
