package com.greymax.android.sve.app.utils;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;


public class VideoInfoLoader extends AsyncTaskLoader<ArrayList<VideoInfo>> {
    private static final String[] VIDEO_PROJECTION =
            new String[] {
                    MediaStore.Video.VideoColumns.DURATION,
                    MediaStore.Video.VideoColumns.DATA,
                    MediaStore.Video.VideoColumns.DATE_ADDED,
                    MediaStore.Video.VideoColumns.DISPLAY_NAME,
            };

    private ArrayList<VideoInfo> cached;
    private boolean observerRegistered = false;
    private final ForceLoadContentObserver forceLoadContentObserver = new ForceLoadContentObserver();

    public VideoInfoLoader(Context context) {
        super(context);
    }

    @Override
    public void deliverResult(ArrayList<VideoInfo> data) {
        if (!isReset() && isStarted()) {
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cached != null) {
            deliverResult(cached);
        }
        if (takeContentChanged() || cached == null) {
            forceLoad();
        }
        registerContentObserver();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();

        onStopLoading();
        cached = null;
        unregisterContentObserver();
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();
        unregisterContentObserver();
    }

    @Override
    public ArrayList<VideoInfo> loadInBackground() {
        ArrayList<VideoInfo> data = queryVideos();
        return data;
    }

    private ArrayList<VideoInfo> queryVideos() {
        return query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, VIDEO_PROJECTION,
                MediaStore.Video.VideoColumns.DATE_MODIFIED, MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.DATA, MediaStore.Video.VideoColumns.DATE_ADDED,
                MediaStore.Video.VideoColumns.DISPLAY_NAME);
    }

    private ArrayList<VideoInfo> query(Uri contentUri, String[] projection, String sortByCol,
                                       String durationCol, String dataCol, String dateAddedCol, String nameCol) {
        final ArrayList<VideoInfo> data = new ArrayList<VideoInfo>();
        Cursor cursor = getContext().getContentResolver()
                .query(contentUri, projection, null, null, sortByCol + " DESC");

        if (cursor == null) {
            return data;
        }

        try {
            while (cursor.moveToNext()) {
                VideoInfo video = new VideoInfo();
                if (cursor.getLong(cursor.getColumnIndex(durationCol)) != 0) {
                    video.setDuration(cursor.getLong(cursor.getColumnIndex(durationCol)));
                    video.setVideoPath(cursor.getString(cursor.getColumnIndex(dataCol)));
                    video.setCreateTime(cursor.getString(cursor.getColumnIndex(dateAddedCol)));
                    video.setVideoName(cursor.getString(cursor.getColumnIndex(nameCol)));
                    data.add(video);
                }
            }
        } finally {
            cursor.close();
        }

        return data;
    }

    private void registerContentObserver() {
        if (!observerRegistered) {
            ContentResolver cr = getContext().getContentResolver();
            cr.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, false,
                    forceLoadContentObserver);

            observerRegistered = true;
        }
    }

    private void unregisterContentObserver() {
        if (observerRegistered) {
            observerRegistered = false;

            getContext().getContentResolver().unregisterContentObserver(forceLoadContentObserver);
        }
    }
}
