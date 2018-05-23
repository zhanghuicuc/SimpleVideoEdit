package com.greymax.android.sve.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.greymax.android.sve.app.utils.UIUtil;
import com.greymax.android.sve.app.utils.VideoInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class VideoGridViewAdapter extends RecyclerView.Adapter<VideoGridViewAdapter.VideoViewHolder>
        implements ListPreloader.PreloadSizeProvider<VideoInfo>, ListPreloader.PreloadModelProvider<VideoInfo>{

    private ArrayList<VideoInfo> videoListData;
    private Context context;
    private ItemClickCallback<Boolean, VideoInfo> mItemClickCallback = null;
    //private ItemSelectCallback itemSelectCallback = null;
    private ArrayList<VideoInfo> videoSelect = new ArrayList<>();
    private ArrayList<ImageView> selectIconList = new ArrayList<>();
    private final RequestBuilder<Drawable> requestBuilder;
    private int[] actualDimensions;
    //private int selectedPos = 0;

    public interface ItemClickCallback<T,V> {
        void onItemClickCallback(T t, V v);
    }

    /*public interface ItemSelectCallback {
        void onItemSelectCallback(int position);
    }*/

    VideoGridViewAdapter(Context context, ArrayList<VideoInfo> dataList, RequestManager requestManager) {
        this.context = context;
        this.videoListData = dataList;
        this.requestBuilder = requestManager.asDrawable();
        /*this.setItemSelectCallback(new VideoGridViewAdapter.ItemSelectCallback(){
            @Override
            public void onItemSelectCallback(int position) {
                notifyItemChanged(selectedPos);
                selectedPos = position;
                notifyItemChanged(selectedPos);//刷新当前点击item
            }
        });*/
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final View view = inflater.inflate(R.layout.video_select_gridview_item, null);

        if (actualDimensions == null) {
            view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (actualDimensions == null) {
                        actualDimensions = new int[] { UIUtil.getScreenWidth(view.getContext()) / 4, UIUtil.getScreenWidth(view.getContext()) / 4 };
                    }
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        }

        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, final int position) {
        /*holder.itemView.setSelected(selectedPos == position);
        if (selectedPos == position) {
            holder.durationTv.setVisibility(View.INVISIBLE);
        } else {
            holder.durationTv.setVisibility(View.VISIBLE);
        }*/
        VideoInfo video = videoListData.get(position);
        holder.durationTv.setText(convertSecondsToTime(video.getDuration() / 1000));
        requestBuilder
                .clone()
                .load(video.getVideoPath())
                .into(holder.videoCover);
    }

    @Override
    public int getItemCount() {
        return videoListData.size();
    }

    void setItemClickCallback(final ItemClickCallback itemClickCallback) {
        this.mItemClickCallback = itemClickCallback;
    }

    /*void setItemSelectCallback(final ItemSelectCallback itemSelectCallback) {
        this.itemSelectCallback = itemSelectCallback;
    }*/

    private boolean isSelected = false;

    class VideoViewHolder extends RecyclerView.ViewHolder {

        ImageView videoCover, selectIcon;
        View videoItemView, videoSelectPanel;
        TextView durationTv;

        VideoViewHolder(final View itemView) {
            super(itemView);
            videoItemView = itemView.findViewById(R.id.video_view);
            videoCover = (ImageView) itemView.findViewById(R.id.cover_image);
            durationTv = (TextView) itemView.findViewById(R.id.video_duration);
            videoSelectPanel = itemView.findViewById(R.id.video_select_panel);
            selectIcon = (ImageView) itemView.findViewById(R.id.select);

            int size = UIUtil.getScreenWidth(itemView.getContext()) / 4;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) videoCover.getLayoutParams();
            params.width = size;
            params.height = size;
            videoCover.setLayoutParams(params);
            videoSelectPanel.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                        VideoInfo videoInfo = videoListData.get(getAdapterPosition());
                        if (videoSelect.size() > 0) {
                            if (videoInfo.equals(videoSelect.get(0))) {
                                selectIcon.setImageResource(R.drawable.icon_video_unselected);
                                clearAll();
                                isSelected = false;
                            } else {
                                selectIconList.get(0).setImageResource(R.drawable.icon_video_unselected);
                                clearAll();
                                addData(videoInfo);
                                selectIcon.setImageResource(R.drawable.icon_video_selected);
                                isSelected = true;
                            }

                        } else {
                            clearAll();
                            addData(videoInfo);
                            selectIcon.setImageResource(R.drawable.icon_video_selected);
                            isSelected = true;
                        }
                        mItemClickCallback.onItemClickCallback(isSelected, videoListData.get(getAdapterPosition()));
                        return true;
                    } /*else if ((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                            && event.getAction() == KeyEvent.ACTION_UP){
                        itemSelectCallback.onItemSelectCallback(getAdapterPosition());
                        return true;
                    }*/
                    return false;
                }
            });
            videoSelectPanel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VideoInfo videoInfo = videoListData.get(getAdapterPosition());
                    if(videoSelect.size() > 0 ){
                        if(videoInfo.equals(videoSelect.get(0))){
                            selectIcon.setImageResource(R.drawable.icon_video_unselected);
                            clearAll();
                            isSelected = false;
                        }else{
                            selectIconList.get(0).setImageResource(R.drawable.icon_video_unselected);
                            clearAll();
                            addData(videoInfo);
                            selectIcon.setImageResource(R.drawable.icon_video_selected);
                            isSelected = true;
                        }

                    }else{
                        clearAll();
                        addData(videoInfo);
                        selectIcon.setImageResource(R.drawable.icon_video_selected);
                        isSelected = true;
                    }
                    mItemClickCallback.onItemClickCallback(isSelected, videoListData.get(getAdapterPosition()));
                }
            });
        }

        private void addData(VideoInfo videoInfo) {
            videoSelect.add(videoInfo);
            selectIconList.add(selectIcon);
        }
    }

    private void clearAll() {
        videoSelect.clear();
        selectIconList.clear();
    }

    @NonNull
    @Override
    public List<VideoInfo> getPreloadItems(int position) {
        return Collections.singletonList(videoListData.get(position));
    }

    @Nullable
    @Override
    public RequestBuilder<Drawable> getPreloadRequestBuilder(@NonNull VideoInfo item) {
        return requestBuilder
                .clone()
                .load(item.getVideoPath());
    }

    @Nullable
    @Override
    public int[] getPreloadSize(@NonNull VideoInfo item, int adapterPosition,
                                int perItemPosition) {
        return actualDimensions;
    }

    /**
     * second to HH:MM:ss
     * @param seconds
     * @return
     */
    private static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0)
            return "00:00";
        else {
            minute = (int)seconds / 60;
            if (minute < 60) {
                second = (int)seconds % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = (int)(seconds - hour * 3600 - minute * 60);
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
