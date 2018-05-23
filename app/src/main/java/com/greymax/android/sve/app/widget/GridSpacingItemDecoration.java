package com.greymax.android.sve.app.widget;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;


public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration{

  private int halfSpace;

  public GridSpacingItemDecoration(int space) {
    this.halfSpace = space / 2;
  }

  @Override
  public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

    if (parent.getPaddingLeft() != halfSpace) {
      parent.setPadding(halfSpace, halfSpace, halfSpace, halfSpace);
      parent.setClipToPadding(false);
    }

    outRect.top = halfSpace;
    outRect.bottom = halfSpace;
    outRect.left = halfSpace;
    outRect.right = halfSpace;
  }
}
