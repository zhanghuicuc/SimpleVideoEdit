package com.greymax.android.sve.app;

import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.greymax.android.sve.SVE;
import com.greymax.android.sve.app.utils.ExtractFrameWorkThread;
import com.greymax.android.sve.app.utils.FileUtils;
import com.greymax.android.sve.app.utils.UIUtil;
import com.greymax.android.sve.app.widget.EditSpacingItemDecoration;
import com.greymax.android.sve.app.widget.FilterListAdapter;
import com.greymax.android.sve.app.widget.RangeSeekBar;
import com.greymax.android.sve.app.widget.VideoThumbAdapter;
import com.greymax.android.sve.filters.EPlayerView;
import com.greymax.android.sve.filters.FilterType;
import com.greymax.android.sve.timeline.TrimVideoListener;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.List;

public class TrimmerActivity extends AppCompatActivity implements TrimVideoListener {

    private static final String TAG = TrimmerActivity.class.getSimpleName();
    public static final int VIDEO_TRIM_REQUEST_CODE = 0x003;
    public static final int VIDEO_TRIM_RESPONSE_CODE = 0x004;
    public static final int INIT_THUMBVIEW_GAP = 0;//dp
    public static final long MIN_CUT_DURATION = 3 * 1000L;// 最小剪辑时间3s
    public static final long MAX_CUT_DURATION = 15 * 1000L;//视频最多剪切多长时间
    public static final int MAX_THUMBNAIL_COUNT = 5;//seekBar的区域内一共有多少张图片
    private ProgressDialog mProgressDialog;

    //播放器
    private String mSrcPath;
    private EPlayerView mPlayerView;
    private MediaPlayer mPlayer;
    //滤镜列表
    private FilterType mCurrentFilterType = FilterType.DEFAULT;
    private ListView mFilterListView;
    private FilterListAdapter filterListAdapter;
    private final List<FilterType> filterTypes = FilterType.createFilterList();
    //Listener
    private TrimVideoListener mOnTrimVideoListener;
    //底部缩略图列表
    private TextView mTextTimeFrame;
    private RecyclerView mRecyclerView;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private VideoThumbAdapter mVideoThumbAdapter;
    private boolean bSeeking;
    private int mScaledTouchSlop;
    private boolean bOverScaledTouchSlop;
    private int lastScrollX;
    private long scrolledMs = 0;
    private float msPerPx;//每毫秒所占的px
    private float pxPerMs;//每px所占用的ms毫秒
    private String thumbnailPath;
    private String trimmedVideoPath;
    //底部入点和出点
    private long mStartPositionMs, mEndPositionMs;
    //底部seekbar
    private LinearLayout seekBarLayout;
    private RangeSeekBar seekBar;
    private ImageView positionIcon;
    private ValueAnimator animator;


    private long mDuration;
    private int mMaxWidth;

    private SVE mSVE;

    public static void go(FragmentActivity from, String videoPath){
        if(!TextUtils.isEmpty(videoPath)) {
            Bundle bundle = new Bundle();
            bundle.putString("path", videoPath);
            Intent intent = new Intent(from,TrimmerActivity.class);
            intent.putExtras(bundle);
            from.startActivityForResult(intent,VIDEO_TRIM_REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_trimmer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle bd = getIntent().getExtras();
        if(bd != null)
            mSrcPath = bd.getString("path");

        mSVE = SVE.getSVE(getApplicationContext());
        init(this, mSrcPath);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPlayer != null) {
            mPlayer.seekTo((int) mStartPositionMs);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null && mPlayer.isPlaying()) {
            videoPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOnTrimVideoListener = null;
        if (animator != null) {
            animator.cancel();
        }
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }
        mUIHandler.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);
        if (!TextUtils.isEmpty(thumbnailPath)) {
            FileUtils.deleteFile(new File(thumbnailPath));
        }
        releasePlayer();
    }

    private void setUpPlayer(String path) {
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                ViewGroup.LayoutParams lp = ((FrameLayout)findViewById(R.id.video_loader)).getLayoutParams();
                int videoWidth = mPlayer.getVideoWidth();
                int videoHeight = mPlayer.getVideoHeight();
                float videoProportion = (float) videoWidth / (float) videoHeight;
                int screenWidth = UIUtil.getScreenWidth(getApplicationContext());
                int screenHeight = UIUtil.getScreenHeight(getApplicationContext());
                float screenProportion = (float) screenWidth / (float) screenHeight;

                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }
                ((FrameLayout)findViewById(R.id.video_loader)).setLayoutParams(lp);

