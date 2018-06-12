package cn.lenovo.sampletarget;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.lenovo.letarget.CloudCardRecognize;
import cn.lenovo.letarget.ImageRecoRenderListener;
import cn.lenovo.letarget.SampleApplication.utils.LoadingDialogHandler;

public class MainActivity extends Activity {

    private static final String TAG = "Smaple-Main";

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
        loadingDialogHandler = new LoadingDialogHandler(this);
        startLoadUIViewAndAnimation();
        requestPemission();
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

    private void requestPemission(){
        /**
         * Check Permission
         */
        boolean isAllGranted = checkPermissionAllGranted(
                new String[] {
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }
        );

        if(isAllGranted){
            cloudCardRecognize = new CloudCardRecognize(mImageListener, this, license_key, kAccessKey, kSecretKey);
            return;
        }

        /**
         * Requset Permission
         */
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                cloudCardRecognize = new CloudCardRecognize(mImageListener, this, license_key, kAccessKey, kSecretKey);
            }
        }
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
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

    ImageRecoRenderListener mImageListener = new ImageRecoRenderListener() {
        @Override
        public void imageRecoSuccess(final String recoName) {
            Log.d(TAG, "recoName = " + recoName);
            // 图片识别成功的回调，处理自己后续的业务逻辑
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
            Log.d(TAG, "initARDone ........... ");
            /**
             * 识别初始化成功，在此回调中可以去做UI上的显示，
             * mUILayout.bringToFront()； 将自定义Activity的Layout调到前台显示（仅针对投影设备的需求），
             * loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG); 并关闭缓冲的动画。
             */
            mUILayout.bringToFront();
            // Hides the Loading Dialog
            loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);
        }
    };

}
