package cn.lenovo.card_reco;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.vuforia.CameraDevice;
import com.vuforia.ObjectTracker;
import com.vuforia.State;
import com.vuforia.TargetFinder;
import com.vuforia.TargetSearchResult;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.util.Vector;

import cn.lenovo.card_reco.SampleApplication.SampleApplicationControl;
import cn.lenovo.card_reco.SampleApplication.SampleApplicationException;
import cn.lenovo.card_reco.SampleApplication.SampleApplicationSession;
import cn.lenovo.card_reco.SampleApplication.utils.LoadingDialogHandler;
import cn.lenovo.card_reco.SampleApplication.utils.SampleApplicationGLView;
import cn.lenovo.card_reco.SampleApplication.utils.Texture;

/**
 * Created by linsen3 on 2018/6/5.
 */

public class CloudCardRecognize implements SampleApplicationControl {

    private static final String LOGTAG = "LS-CloudCardRecognize";

    static final int UPDATE_ERROR_AUTHORIZATION_FAILED = -1;
    static final int UPDATE_ERROR_PROJECT_SUSPENDED = -2;
    static final int UPDATE_ERROR_NO_NETWORK_CONNECTION = -3;
    static final int UPDATE_ERROR_SERVICE_NOT_AVAILABLE = -4;
    static final int UPDATE_ERROR_BAD_FRAME_QUALITY = -5;
    static final int UPDATE_ERROR_UPDATE_SDK = -6;
    static final int UPDATE_ERROR_TIMESTAMP_OUT_OF_RANGE = -7;
    static final int UPDATE_ERROR_REQUEST_TIMEOUT = -8;

    static final int HIDE_LOADING_DIALOG = 0;

    private ImageRecoRenderListener mListener;
    private Activity mActivity;

    private SampleApplicationSession vuforiaAppSession;

    // Our OpenGL view:
    private SampleApplicationGLView mGlView;
    private CloudRecoRenderer mRenderer;

    private GestureDetector mGestureDetector;
    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    private boolean mExtendedTracking = false;
    private boolean mIsDroidDevice = false;

    // tmac custom
    private static final String kAccessKey = "9490889251d27d3aef8c8277fcdf34954692b7f4";
    private static final String kSecretKey = "74830034b893fb6bee7e081df5503819e630b669";
    private int mInitErrorCode = 0;


