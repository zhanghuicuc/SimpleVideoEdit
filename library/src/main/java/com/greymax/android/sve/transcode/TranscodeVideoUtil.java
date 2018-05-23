/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.greymax.android.sve.transcode;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import com.greymax.android.sve.filters.FilterType;
import com.greymax.android.sve.models.PresetInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * <p>It uses MediaExtractor to get frames from a input stream, decodes them to a surface, uses a
 * shader to edit them, encodes them from the resulting surface, and then uses MediaMuxer to write
 * them into a file.
 *
 * <p>It does not currently check whether the result file is correct, but makes sure that nothing
 * fails along the way.
 *
 */
@TargetApi(21)
public class TranscodeVideoUtil {

    private static final String TAG = TranscodeVideoUtil.class.getSimpleName();
    private static final boolean VERBOSE = false; // lots of logging

    /** How long to wait for the next buffer to become available. */
    private static final int TIMEOUT_USEC = 10000;

    /** Whether to copy the video from the input video. */
    private boolean mCopyVideo;
    /** Whether to copy the audio from the input video. */
    private boolean mCopyAudio;
    /** Whether to verify the audio format. */
    private boolean mVerifyAudioFormat;
    /** Width of the output frames. */
    private int mWidth = -1;
    /** Height of the output frames. */
    private int mHeight = -1;

    /** The input file. */
    private String mSourceRes;

    /** The destination file for the encoded output. */
    private String mOutputFile;

    private TranscodeVideoListener mCallback;

    private FilterType mFilterType;
    private Context mContext;
    private PresetInfo mPresetInfo = new PresetInfo();

    public void transcode(Context context, String inputPath, String outputDir,
                          FilterType filterType, PresetInfo presetInfo,
                          final TranscodeVideoListener callback) {
        if(!setSize(presetInfo.getVideoOutputWidth(), presetInfo.getVideoOutputHeight())) return;
        setSource(inputPath);
        setOutputFile(outputDir);
        setCopyAudio();
        setCopyVideo();
        setVerifyAudioFormat();
        mCallback = callback;
        mFilterType = filterType;
        mContext = context;
        mPresetInfo = presetInfo;
        TranscodeWrapper.runTranscode(this);
    }

    /** Wraps ExtractDecodeEditEncodeMux() */
    private static class TranscodeWrapper implements Runnable {
        private TranscodeVideoUtil mTrans;

        private TranscodeWrapper(TranscodeVideoUtil trans) {
            mTrans = trans;
        }

        @Override
        public void run() {
            mTrans.extractDecodeEditEncodeMux();
        }

        /**
         * Entry point.
         */
        public static void runTranscode(TranscodeVideoUtil trans){
            TranscodeWrapper wrapper = new TranscodeWrapper(trans);
            Thread th = new Thread(wrapper, "Transcode");
            th.start();
            try {
                th.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * copy the video stream.
     */
    private void setCopyVideo() {
        mCopyVideo = true;
    }

    /**
     * copy the video stream.
     */
    private void setCopyAudio() {
        mCopyAudio = true;
    }

    /**
     * verify the output audio format.
     */
    private void setVerifyAudioFormat() {
        mVerifyAudioFormat = true;
    }

    /**
     * Sets the desired frame size and returns whether the given resolution is
     * supported.
     *
     * <p>If decoding/encoding using AVC as the codec, checks that the resolution
     * is supported. For other codecs, always return {@code true}.
     */
    private boolean setSize(int width, int height) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;

        // TODO: remove this logic in setSize as it is now handled when configuring codecs
        return true;
    }

    /**
     * Sets the raw resource used as the source video.
     */
    private void setSource(String filePath) {
        mSourceRes = filePath;
    }

    /**
     * Sets the name of the output file based on the other parameters.
     *
     * <p>Must be called after {@link #setSize(int, int)} and {@link #setSource(String)}.
     */
    private void setOutputFile(String outputDir) {
        StringBuilder sb = new StringBuilder();
        sb.append(outputDir);
        sb.append("/compressed-");
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        sb.append(timeStamp);
        if (mCopyVideo) {
            sb.append('-');
            sb.append("video");
            sb.append('-');
            sb.append(mWidth);
            sb.append('x');
            sb.append(mHeight);
        }
        if (mCopyAudio) {
            sb.append('-');
            sb.append("audio");
        }
        sb.append(".mp4");
        mOutputFile = sb.toString();
    }

    private void extractDecodeEditEncodeMux() {
        mCallback.onStartTranscode();
        // Exception that may be thrown during release.
        Exception exception = null;

        MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);

        // We avoid the device-specific limitations on width and height by using values
        // that are multiples of 16, which all tested devices seem to be able to handle.
        MediaFormat outputVideoFormat =
                MediaFormat.createVideoFormat(mPresetInfo.getVideoOutputMime(), mWidth, mHeight);

        // Set some properties. Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        outputVideoFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT, mPresetInfo.getVideoOuputColorFormat());
        outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mPresetInfo.getVideoOutputBitrate());
        outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mPresetInfo.getVideoOutputFps());
        outputVideoFormat.setInteger(
                MediaFormat.KEY_I_FRAME_INTERVAL, mPresetInfo.getVideoOutputGop());
        if (VERBOSE) Log.d(TAG, "video format: " + outputVideoFormat);

