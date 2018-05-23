package com.greymax.android.sve.timeline;

import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConcatVideoUtil {
  //ffmpeg -f concat -safe 0 -i files.txt -codec copy output.mp4

  public static void concat(Context context, String fileList, String outputDir, final ConcatVideoListener callback) {
    final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    final String outputName = "/outputVideo_" + timeStamp + ".mp4";
    final String outputFile = outputDir + outputName;
    String cmd = "-y -f concat -safe 0 -i " + fileList + " -codec copy " + outputFile;
    String[] command = cmd.split(" ");
    try {
      FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {
        @Override
        public void onFailure(String msg) {
          callback.onConcatFail();
        }

        @Override
        public void onSuccess(String msg) {
          callback.onFinishConcat(msg);
        }

        @Override
        public void onStart() {
          callback.onStartConcat();
        }

        @Override
        public void onFinish() {
        }
      });
    } catch (FFmpegCommandAlreadyRunningException e) {
      e.printStackTrace();
    }
  }
}