    public CloudCardRecognize(ImageRecoRenderListener listener, Activity activity){
        mListener = listener;
        mActivity = activity;

        vuforiaAppSession = new SampleApplicationSession(this);
        vuforiaAppSession
                .initAR(mActivity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Creates the GestureDetector listener for processing double tap
        mGestureDetector = new GestureDetector(mActivity, new GestureListener());

        mTextures = new Vector<Texture>();
        loadTextures();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(
                "droid");
    }

    public void onResume(){
        if (mIsDroidDevice)
        {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        vuforiaAppSession.onResume();
    }

    public void onPause(){
        try
        {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e)
        {
            e.printStackTrace();
        }

        // Pauses the OpenGLView
        if (mGlView != null)
        {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }
    }

    public void onDestory(){
        try
        {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e)
        {
            e.printStackTrace();
        }

        System.gc();
    }

    private void initApplicationAR()
    {
        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        // Initialize the GLView with proper flags
        mGlView = new SampleApplicationGLView(mActivity);
        mGlView.init(translucent, depthSize, stencilSize);

        // Setups the Renderer of the GLView
        mRenderer = new CloudRecoRenderer(vuforiaAppSession, mActivity, mListener);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);

    }

    // We want to load specific textures from the APK, which we will later use
    // for rendering.
    private void loadTextures()
    {
        mTextures.add(Texture.loadTextureFromApk("TextureTeapotRed.png",
                mActivity.getAssets()));
    }


    // Process Single Tap event to trigger autofocus
    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener
    {
        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();


        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e)
        {
            boolean result = CameraDevice.getInstance().setFocusMode(
                    CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable()
            {
                public void run()
                {
                    final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(
                            CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                    if (!autofocusResult)
                        Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                }
            }, 1000L);

            return true;
        }
    }


    @Override
    public boolean doInitTrackers() {
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Indicate if the trackers were initialized correctly
        boolean result = true;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null)
        {
            Log.e(
                    LOGTAG,
                    "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else
        {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        return result;
    }

    @Override
    public boolean doLoadTrackersData() {
        Log.d(LOGTAG, "initCloudReco");

        // Get the object tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Initialize target finder:
        TargetFinder targetFinder = objectTracker.getTargetFinder();

        // Start initialization:
        if (targetFinder.startInit(kAccessKey, kSecretKey))
        {
            targetFinder.waitUntilInitFinished();
        }

        int resultCode = targetFinder.getInitState();
        if (resultCode != TargetFinder.INIT_SUCCESS)
        {
            if(resultCode == TargetFinder.INIT_ERROR_NO_NETWORK_CONNECTION)
            {
                mInitErrorCode = UPDATE_ERROR_NO_NETWORK_CONNECTION;
            }
            else
            {
                mInitErrorCode = UPDATE_ERROR_SERVICE_NOT_AVAILABLE;
            }

            Log.e(LOGTAG, "Failed to initialize target finder.");
            return false;
        }

        return true;    }

    @Override
    public boolean doStartTrackers() {
        // Indicate if the trackers were started correctly
        boolean result = true;

        // Start the tracker:
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        objectTracker.start();

        // Start cloud based recognition if we are in scanning mode:
        TargetFinder targetFinder = objectTracker.getTargetFinder();
        targetFinder.startRecognition();
        /*scanlineStart();
        mFinderStarted = true;*/

        return result;
    }

    @Override
    public boolean doStopTrackers() {
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        if(objectTracker != null)
        {
            objectTracker.stop();

            // Stop cloud based recognition:
            TargetFinder targetFinder = objectTracker.getTargetFinder();
            targetFinder.stop();
            /*scanlineStop();
            mFinderStarted = false;*/

            // Clears the trackables
            targetFinder.clearTrackables();
        }
        else
        {
            result = false;
        }

        return result;
    }

    @Override
    public boolean doUnloadTrackersData() {
        return true;
    }

    @Override
    public boolean doDeinitTrackers() {
        // Indicate if the trackers were deinitialized correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());

        return result;
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {
        if (exception == null)
        {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.

            mActivity.addContentView(mGlView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);

            mListener.initARDone();
            /*mUILayout.bringToFront();

            // Hides the Loading Dialog
            loadingDialogHandler.sendEmptyMessage(HIDE_LOADING_DIALOG);

            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            mSampleAppMenu = new SampleAppMenu(this, this, "Cloud Reco",
                    mGlView, mUILayout, null);
            setSampleAppMenuSettings();*/

        } else
        {
            Log.e(LOGTAG, exception.getString());
            if(mInitErrorCode != 0)
            {
                //showErrorMessage(mInitErrorCode,10, true);
            }
            else
            {
                //showInitializationErrorMessage(exception.getString());
            }
        }
    }

    @Override
    public void onVuforiaUpdate(State state) {
        // Get the tracker manager:
        TrackerManager trackerManager = TrackerManager.getInstance();

        // Get the object tracker:
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());

        // Get the target finder:
        TargetFinder finder = objectTracker.getTargetFinder();

        // Check if there are new results available:
        final int statusCode = finder.updateSearchResults();

        // Show a message if we encountered an error:
        if (statusCode < 0)
        {

            boolean closeAppAfterError = (
                    statusCode == UPDATE_ERROR_NO_NETWORK_CONNECTION ||
                            statusCode == UPDATE_ERROR_SERVICE_NOT_AVAILABLE);

            //showErrorMessage(statusCode, state.getFrame().getTimeStamp(), closeAppAfterError);

        } else if (statusCode == TargetFinder.UPDATE_RESULTS_AVAILABLE)
        {
            // Process new search results
            if (finder.getResultCount() > 0)
            {
                TargetSearchResult result = finder.getResult(0);

                // Check if this target is suitable for tracking:
                if (result.getTrackingRating() > 0)
                {
                    Trackable trackable = finder.enableTracking(result);

                    if (mExtendedTracking)
                        trackable.startExtendedTracking();
                }
            }
        }
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null)
        {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        // Set camera focus mode
        if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO))
        {
            // If continuous autofocus mode fails, attempt to set to a different mode
            if(!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO))
            {
                CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
            }
        }
    }
}