        String videoEncoderName = mcl.findEncoderForFormat(outputVideoFormat);
        if (videoEncoderName == null) {
            // Don't fail CTS if they don't have an AVC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + outputVideoFormat);
            return;
        }
        if (VERBOSE) Log.d(TAG, "video found codec: " + videoEncoderName);

        MediaFormat outputAudioFormat =
                MediaFormat.createAudioFormat(
                        mPresetInfo.getAudioOutputMime(), mPresetInfo.getAudioOutputSr(),
                        mPresetInfo.getAudioOutputChannel());
        outputAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mPresetInfo.getAudioOutputBitrate());
        //outputAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AUDIO_AAC_PROFILE);

        String audioEncoderName = mcl.findEncoderForFormat(outputAudioFormat);
        if (audioEncoderName == null) {
            // Don't fail CTS if they don't have an AAC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + outputAudioFormat);
            return;
        }
        if (VERBOSE) Log.d(TAG, "audio found codec: " + audioEncoderName);

        MediaExtractor videoExtractor = null;
        MediaExtractor audioExtractor = null;
        OutputSurface outputSurface = null;
        MediaCodec videoDecoder = null;
        MediaCodec audioDecoder = null;
        MediaCodec videoEncoder = null;
        MediaCodec audioEncoder = null;
        MediaMuxer muxer = null;

        InputSurface inputSurface = null;

        try {
            if (mCopyVideo) {
                videoExtractor = createExtractor();
                int videoInputTrack = getAndSelectVideoTrackIndex(videoExtractor);
                if (videoInputTrack == -1) {
                    Log.e(TAG, "missing video track in input video");
                }
                MediaFormat inputFormat = videoExtractor.getTrackFormat(videoInputTrack);

                // Create a MediaCodec for the desired codec, then configure it as an encoder with
                // our desired properties. Request a Surface to use for input.
                AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();
                videoEncoder = createVideoEncoder(
                        videoEncoderName, outputVideoFormat, inputSurfaceReference);
                inputSurface = new InputSurface(inputSurfaceReference.get());
                inputSurface.makeCurrent();
                // Create a MediaCodec for the decoder, based on the extractor's format.
                outputSurface = new OutputSurface(FilterType.createGlFilter(mFilterType, mContext));
                videoDecoder = createVideoDecoder(mcl, inputFormat, outputSurface.getSurface());
            }

            audioExtractor = createExtractor();
            int audioInputTrack = getAndSelectAudioTrackIndex(audioExtractor);
            if (mCopyAudio) {
                if (audioInputTrack == -1) {
                    Log.e(TAG, "missing audio track in input video");
                    mCopyAudio = false;
                }
            }
            if (mCopyAudio) {
                MediaFormat inputFormat = audioExtractor.getTrackFormat(audioInputTrack);

                // Create a MediaCodec for the desired codec, then configure it as an encoder with
                // our desired properties. Request a Surface to use for input.
                audioEncoder = createAudioEncoder(audioEncoderName, outputAudioFormat);
                // Create a MediaCodec for the decoder, based on the extractor's format.
                audioDecoder = createAudioDecoder(mcl, inputFormat);
            }

            // Creates a muxer but do not start or add tracks just yet.
            muxer = createMuxer();

            doExtractDecodeEditEncodeMux(
                    videoExtractor,
                    audioExtractor,
                    videoDecoder,
                    videoEncoder,
                    audioDecoder,
                    audioEncoder,
                    muxer,
                    inputSurface,
                    outputSurface);
        } finally {
            if (VERBOSE) Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");
            // Try to release everything we acquired, even if one of the releases fails, in which
            // case we save the first exception we got and re-throw at the end (unless something
            // other exception has already been thrown). This guarantees the first exception thrown
            // is reported as the cause of the error, everything is (attempted) to be released, and
            // all other exceptions appear in the logs.
            try {
                if (videoExtractor != null) {
                    if (VERBOSE) Log.d(TAG, "videoExtractor.release");
                    videoExtractor.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing videoExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (audioExtractor != null) {
                    if (VERBOSE) Log.d(TAG, "audioExtractor.release");
                    audioExtractor.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing audioExtractor", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (videoDecoder != null) {
                    if (VERBOSE) Log.d(TAG, "videoDecoder.stop");
                    videoDecoder.stop();
                    videoDecoder.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing videoDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (outputSurface != null) {
                    if (VERBOSE) Log.d(TAG, "outputSurface.release");
                    outputSurface.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing outputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (videoEncoder != null) {
                    if (VERBOSE) Log.d(TAG, "videoEncoder.stop");
                    videoEncoder.stop();
                    videoEncoder.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing videoEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (audioDecoder != null) {
                    if (VERBOSE) Log.d(TAG, "audioDecoder.stop");
                    audioDecoder.stop();
                    audioDecoder.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing audioDecoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (audioEncoder != null) {
                    if (VERBOSE) Log.d(TAG, "audioEncoder.stop");
                    audioEncoder.stop();
                    audioEncoder.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing audioEncoder", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (muxer != null) {
                    if (VERBOSE) Log.d(TAG, "muxer.stop");
                    muxer.stop();
                    if (VERBOSE) Log.d(TAG, "muxer.release");
                    muxer.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing muxer", e);
                if (exception == null) {
                    exception = e;
                }
            }
            try {
                if (inputSurface != null) {
                    if (VERBOSE) Log.d(TAG, "inputSurface.release");
                    inputSurface.release();
                }
            } catch(Exception e) {
                if (VERBOSE) Log.e(TAG, "error while releasing inputSurface", e);
                if (exception == null) {
                    exception = e;
                }
            }
            if (exception != null) {
                exception.printStackTrace();
            } else {
                Log.d(TAG, "onSuccess");
                mCallback.onFinishTranscode(mOutputFile);
            }
        }
        //wont go here

        MediaExtractor mediaExtractor = null;
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(mOutputFile);

            if ((mCopyAudio ? 1 : 0) + (mCopyVideo ? 1 : 0) != mediaExtractor.getTrackCount()) {
                Log.e(TAG, "incorrect number of tracks");
            }
            if (mVerifyAudioFormat) {
                boolean foundAudio = false;
                for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                    MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                    if (isAudioFormat(trackFormat)) {
                        foundAudio = true;
                        int expectedSampleRate = mPresetInfo.getAudioOutputSr();

                        /* SBR mode halves the sample rate in the format.
                        if (OUTPUT_AUDIO_AAC_PROFILE ==
                                MediaCodecInfo.CodecProfileLevel.AACObjectHE) {
                            expectedSampleRate /= 2;
                        }*/
                        if (expectedSampleRate != trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) {
                            Log.e(TAG, "sample rates should match");
                        }
                    }
                }
                if (foundAudio && !mCopyAudio) {
                    Log.e(TAG, "output should have an audio track");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }
        }
        // TODO: Check the generated output file's video format and sample data.
    }

    /**
     * Creates an extractor that reads its frames from {@link #mSourceRes}.
     */
    private MediaExtractor createExtractor() {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(mSourceRes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractor;
    }

    /**
     * Creates a decoder for the given format, which outputs to the given surface.
     *
     * @param inputFormat the format of the stream to decode
     * @param surface into which to decode the frames
     */
    private MediaCodec createVideoDecoder(
            MediaCodecList mcl, MediaFormat inputFormat, Surface surface){
        try {
            MediaCodec decoder = MediaCodec.createByCodecName(mcl.findDecoderForFormat(inputFormat));
            decoder.configure(inputFormat, surface, null, 0);
            decoder.start();
            return decoder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates an encoder for the given format using the specified codec, taking input from a
     * surface.
     *
     * <p>The surface to use as input is stored in the given reference.
     *
     * @param codecName of the codec to use
     * @param format of the stream to be produced
     * @param surfaceReference to store the surface to use as input
     */
    private MediaCodec createVideoEncoder(
            String codecName,
            MediaFormat format,
            AtomicReference<Surface> surfaceReference) {
        try {
            MediaCodec encoder = MediaCodec.createByCodecName(codecName);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            // Must be called before start() is.
            surfaceReference.set(encoder.createInputSurface());
            encoder.start();
            return encoder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a decoder for the given format.
     *
     * @param inputFormat the format of the stream to decode
     */
    private MediaCodec createAudioDecoder(
            MediaCodecList mcl, MediaFormat inputFormat) {
        try {
            MediaCodec decoder = MediaCodec.createByCodecName("OMX.google.aac.decoder");//mcl.findDecoderForFormat(inputFormat));
            decoder.configure(inputFormat, null, null, 0);
            decoder.start();
            return decoder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates an encoder for the given format using the specified codec.
     *
     * @param codecName of the codec to use
     * @param format of the stream to be produced
     */
    private MediaCodec createAudioEncoder(String codecName, MediaFormat format) {
        try {
            MediaCodec encoder = MediaCodec.createByCodecName(codecName);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            return encoder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a muxer to write the encoded frames.
     *
     * <p>The muxer is not started as it needs to be started only after all streams have been added.
     */
    private MediaMuxer createMuxer() {
        try {
            return new MediaMuxer(mOutputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  null;
    }

    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isVideoFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
        for (int index = 0; index < extractor.getTrackCount(); ++index) {
            if (VERBOSE) {
                Log.d(TAG, "format for track " + index + " is "
                        + getMimeTypeFor(extractor.getTrackFormat(index)));
            }
            if (isAudioFormat(extractor.getTrackFormat(index))) {
                extractor.selectTrack(index);
                return index;
            }
        }
        return -1;
    }

    /**
     * Does the actual work for extracting, decoding, encoding and muxing.
     */
    private void doExtractDecodeEditEncodeMux(
            MediaExtractor videoExtractor,
            MediaExtractor audioExtractor,
            MediaCodec videoDecoder,
            MediaCodec videoEncoder,
            MediaCodec audioDecoder,
            MediaCodec audioEncoder,
            MediaMuxer muxer,
            InputSurface inputSurface,
            OutputSurface outputSurface) {
        ByteBuffer[] videoDecoderInputBuffers = null;
        ByteBuffer[] videoDecoderOutputBuffers = null;
        ByteBuffer[] videoEncoderOutputBuffers = null;
        MediaCodec.BufferInfo videoDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo videoEncoderOutputBufferInfo = null;
        if (mCopyVideo) {
            videoDecoderInputBuffers = videoDecoder.getInputBuffers();
            videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
            videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
            videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        }
        ByteBuffer[] audioDecoderInputBuffers = null;
        ByteBuffer[] audioDecoderOutputBuffers = null;
        ByteBuffer[] audioEncoderInputBuffers = null;
        ByteBuffer[] audioEncoderOutputBuffers = null;
        MediaCodec.BufferInfo audioDecoderOutputBufferInfo = null;
        MediaCodec.BufferInfo audioEncoderOutputBufferInfo = null;
        if (mCopyAudio) {
            audioDecoderInputBuffers = audioDecoder.getInputBuffers();
            audioDecoderOutputBuffers =  audioDecoder.getOutputBuffers();
            audioEncoderInputBuffers = audioEncoder.getInputBuffers();
            audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
            audioDecoderOutputBufferInfo = new MediaCodec.BufferInfo();
            audioEncoderOutputBufferInfo = new MediaCodec.BufferInfo();
        }
        // We will get these from the decoders when notified of a format change.
        MediaFormat decoderOutputVideoFormat = null;
        MediaFormat decoderOutputAudioFormat = null;
        // We will get these from the encoders when notified of a format change.
        MediaFormat encoderOutputVideoFormat = null;
        MediaFormat encoderOutputAudioFormat = null;
        // We will determine these once we have the output format.
        int outputVideoTrack = -1;
        int outputAudioTrack = -1;
        // Whether things are done on the video side.
        boolean videoExtractorDone = false;
        boolean videoDecoderDone = false;
        boolean videoEncoderDone = false;
        // Whether things are done on the audio side.
        boolean audioExtractorDone = false;
        boolean audioDecoderDone = false;
        boolean audioEncoderDone = false;
        // The audio decoder output buffer to process, -1 if none.
        int pendingAudioDecoderOutputBufferIndex = -1;

        boolean muxing = false;

        int videoExtractedFrameCount = 0;
        int videoDecodedFrameCount = 0;
        int videoEncodedFrameCount = 0;

        int audioExtractedFrameCount = 0;
        int audioDecodedFrameCount = 0;
        int audioEncodedFrameCount = 0;

        //手机上seekTo找关键帧不准，938上面找的是准的

        //最外层大循环
        while ((mCopyVideo && !videoEncoderDone) || (mCopyAudio && !audioEncoderDone)) {
            if (VERBOSE) {
                Log.d(TAG, String.format(
                        "loop: "

                                + "V(%b){"
                                + "extracted:%d(done:%b) "
                                + "decoded:%d(done:%b) "
                                + "encoded:%d(done:%b)} "

                                + "A(%b){"
                                + "extracted:%d(done:%b) "
                                + "decoded:%d(done:%b) "
                                + "encoded:%d(done:%b) "
                                + "pending:%d} "

                                + "muxing:%b(V:%d,A:%d)",

                        mCopyVideo,
                        videoExtractedFrameCount, videoExtractorDone,
                        videoDecodedFrameCount, videoDecoderDone,
                        videoEncodedFrameCount, videoEncoderDone,

                        mCopyAudio,
                        audioExtractedFrameCount, audioExtractorDone,
                        audioDecodedFrameCount, audioDecoderDone,
                        audioEncodedFrameCount, audioEncoderDone,
                        pendingAudioDecoderOutputBufferIndex,

                        muxing, outputVideoTrack, outputAudioTrack));
            }

            // Extract video from file and feed to decoder.  parse一帧并送给decoder
            // Do not extract video if we have determined the output format but we are not yet
            // ready to mux the frames.
            // 如果还没拿到encoderOutputVideoFormat，则extract
            // 如果拿到了encoderOuputVideoFormat，但是muxing还是false，则不要extract
            while (mCopyVideo && !videoExtractorDone
                    && (encoderOutputVideoFormat == null || muxing)) {
                int decoderInputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no video decoder input buffer");
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned input buffer: " + decoderInputBufferIndex);
                }
                ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
                int size = videoExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = videoExtractor.getSampleTime();
                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    //Log.d(TAG, "video extractor sync frame");
                }
                if (VERBOSE) {
                    Log.d(TAG, "video extractor: returned buffer of size " + size);
                    Log.d(TAG, "video extractor: returned buffer for time " + presentationTime);
                }
                if (size >= 0) {
                    videoDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            videoExtractor.getSampleFlags());
                }
                videoExtractorDone = !videoExtractor.advance();
                if (videoExtractorDone) {
                    if (VERBOSE) Log.d(TAG, "video extractor: EOS");
                    videoDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                videoExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            // Extract audio from file and feed to decoder.
            // Do not extract audio if we have determined the output format but we are not yet
            // ready to mux the frames.
            while (mCopyAudio && !audioExtractorDone
                    && (encoderOutputAudioFormat == null || muxing)) {
                int decoderInputBufferIndex = audioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio decoder input buffer");
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned input buffer: " + decoderInputBufferIndex);
                }
                ByteBuffer decoderInputBuffer = audioDecoderInputBuffers[decoderInputBufferIndex];
                int size = audioExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = audioExtractor.getSampleTime();
                if (VERBOSE) {
                    Log.d(TAG, "audio extractor: returned buffer of size " + size);
                    Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
                }
                if (size >= 0) {
                    audioDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            audioExtractor.getSampleFlags());
                }
                audioExtractorDone = !audioExtractor.advance();
                if (audioExtractorDone) {
                    if (VERBOSE) Log.d(TAG, "audio extractor: EOS");
                    audioDecoder.queueInputBuffer(
                            decoderInputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                audioExtractedFrameCount++;
                // We extracted a frame, let's try something else next.
                break;
            }

            // Poll output frames from the video decoder and feed the encoder.
            // 把解码后的video数据帧送给encoder
            while (mCopyVideo && !videoDecoderDone
                    && (encoderOutputVideoFormat == null || muxing)) {
                int decoderOutputBufferIndex =
                        videoDecoder.dequeueOutputBuffer(
                                videoDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no video decoder output buffer");
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "video decoder: output buffers changed");
                    videoDecoderOutputBuffers = videoDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputVideoFormat = videoDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "video decoder: output format changed: "
                                + decoderOutputVideoFormat);
                    }
                    //outputSurface.resolutionChanged(decoderOutputVideoFormat.getInteger(MediaFormat.KEY_WIDTH),
                    //        decoderOutputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT));
                    outputSurface.resolutionChanged(mWidth, mHeight);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned output buffer: "
                            + decoderOutputBufferIndex);
                    Log.d(TAG, "video decoder: returned buffer of size "
                            + videoDecoderOutputBufferInfo.size);
                }
                ByteBuffer decoderOutputBuffer =
                        videoDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: codec config buffer");
                    videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video decoder: returned buffer for time "
                            + videoDecoderOutputBufferInfo.presentationTimeUs);
                }
                boolean render = videoDecoderOutputBufferInfo.size != 0;
                videoDecoder.releaseOutputBuffer(decoderOutputBufferIndex, render);
                if (render) {
                    if (VERBOSE) Log.d(TAG, "output surface: await new image");
                    outputSurface.awaitNewImage();
                    // Edit the frame and send it to the encoder.
                    if (VERBOSE) Log.d(TAG, "output surface: draw image");
                    outputSurface.drawImage();
                    inputSurface.setPresentationTime(
                            videoDecoderOutputBufferInfo.presentationTimeUs * 1000);
                    if (VERBOSE) Log.d(TAG, "input surface: swap buffers");
                    inputSurface.swapBuffers();
                    if (VERBOSE) Log.d(TAG, "video encoder: notified of new frame");
                }
                if ((videoDecoderOutputBufferInfo.flags
                        & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "video decoder: EOS");
                    videoDecoderDone = true;
                    videoEncoder.signalEndOfInputStream();
                }
                videoDecodedFrameCount++;
                // We extracted a pending frame, let's try something else next.
                break;
            }

            // Poll output frames from the audio decoder.
            //解码音频帧
            // Do not poll if we already have a pending buffer to feed to the encoder.
            while (mCopyAudio && !audioDecoderDone && pendingAudioDecoderOutputBufferIndex == -1
                    && (encoderOutputAudioFormat == null || muxing)) {
                int decoderOutputBufferIndex =
                        audioDecoder.dequeueOutputBuffer(
                                audioDecoderOutputBufferInfo, TIMEOUT_USEC);
                if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio decoder output buffer");
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: output buffers changed");
                    audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                    break;
                }
                if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    decoderOutputAudioFormat = audioDecoder.getOutputFormat();
                    if (VERBOSE) {
                        Log.d(TAG, "audio decoder: output format changed: "
                                + decoderOutputAudioFormat);
                    }
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned output buffer: "
                            + decoderOutputBufferIndex);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer of size "
                            + audioDecoderOutputBufferInfo.size);
                }
                ByteBuffer decoderOutputBuffer =
                        audioDecoderOutputBuffers[decoderOutputBufferIndex];
                if ((audioDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: codec config buffer");
                    audioDecoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: returned buffer for time "
                            + audioDecoderOutputBufferInfo.presentationTimeUs);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: output buffer is now pending: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                pendingAudioDecoderOutputBufferIndex = decoderOutputBufferIndex;
                audioDecodedFrameCount++;
                // We extracted a pending frame, let's try something else next.
                break;
            }

            // Feed the pending decoded audio buffer to the audio encoder.
            //把解码后的音频帧送给编码器
            while (mCopyAudio && pendingAudioDecoderOutputBufferIndex != -1) {
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: attempting to process pending buffer: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                int encoderInputBufferIndex = audioEncoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (encoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio encoder input buffer");
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned input buffer: " + encoderInputBufferIndex);
                }
                ByteBuffer encoderInputBuffer = audioEncoderInputBuffers[encoderInputBufferIndex];
                int size = audioDecoderOutputBufferInfo.size;
                long presentationTime = audioDecoderOutputBufferInfo.presentationTimeUs;
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: processing pending buffer: "
                            + pendingAudioDecoderOutputBufferIndex);
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder: pending buffer of size " + size);
                    Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);
                }
                if (size >= 0) {
                    ByteBuffer decoderOutputBuffer =
                            audioDecoderOutputBuffers[pendingAudioDecoderOutputBufferIndex]
                                    .duplicate();
                    decoderOutputBuffer.position(audioDecoderOutputBufferInfo.offset);
                    decoderOutputBuffer.limit(audioDecoderOutputBufferInfo.offset + size);
                    encoderInputBuffer.position(0);
                    //这里会有一次arraycopy,但是只是audio的，所以还ok
                    encoderInputBuffer.put(decoderOutputBuffer);

                    audioEncoder.queueInputBuffer(
                            encoderInputBufferIndex,
                            0,
                            size,
                            presentationTime,
                            audioDecoderOutputBufferInfo.flags);
                }
                audioDecoder.releaseOutputBuffer(pendingAudioDecoderOutputBufferIndex, false);
                pendingAudioDecoderOutputBufferIndex = -1;
                if ((audioDecoderOutputBufferInfo.flags
                        & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (VERBOSE) Log.d(TAG, "audio decoder: EOS");
                    audioDecoderDone = true;
                }
                // We enqueued a pending frame, let's try something else next.
                break;
            }

            // Poll frames from the video encoder and send them to the muxer.
            //从video encoder取出编码后的帧送给mux
            while (mCopyVideo && !videoEncoderDone
                    && (encoderOutputVideoFormat == null || muxing)) {
                int encoderOutputBufferIndex = videoEncoder.dequeueOutputBuffer(
                        videoEncoderOutputBufferInfo, TIMEOUT_USEC);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no video encoder output buffer");
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "video encoder: output buffers changed");
                    videoEncoderOutputBuffers = videoEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "video encoder: output format changed");
                    if (outputVideoTrack >= 0) {
                        Log.e(TAG, "video encoder changed its output format again?");
                    }
                    encoderOutputVideoFormat = videoEncoder.getOutputFormat();
                    break;
                }
                if (!muxing) {
                    Log.e(TAG, "should have added track before processing output");
                }

                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned output buffer: "
                            + encoderOutputBufferIndex);
                    Log.d(TAG, "video encoder: returned buffer of size "
                            + videoEncoderOutputBufferInfo.size);
                }
                ByteBuffer encoderOutputBuffer =
                        videoEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "video encoder: codec config buffer");
                    // Simply ignore codec config buffers.
                    videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "video encoder: returned buffer for time "
                            + videoEncoderOutputBufferInfo.presentationTimeUs);
                }
                if (videoEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(
                            outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
                }
                if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "video encoder: EOS");
                    videoEncoderDone = true;
                }
                videoEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                videoEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }

            // Poll frames from the audio encoder and send them to the muxer.
            //取出audio encoder编码后的帧送给mux
            while (mCopyAudio && !audioEncoderDone
                    && (encoderOutputAudioFormat == null || muxing)) {
                int encoderOutputBufferIndex = audioEncoder.dequeueOutputBuffer(
                        audioEncoderOutputBufferInfo, TIMEOUT_USEC);
                if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (VERBOSE) Log.d(TAG, "no audio encoder output buffer");
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: output buffers changed");
                    audioEncoderOutputBuffers = audioEncoder.getOutputBuffers();
                    break;
                }
                if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: output format changed");
                    if (outputAudioTrack >= 0) {
                        Log.e(TAG, "audio encoder changed its output format again?");
                    }

                    encoderOutputAudioFormat = audioEncoder.getOutputFormat();
                    break;
                }
                if (!muxing) {
                    Log.e(TAG, "should have added track before processing output");
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned output buffer: "
                            + encoderOutputBufferIndex);
                    Log.d(TAG, "audio encoder: returned buffer of size "
                            + audioEncoderOutputBufferInfo.size);
                }
                ByteBuffer encoderOutputBuffer =
                        audioEncoderOutputBuffers[encoderOutputBufferIndex];
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: codec config buffer");
                    // Simply ignore codec config buffers.
                    audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                    break;
                }
                if (VERBOSE) {
                    Log.d(TAG, "audio encoder: returned buffer for time "
                            + audioEncoderOutputBufferInfo.presentationTimeUs);
                }
                if (audioEncoderOutputBufferInfo.size != 0) {
                    muxer.writeSampleData(
                            outputAudioTrack, encoderOutputBuffer, audioEncoderOutputBufferInfo);
                }
                if ((audioEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        != 0) {
                    if (VERBOSE) Log.d(TAG, "audio encoder: EOS");
                    audioEncoderDone = true;
                }
                audioEncoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
                audioEncodedFrameCount++;
                // We enqueued an encoded frame, let's try something else next.
                break;
            }

            if (!muxing
                    && (!mCopyAudio || encoderOutputAudioFormat != null)
                    && (!mCopyVideo || encoderOutputVideoFormat != null)) {
                if (mCopyVideo) {
                    Log.d(TAG, "muxer: adding video track.");
                    outputVideoTrack = muxer.addTrack(encoderOutputVideoFormat);
                }
                if (mCopyAudio) {
                    Log.d(TAG, "muxer: adding audio track.");
                    outputAudioTrack = muxer.addTrack(encoderOutputAudioFormat);
                }
                Log.d(TAG, "muxer: starting");
                muxer.start();
                muxing = true;
            }
        }

        // Basic sanity checks.
        if (mCopyVideo) {
            if (videoDecodedFrameCount != videoEncodedFrameCount) {
                Log.e(TAG, "encoded and decoded video frame counts should match");
            }
            if (videoDecodedFrameCount > videoExtractedFrameCount) {
                Log.e(TAG, "decoded frame count should be less than extracted frame count");
            }
        }
        if (mCopyAudio) {
            if (pendingAudioDecoderOutputBufferIndex != -1) {
                Log.e(TAG, "no frame should be pending");
            }
        }
    }

    private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }

    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }
}
