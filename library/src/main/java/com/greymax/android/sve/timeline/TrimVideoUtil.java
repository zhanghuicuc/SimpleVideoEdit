package com.greymax.android.sve.timeline;

import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.greymax.android.sve.filters.FilterType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrimVideoUtil {

    private static final String TAG = TrimVideoUtil.class.getSimpleName();

    public static void trim(final Context context, String inputFile, String outputDir, long startMs, long endMs,
                            final FilterType filterType, final TrimVideoListener callback) {
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        final String outputName = "/trimmedVideo_" + timeStamp + ".mp4";
        final String outputFile = outputDir + outputName;

        String start = convertSecondsToTime(startMs / 1000);
        String duration = convertSecondsToTime((endMs - startMs) / 1000);

        /** 裁剪视频ffmpeg指令说明：
         * ffmpeg -ss START -t DURATION -i INPUT -vcodec copy -acodec copy OUTPUT
         -ss 开始时间，如： 00:00:20，表示从20秒开始；
         -t 时长，如： 00:00:10，表示截取10秒长的视频；
         -i 输入，后面是空格，紧跟着就是输入视频文件；
         -vcodec copy 和 -acodec copy 表示所要使用的视频和音频的编码格式，这里指定为copy表示原样拷贝；
         INPUT，输入视频文件；
         OUTPUT，输出视频文件
        */
        String cmd = "-y -ss " + start + " -t " + duration + " -i " + inputFile + " -vcodec copy -acodec copy " + outputFile;
        String[] command = cmd.split(" ");
        try {
            final String tempOutFile = outputFile;
            FFmpeg.getInstance(context).execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    callback.onTrimFail();
                }

                @Override
                public void onSuccess(String s) {
                    callback.onFinishTrim(tempOutFile, filterType);
                }

                @Override
                public void onStart() {
                    callback.onStartTrim();
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0)
            return "00:00";
        else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    private static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }
}
