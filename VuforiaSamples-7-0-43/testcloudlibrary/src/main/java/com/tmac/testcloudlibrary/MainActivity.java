package com.tmac.testcloudlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

    static final int MY_PERMISSION_REQUEST_CODE = 1;

    /**
     * The following several key fields are necessary, there are sample key.
     * We need the key to connect cloud database and recognition.
     * you can contact us to get the key.
     */
    private static final String license_key = "AaARwsP/////AAAAmV7j7EcF7k4ShBn0MLTai5kBZ+RiN7J3CBEhdKYMuA8tNLA6am/Kw6Vr9tux4fotVzt7hLBY+dis7GxpqbJkoiP0LvSmsFYifVHL62AkoeqdCvACMsBJNStEIfR+VP475TcjpA0TyALKXK6xxRtsAFRiTtUbooQS2WeBDn3qCzWx14PIJJdDZv8U3Mp/BPPxHs3xWsWjcp6nXqGdjM9vEFi6x9em06NR7HBmul9iP09VVx0JwyMRoX+MnU1xmUCtXIVB+RBzuAlfvZcbCz3XXDEfc7SPuvG2iGbIhMt7yM5d04Jsc1LxAhg8B3TMZpEfGDm4mhleOBp6VtlmXWz2Ey75YVmpL879b//PaHNmoAGP";
    private static final String kAccessKey = "9490889251d27d3aef8c8277fcdf34954692b7f4";
    private static final String kSecretKey = "74830034b893fb6bee7e081df5503819e630b669";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);

        loadingDialogHandler = new LoadingDialogHandler(this);
        startLoadUIViewAndAnimation();

        requestPemission();

    }

    private void requestPemission(){
        /**
         * 第 1 步: 检查是否有相应的权限
         */
        boolean isAllGranted = checkPermissionAllGranted(
                new String[] {
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }
        );

        if(isAllGranted){
            cloudCardRecognize = new CloudCardRecognize(this, this, license_key, kAccessKey, kSecretKey);
            return;
        }


        /**
         * 第 2 步: 请求权限
         */
        // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                MY_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * 第 3 步: 申请权限结果返回处理
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                // 如果所有的权限都授予了, 则开始识别
                cloudCardRecognize = new CloudCardRecognize(this, this, license_key, kAccessKey, kSecretKey);
            }
        }
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
        if(cloudCardRecognize != null){
            Log.d("LS-", "onResume");
            cloudCardRecognize.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cloudCardRecognize != null){
            cloudCardRecognize.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cloudCardRecognize != null){
            cloudCardRecognize.onDestory();
        }
    }

    @Override
    public void imageRecoSuccess(final String recoName) {
        Log.d("LS-MainActivity", "recoName = " + recoName);
        mHandler.removeMessages(MSG_TEXT_NULL);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result_display.setText(recoName);
                mHandler.sendEmptyMessageDelayed(MSG_TEXT_NULL, 500);
            }
        });
    }

    @Override
    public void initARDone() {
        Log.d("LS-Tmac", "initARDone ........... ");
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


    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

}
