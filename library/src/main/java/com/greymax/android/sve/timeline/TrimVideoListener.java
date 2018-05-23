package com.greymax.android.sve.timeline;

import com.greymax.android.sve.filters.FilterType;

public interface TrimVideoListener {

    void onStartTrim();

    void onFinishTrim(String url, FilterType filterType);

    void onCancelTrim();

    void onTrimFail();
}
