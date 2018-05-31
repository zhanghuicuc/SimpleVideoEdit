package com.greymax.android.sve;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.greymax.android.sve.transcode.TranscodeVideoListener;
import com.greymax.android.sve.transcode.TranscodeVideoUtil;
import com.greymax.android.sve.filters.FilterType;
import com.greymax.android.sve.models.ClipInfo;
import com.greymax.android.sve.models.PresetInfo;
import com.greymax.android.sve.timeline.ConcatVideoListener;
import com.greymax.android.sve.timeline.ConcatVideoUtil;
import com.greymax.android.sve.timeline.TrimVideoListener;
import com.greymax.android.sve.timeline.TrimVideoUtil;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class SVE {

    private static final String TAG = SVE.class.getSimpleName();
    private static SVE mSVE = null;
    private static volatile boolean bInited = false;
    private ArrayList<ClipInfo> mInputClips;
    private ArrayList<String> mOutputClips;
    private Context mContext;
    private TranscodeVideoUtil mTranscodeUtil;
    private PresetInfo mPresetInfo;

    /**
     * 初始化SVE
     * @param context
     * @return 单例的SVE
     */
    public static SVE getSVE(Context context) {
        synchronized (SVE.class) {
            if (!bInited) {
                mSVE = new SVE();
                mSVE.mInputClips = new ArrayList<>();
                mSVE.mOutputClips = new ArrayList<>();
                mSVE.mContext = context;
                initFFmpegBinary(context);
                mSVE.mTranscodeUtil = new TranscodeVideoUtil();
                mSVE.mPresetInfo = new PresetInfo();
                bInited = true;
            }
            return mSVE;
        }
    }

    /**
     * 销毁SVE
     */
    public static void releaseSVE() {
        synchronized (SVE.class) {
            mSVE.mInputClips = null;
            mSVE.mOutputClips = null;
            mSVE.mTranscodeUtil = null;
            mSVE.mPresetInfo = null;
            mSVE.mContext = null;
            mSVE = null;

            bInited = false;
        }
    }

    /**
     * 对源视频做时间轴上的裁剪
     * @param srcPath 源视频路径
     * @param dstDir 裁剪后视频的保存目录
     * @param startPosMs 起点时间（毫秒）
     * @param endPosMs 终点时间（毫秒）
     * @param filterType 滤镜类型
     * @param listener
     */
    public void trim(String srcPath, String dstDir, long startPosMs, long endPosMs, FilterType filterType, TrimVideoListener listener) {
        TrimVideoUtil.trim(mContext, srcPath, dstDir, startPosMs, endPosMs, filterType, listener);
    }

    /**
     * 添加一个输入Clip
     * Clip是经过裁剪后的原始视频
     * @param clipPath Clip路径
     * @param clipFilter Clip滤镜
     * @param clipIndex Clip索引
     */
    public void addOneInputClip(String clipPath, int clipFilter, int clipIndex) {
        if (mInputClips != null) {
            if (mInputClips.size() >= (clipIndex + 1)
                    && mInputClips.get(clipIndex) != null) {
                mInputClips.set(clipIndex, ClipInfo.buildClip(clipPath, FilterType.fromInteger(clipFilter), clipIndex));
            } else {
                mInputClips.add(ClipInfo.buildClip(clipPath, FilterType.fromInteger(clipFilter), clipIndex));
            }
        }
    }

    /**
     * 获得所有输入Clip
     * @return Clip列表
     */
    public ArrayList<ClipInfo> getAllInputClips() {
        return mInputClips;
    }

    /**
     * 设置转码预设
     * @param videoMime 目标视频编码
     * @param videoWidth 目标视频宽
     * @param videoHeight 目标视频高
     * @param videoBitrate 目标视频码率
     * @param videoFps 目标视频帧率
     * @param videoGop 目标视频GOP长度
     * @param audioMime 目标音频编码
     * @param audioBitrate 目标音频码率
     * @param audioChannel 目标音频声道数
     * @param audioSampleRate 目标音频采样率
     */
    public void setPresetInfo(String videoMime, int videoWidth, int videoHeight,
                              int videoBitrate, int videoFps, int videoGop,
                              String audioMime, int audioBitrate, int audioChannel, int audioSampleRate) {
        if (mPresetInfo != null) {
            if (!TextUtils.isEmpty(videoMime)) mPresetInfo.setVideoOutputMime(videoMime);
            if (videoWidth >= 0) mPresetInfo.setVideoOutputWidth(videoWidth);
            if (videoHeight >= 0) mPresetInfo.setVideoOutputHeight(videoHeight);
            if (videoBitrate >= 0) mPresetInfo.setVideoOutputBitrate(videoBitrate);
            if (videoFps >= 0) mPresetInfo.setVideoOutputFps(videoFps);
            if (videoGop >= 0) mPresetInfo.setVideoOutputGop(videoGop);
            if (!TextUtils.isEmpty(audioMime)) mPresetInfo.setAudioOutputMime(audioMime);
            if (audioBitrate >= 0) mPresetInfo.setAudioOutputBitrate(audioBitrate);
            if (audioChannel >= 0) mPresetInfo.setAudioOutputChannel(audioChannel);
            if (audioSampleRate >= 0) mPresetInfo.setAudioOutputSr(audioSampleRate);
        }
    }

    /**
     * 对输入Clip转码， 得到输出Clip
     * @param clipInfo 输入Clip的信息
     * @param listener
     * @param outputDir 输出Clip的保存目录
     */
    public void transcodeClip(ClipInfo clipInfo, TranscodeVideoListener listener, String outputDir) {
        new TranscodeTask(listener, clipInfo.getClipPath(), clipInfo.getClipfilter(), outputDir).execute();
    }

    /**
     * 添加一个输出Clip
     * 输出Clip是转码后的Clip
     * @param clipPath Clip路径
     */
    public void addOneOutputClip(String clipPath) {
        if (mOutputClips != null) {
            mOutputClips.add(clipPath);
        }
    }

    /**
     * 获得所有输出Clip的地址
     * @return 输出Clip的地址列表
     */
    public ArrayList<String> getAllOutputClips() {
        return mOutputClips;
    }

    /**
     * 拼接输出Clip，获得最终输出视频
     * @param outputDir 输出视频保存目录
     * @param listener
     */
    public void concatForOutput(String outputDir, ConcatVideoListener listener) {
        try {
            if (mOutputClips != null) {
                String fileList = "/sdcard/files.txt";
                PrintWriter writer = new PrintWriter(fileList, "UTF-8");
                for (int i = 0; i < mOutputClips.size(); i++) {
                    writer.println("file " + mOutputClips.get(i));
                }
                writer.close();
                ConcatVideoUtil.concat(mContext, fileList, outputDir, listener);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static class TranscodeTask extends AsyncTask<String, Integer, String> {
        private final WeakReference<TranscodeVideoListener> compressVideoListenerWeakReference;
        private String clipPath;
        private FilterType filterType;
        private String outputDir;

        TranscodeTask(TranscodeVideoListener listener, String clipPath,
                      FilterType filterType, String outputDir) {
            this.compressVideoListenerWeakReference = new WeakReference<TranscodeVideoListener>(listener);
            this.clipPath = clipPath;
            this.filterType = filterType;
            this.outputDir = outputDir;
        }

        @Override
        protected String doInBackground(String... urls) {
            if (mSVE.mTranscodeUtil != null) {
                mSVE.mTranscodeUtil.transcode(mSVE.mContext, clipPath, outputDir,
                        filterType, mSVE.mPresetInfo, compressVideoListenerWeakReference.get());
            }
            return "";
        }

        @Override
        protected void onPostExecute(String t) {
        }
    }

    private static void initFFmpegBinary(Context context) {

        try {
            FFmpeg.getInstance(context).loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Log.e(TAG, "initFFmpegBinary Fail!");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            e.printStackTrace();
        }
    }

}