                ViewGroup.LayoutParams lp1 = ((ListView)findViewById(R.id.filter_list)).getLayoutParams();
                ViewGroup.LayoutParams lp2 = ((RelativeLayout)findViewById(R.id.layout)).getLayoutParams();
                lp1.height = UIUtil.getScreenHeight(getApplicationContext())- lp.height - lp2.height;
                ((ListView)findViewById(R.id.filter_list)).setLayoutParams(lp1);

                mDuration = mp.getDuration();
                initEditVideo();
                videoStart();
            }
        });
        mPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
            Log.d(TAG, "seek complete, curPos:" + mp.getCurrentPosition());
            }
        });
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.seekTo((int)mStartPositionMs);
            }
        });
        try {
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpGlPlayerView() {
        mPlayerView = new EPlayerView(this);
        mPlayerView.setPlayer(mPlayer);
        mPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.video_loader)).addView(mPlayerView);
        mPlayerView.onResume();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        if (mPlayerView != null) {
            mPlayerView.onPause();
        }
        ((FrameLayout)findViewById(R.id.video_loader)).removeAllViews();
    }

    private void init(final Context context, String path) {
        mOnTrimVideoListener = this;

        setUpPlayer(path);
        setUpGlPlayerView();

        mFilterListView = (ListView) findViewById(R.id.filter_list);
        filterListAdapter = new FilterListAdapter(context, R.layout.row_text, filterTypes);
        mFilterListView.setAdapter(filterListAdapter);
        mFilterListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCurrentFilterType = filterTypes.get(position);
                mPlayerView.setGlFilter(FilterType.createGlFilter(filterTypes.get(position), context));
            }
        });


        findViewById(R.id.cancelBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnTrimVideoListener.onCancelTrim();
            }
        });
        findViewById(R.id.finishBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mEndPositionMs - mStartPositionMs < MIN_CUT_DURATION) {
                            Toast.makeText(context, "视频长不足" + MIN_CUT_DURATION/1000 + "秒", Toast.LENGTH_SHORT).show();
                        }else{
                            releasePlayer();
                            mSVE.trim(mSrcPath, trimmedVideoPath, mStartPositionMs, mEndPositionMs,
                                    mCurrentFilterType, mOnTrimVideoListener);
                        }
                    }
                }
        );

        mTextTimeFrame = (TextView) findViewById(R.id.text_time_selection);
        mRecyclerView = (RecyclerView) findViewById(R.id.video_thumb_recycleview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoThumbAdapter(this,
                (UIUtil.getScreenWidth(context) - UIUtil.dip2px(context, 2*INIT_THUMBVIEW_GAP)) / MAX_THUMBNAIL_COUNT);
        mRecyclerView.setAdapter(mVideoThumbAdapter);
        mRecyclerView.addOnScrollListener(mOnScrollListener);
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        //seekbar最大宽度
        mMaxWidth = UIUtil.getScreenWidth(this) - UIUtil.dip2px(this, 2*INIT_THUMBVIEW_GAP);
        positionIcon = (ImageView) findViewById(R.id.positionIcon);
        seekBarLayout = (LinearLayout) findViewById(R.id.timeLineBar);
    }

    private void initEditVideo() {
        //for video edit
        long startPosition = 0;
        long endPosition = mDuration;
        int thumbnailsCount;
        int rangeWidth;//seekbar可以移动的范围
        boolean isOverMaxCurDur;
        if (endPosition <= MAX_CUT_DURATION) {
            isOverMaxCurDur = false;
            thumbnailsCount = MAX_THUMBNAIL_COUNT;
            rangeWidth = mMaxWidth;
        } else {
            isOverMaxCurDur = true;
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f) * MAX_THUMBNAIL_COUNT);
            rangeWidth = mMaxWidth / MAX_THUMBNAIL_COUNT * thumbnailsCount;
        }
        mRecyclerView.addItemDecoration(new EditSpacingItemDecoration(UIUtil.dip2px(this, INIT_THUMBVIEW_GAP), thumbnailsCount));

        //init seekBar
        if (isOverMaxCurDur) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);
        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }
        seekBar.setMin_cut_time(MIN_CUT_DURATION);//设置最小裁剪时间
        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        seekBarLayout.addView(seekBar);

        Log.d(TAG, "thumbnailsCount:" + thumbnailsCount);
        //每毫秒对应需要移动多少px
        msPerPx = mDuration * 1.0f / rangeWidth * 1.0f;
        Log.d(TAG, "rangeWidth:" + rangeWidth);
        Log.d(TAG, "getDuration:" + mDuration);
        Log.d(TAG, "msPerPx" + msPerPx);
        thumbnailPath = FileUtils.getSaveEditThumbnailDir(this);
        trimmedVideoPath = FileUtils.getSaveTrimVideoDir(this);
        int extractW = (UIUtil.getScreenWidth(this) - UIUtil.dip2px(this, 2*INIT_THUMBVIEW_GAP)) / MAX_THUMBNAIL_COUNT;
        int extractH = UIUtil.dip2px(this, 60);
        mExtractFrameWorkThread = new ExtractFrameWorkThread(extractW, extractH, mUIHandler, mSrcPath, thumbnailPath, startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();

        //init pos icon start
        mStartPositionMs = 0;
        if (isOverMaxCurDur) {
            mEndPositionMs = MAX_CUT_DURATION;
        } else {
            mEndPositionMs = endPosition;
        }
        setTimeFrames();
        //在可见范围内每移动1px对应多少毫秒，其实就是averageMsPx的倒数
        pxPerMs = (mMaxWidth * 1.0f / (mEndPositionMs - mStartPositionMs));
        Log.d(TAG, "pxPerMs" + pxPerMs);
    }

    @Override
    public void onStartTrim() {
        buildDialog(getResources().getString(R.string.trimming)).show();
    }

    @Override
    public void onFinishTrim(String in, FilterType filterType) {
        if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
        Toast.makeText(this, "Trim done!", Toast.LENGTH_SHORT).show();
        mOnTrimVideoListener = null;
        releasePlayer();
        Intent intent = new Intent();
        intent.putExtra("path", in);
        intent.putExtra("filter", filterType.getId());
        setResult(VIDEO_TRIM_RESPONSE_CODE, intent);
        finish();
    }

    @Override
    public void onCancelTrim() {
        mOnTrimVideoListener = null;
        releasePlayer();
        finish();
    }

    @Override
    public void onTrimFail() {
        if (mProgressDialog.isShowing()) mProgressDialog.dismiss();
        Toast.makeText(this, "Trim Fail", Toast.LENGTH_LONG);
    }

    private ProgressDialog buildDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "", msg);
        }
        mProgressDialog.setMessage(msg);
        return mProgressDialog;
    }

    private Handler handler = new Handler();
    private Runnable run = new Runnable() {

        @Override
        public void run() {
            videoProgressUpdate();
            handler.postDelayed(run, 1000);
        }
    };

    private void videoProgressUpdate() {
        if (mPlayer != null) {
            long currentPosition = mPlayer.getCurrentPosition();
            Log.d(TAG, "currentPosition:" + currentPosition);
            if (currentPosition >= (mEndPositionMs)) {
                Log.d(TAG, "reach end of:" + mEndPositionMs + ", seekto:" + (int)mStartPositionMs);
                mPlayer.seekTo((int) mStartPositionMs);
                positionIcon.clearAnimation();
                if (animator != null && animator.isRunning()) {
                    animator.cancel();
                }
                anim();
            }
        }
    }

    private void videoStart() {
        Log.d(TAG, "----videoStart----->>>>>>>");
        mPlayer.start();
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
        handler.removeCallbacks(run);
        handler.post(run);
    }

    private void videoPause() {
        bSeeking = false;
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
            handler.removeCallbacks(run);
        }
        Log.d(TAG, "videoPause");
        if (positionIcon.getVisibility() == View.VISIBLE) {
            positionIcon.setVisibility(View.GONE);
        }
        positionIcon.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    private void setTimeFrames() {
        String seconds = "sec";
        mTextTimeFrame.setText(String.format("%s %s - %s %s", stringForTime(mStartPositionMs), seconds, stringForTime(mEndPositionMs), seconds));
    }

    private static String stringForTime(long timeMs) {
        int totalSeconds = (int)(timeMs / 1000);

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        Formatter mFormatter = new Formatter();
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {
            Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            mStartPositionMs = minValue + scrolledMs;
            mEndPositionMs = maxValue + scrolledMs;
            setTimeFrames();
            Log.d(TAG, "mStartPositionMs:" + mStartPositionMs);
            Log.d(TAG, "mEndPosition" + mEndPositionMs);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "-----ACTION_DOWN---->>>>>>");
                    bSeeking = false;
                    //videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d(TAG, "-----ACTION_MOVE---->>>>>>");
                    bSeeking = true;
                    int seekPos = (int) (pressedThumb == RangeSeekBar.Thumb.MIN ?
                            mStartPositionMs : mEndPositionMs);
                    Log.d(TAG, "seekTo:" + seekPos);
                    mPlayer.seekTo(seekPos);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "-----ACTION_UP--leftProgress--->>>>>>" + mStartPositionMs);
                    bSeeking = false;
                    //从minValue开始播
                    Log.d(TAG, "ACTION_UP seekTo:" + mStartPositionMs);
                    mPlayer.seekTo((int)mStartPositionMs);
                    break;
                default:
                    break;
            }
        }
    };

    private void anim() {
        Log.d(TAG, "--anim--onProgressUpdate---->>>>>>>" + mPlayer.getCurrentPosition());
        if (positionIcon.getVisibility() == View.GONE) {
            positionIcon.setVisibility(View.VISIBLE);
        }
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) positionIcon.getLayoutParams();
        int start = (int) (UIUtil.dip2px(this, INIT_THUMBVIEW_GAP) + (mStartPositionMs/*mVideoView.getCurrentPosition()*/ - scrolledMs) * pxPerMs);
        int end = (int) (UIUtil.dip2px(this, INIT_THUMBVIEW_GAP) + (mEndPositionMs - scrolledMs) * pxPerMs);
        animator = ValueAnimator
                .ofInt(start, end)
                .setDuration((mEndPositionMs - scrolledMs) - (mStartPositionMs/*mVideoView.getCurrentPosition()*/ - scrolledMs));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                positionIcon.setLayoutParams(params);
            }
        });
        animator.start();
    }

    /**
     * 水平滑动了多少px
     *
     * @return int px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "-------newState:>>>>>" + newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                bSeeking = false;
//                videoStart();
            } else {
                bSeeking = true;
                if (bOverScaledTouchSlop && mPlayer != null && mPlayer.isPlaying()) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            bSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                bOverScaledTouchSlop = false;
                return;
            }
            bOverScaledTouchSlop = true;
            Log.d(TAG, "scrollX:" + scrollX);
            // 初始状态的时候有35dp的空白
            if (scrollX == -UIUtil.dip2px(TrimmerActivity.this, INIT_THUMBVIEW_GAP)) {
                scrolledMs = 0;
            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                if (mPlayer != null && mPlayer.isPlaying()) {
                    videoPause();
                }
                bSeeking = true;
                scrolledMs = (long) (msPerPx * (UIUtil.dip2px(TrimmerActivity.this, INIT_THUMBVIEW_GAP) + scrollX));
                Log.d(TAG, "scrolledMs:" + scrolledMs);
                mStartPositionMs = seekBar.getSelectedMinValue() + scrolledMs;
                mEndPositionMs = seekBar.getSelectedMaxValue() + scrolledMs;
                setTimeFrames();
                Log.d(TAG, "mStartPositionMs:" + mStartPositionMs);
                mPlayer.seekTo((int) mStartPositionMs);
                videoStart();
            }
            lastScrollX = scrollX;
        }
    };

    private final MainHandler mUIHandler = new MainHandler(this);

    private static class MainHandler extends Handler {
        private final WeakReference<TrimmerActivity> mActivity;

        MainHandler(TrimmerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TrimmerActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    if (activity.mVideoThumbAdapter != null) {
                        activity.mVideoThumbAdapter.addItemPath((String) msg.obj);
                    }
                }
            }
        }
    }
}
