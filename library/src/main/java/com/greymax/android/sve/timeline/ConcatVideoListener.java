package com.greymax.android.sve.timeline;


public interface ConcatVideoListener {
    void onStartConcat();

    void onFinishConcat(String url);

    void onCancelConcat();

    void onConcatFail();
}
