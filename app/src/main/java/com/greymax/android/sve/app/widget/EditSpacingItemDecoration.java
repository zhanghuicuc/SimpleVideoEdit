package com.greymax.android.sve.app.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;


public class EditSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private int space;
    private int thumbnailsCount;

    public EditSpacingItemDecoration(int space, int thumbnailsCount) {
        this.space = space;
        this.thumbnailsCount = thumbnailsCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // 第一个的前面和最后一个的后面
        int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.left = space;
            outRect.right = 0;
        } else if (position == thumbnailsCount - 1) {
            outRect.left = 0;
            outRect.right = space;
        } else {
            outRect.left = 0;
            outRect.right = 0;
        }
    }
}