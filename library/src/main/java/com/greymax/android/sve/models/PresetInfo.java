package com.greymax.android.sve.models;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.Serializable;


public class PresetInfo implements Serializable{
    // presets for the video encoder
    private static final String DEFAULT_OUTPUT_VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int DEFAULT_OUTPUT_VIDEO_WIDTH = 1280;
    private static final int DEFAULT_OUTPUT_VIDEO_HEIGHT = 720;
    private static final int DEFAULT_OUTPUT_VIDEO_BIT_RATE = 1000000;   // 1Mbps
    private static final int DEFAULT_OUTPUT_VIDEO_FRAME_RATE = 25;      // 25fps
    private static final int DEFAULT_OUTPUT_VIDEO_IFRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int DEFAULT_OUTPUT_VIDEO_COLOR_FORMAT =
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

    // presets for the audio encoder
    private static final String DEFAULT_OUTPUT_AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int DEFAULT_OUTPUT_AUDIO_CHANNEL_COUNT = 2;    // Must match the input stream.
    private static final int DEFAULT_OUTPUT_AUDIO_BIT_RATE = 128 * 1024; // 128kbps
    private static final int DEFAULT_OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100; // Must match the input stream.

    private String videoOutputMime = DEFAULT_OUTPUT_VIDEO_MIME_TYPE;
    private int videoOutputWidth = DEFAULT_OUTPUT_VIDEO_WIDTH;
    private int videoOutputHeight = DEFAULT_OUTPUT_VIDEO_HEIGHT;
    private int videoOutputBitrate = DEFAULT_OUTPUT_VIDEO_BIT_RATE;
    private int videoOutputFps = DEFAULT_OUTPUT_VIDEO_FRAME_RATE;
    private int videoOutputGop = DEFAULT_OUTPUT_VIDEO_IFRAME_INTERVAL;
    private int videoOuputColorFormat = DEFAULT_OUTPUT_VIDEO_COLOR_FORMAT;

    private String audioOutputMime = DEFAULT_OUTPUT_AUDIO_MIME_TYPE;
    private int audioOutputChannel = DEFAULT_OUTPUT_AUDIO_CHANNEL_COUNT;
    private int audioOutputBitrate = DEFAULT_OUTPUT_AUDIO_BIT_RATE;
    private int audioOutputSr = DEFAULT_OUTPUT_AUDIO_SAMPLE_RATE_HZ;

    public String getVideoOutputMime() {
        return videoOutputMime;
    }

    public void setVideoOutputMime(String videoOutputMime) {
        this.videoOutputMime = videoOutputMime;
    }

    public int getVideoOutputWidth() {
        return videoOutputWidth;
    }

    public void setVideoOutputWidth(int videoOutputWidth) {
        this.videoOutputWidth = videoOutputWidth;
    }

    public int getVideoOutputHeight() {
        return videoOutputHeight;
    }

    public void setVideoOutputHeight(int videoOutputHeight) {
        this.videoOutputHeight = videoOutputHeight;
    }

    public int getVideoOutputBitrate() {
        return videoOutputBitrate;
    }

    public void setVideoOutputBitrate(int videoOutputBitrate) {
        this.videoOutputBitrate = videoOutputBitrate;
    }

    public int getVideoOutputFps() {
        return videoOutputFps;
    }

    public void setVideoOutputFps(int videoOutputFps) {
        this.videoOutputFps = videoOutputFps;
    }

    public int getVideoOutputGop() {
        return videoOutputGop;
    }

    public void setVideoOutputGop(int videoOutputGop) {
        this.videoOutputGop = videoOutputGop;
    }

    public int getVideoOuputColorFormat() {
        return videoOuputColorFormat;
    }

    public void setVideoOuputColorFormat(int videoOuputColorFormat) {
        this.videoOuputColorFormat = videoOuputColorFormat;
    }

    public String getAudioOutputMime() {
        return audioOutputMime;
    }

    public void setAudioOutputMime(String audioOutputMimie) {
        this.audioOutputMime = audioOutputMimie;
    }

    public int getAudioOutputChannel() {
        return audioOutputChannel;
    }

    public void setAudioOutputChannel(int audioOutputChannel) {
        this.audioOutputChannel = audioOutputChannel;
    }

    public int getAudioOutputBitrate() {
        return audioOutputBitrate;
    }

    public void setAudioOutputBitrate(int audioOutputBitrate) {
        this.audioOutputBitrate = audioOutputBitrate;
    }

    public int getAudioOutputSr() {
        return audioOutputSr;
    }

    public void setAudioOutputSr(int audioOutputSr) {
        this.audioOutputSr = audioOutputSr;
    }

}
