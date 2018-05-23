package com.greymax.android.sve.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.greymax.android.sve.SVE;
import com.greymax.android.sve.app.databinding.ActivityVideoOutputBinding;
import com.greymax.android.sve.app.utils.FileUtils;
import com.greymax.android.sve.app.utils.UIUtil;
import com.greymax.android.sve.transcode.TranscodeVideoListener;
import com.greymax.android.sve.filters.EPlayerView;
import com.greymax.android.sve.filters.FilterType;
import com.greymax.android.sve.timeline.ConcatVideoListener;

import java.io.File;
import java.io.IOException;

public class VideoOutputActivity extends AppCompatActivity implements View.OnClickListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        TranscodeVideoListener, ConcatVideoListener{

    private static final int MSG_COMPRESS_NEXT = 0;
    private static final int MSG_COMPRESS_FINISH = 1;
    private static final int MSG_CONCAT_FINISH = 2;

    private ActivityVideoOutputBinding binding;
    private ProgressDialog mProgressDialog;
    private EPlayerView ePlayerView;
    private MediaPlayer player;
    private int playlistIndex = 0;
    private int compressIndex = 0;
    private String compressedVideoPath;
    private String outputVideoPath;
    private SVE mSVE;

    public static void go(FragmentActivity from){
        Intent intent = new Intent(from,VideoOutputActivity.class);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_output);
        binding.outputButton.setOnClickListener(this);
        binding.outputButton.setTextAppearance(this, R.style.blue_text_18_style);
        compressedVideoPath = FileUtils.getSaveCompressVideoDir(this);
        outputVideoPath = FileUtils.getOutputVideoDir(this);
        mSVE = SVE.getSVE(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        setMediaPlayer();
        setUpGlPlayerView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.outputButton.getId()) {
            releasePlayer();
            buildDialog(getResources().getString(R.string.compressing) + compressIndex).show();
            mSVE.transcodeClip(mSVE.getAllInputClips().get(0), this, compressedVideoPath);
        }
    }

    private void setMediaPlayer() {
        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
        try {
            player.setDataSource(mSVE.getAllInputClips().get(0).getClipPath());
            player.prepare();
            player.start();
        } catch (IOException e) {

        }
    }

    private void setUpGlPlayerView() {
        ePlayerView = new EPlayerView(this);
        ePlayerView.setPlayer(player);
        ePlayerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.video_output_loader)).addView(ePlayerView);
        ePlayerView.onResume();
    }

    private void releasePlayer() {
        //the view can release opengl context
        if (ePlayerView != null) {
            ePlayerView.onPause();
            ((FrameLayout) findViewById(R.id.video_output_loader)).removeAllViews();
            ePlayerView = null;
        }
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp){
        int index = (++playlistIndex) % mSVE.getAllInputClips().size();
        try {
            mp.reset();
            mp.setDataSource(mSVE.getAllInputClips().get(index).getClipPath());
            mp.prepare();
            mp.start();
        } catch (IOException e) {

        }

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            int index = (playlistIndex) % mSVE.getAllInputClips().size();
            ePlayerView.setGlFilter(FilterType.createGlFilter(mSVE.getAllInputClips().get(index).getClipfilter(),this));
            return true;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        ViewGroup.LayoutParams lp = ((FrameLayout)findViewById(R.id.video_output_loader)).getLayoutParams();
        int screenWidth = UIUtil.getScreenWidth(getApplicationContext());
        lp.width = screenWidth;
        lp.height = (int) ((float) screenWidth / (16.0/9.0));
        ((FrameLayout)findViewById(R.id.video_output_loader)).setLayoutParams(lp);
    }

    private Handler mainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_COMPRESS_NEXT) {
                buildDialog(getResources().getString(R.string.compressing) + msg.obj).show();
            } else if (msg.what == MSG_COMPRESS_FINISH) {
                buildDialog(getResources().getString(R.string.concating)).show();
            }
        }
    };

    private ProgressDialog buildDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "", msg);
        }
        mProgressDialog.setMessage(msg);
        return mProgressDialog;
    }

    @Override
    public void onStartTranscode() {

    }

    @Override
    public void onFinishTranscode(String message) {
        mSVE.addOneOutputClip(message);
        compressIndex++;
        if (compressIndex < mSVE.getAllInputClips().size()) {
            Message msg = new Message();
            msg.what = MSG_COMPRESS_NEXT;
            msg.obj = compressIndex;
            mainHandler.sendMessage(msg);
            mSVE.transcodeClip(mSVE.getAllInputClips().get(compressIndex), this, compressedVideoPath);
        } else {
            Message msg = new Message();
            msg.what = MSG_COMPRESS_FINISH;
            msg.obj = message;
            mainHandler.sendMessage(msg);
            mSVE.concatForOutput(outputVideoPath, this);
        }
    }

    @Override
    public void onCancelTranscode() {

    }

    @Override
    public void onStartConcat() {

    }

    @Override
    public void onFinishConcat(String message) {
        if (!TextUtils.isEmpty(compressedVideoPath)) {
            FileUtils.deleteFile(new File(compressedVideoPath));
        }
        if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
        finish();
    }

    @Override
    public void onCancelConcat() {

    }

    @Override
    public void onConcatFail() {
        if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
        Toast.makeText(this, "Concat Fail", Toast.LENGTH_LONG);
    }
}
