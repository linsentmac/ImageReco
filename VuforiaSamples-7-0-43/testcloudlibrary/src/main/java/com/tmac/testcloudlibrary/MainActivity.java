package com.tmac.testcloudlibrary;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.lenovo.letarget.CloudCardRecognize;
import cn.lenovo.letarget.ImageRecoRenderListener;
import cn.lenovo.letarget.SampleApplication.utils.LoadingDialogHandler;

public class MainActivity extends Activity implements ImageRecoRenderListener{

    private CloudCardRecognize cloudCardRecognize;
    private RelativeLayout mUILayout;
    private LoadingDialogHandler loadingDialogHandler;

    private TextView result_display;

    static final int HIDE_LOADING_DIALOG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        loadingDialogHandler = new LoadingDialogHandler(this);
        startLoadUIViewAndAnimation();
        cloudCardRecognize = new CloudCardRecognize(this, this);

    }

    private void startLoadUIViewAndAnimation(){

        mUILayout = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.activity_main, null, false);

        result_display = mUILayout.findViewById(R.id.result_display);

        // By default
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);
        loadingDialogHandler.mLoadingDialogContainer
                .setVisibility(View.VISIBLE);

        addContentView(mUILayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

    }

    @Override
    protected void onResume() {
        super.onResume();
        cloudCardRecognize.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cloudCardRecognize.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cloudCardRecognize.onDestory();
    }

    @Override
    public void imageRender(final String recoName) {
        Log.d("LS-MainActivity", "recoName = " + recoName);
        mHandler.removeMessages(MSG_TEXT_NULL);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result_display.setText(recoName);
                mHandler.sendEmptyMessageDelayed(MSG_TEXT_NULL, 1000);
            }
        });
    }

    @Override
    public void onError() {

    }

    @Override
    public void initARDone() {
        mUILayout.bringToFront();
        // Hides the Loading Dialog
        loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);
        //mUILayout.setBackgroundColor(Color.WHITE);
    }


    private final int MSG_TEXT_NULL = 100;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_TEXT_NULL:
                    result_display.setText(null);
                    break;
            }
        }
    };
}
