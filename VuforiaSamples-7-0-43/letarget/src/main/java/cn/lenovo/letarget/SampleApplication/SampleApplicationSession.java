/*===============================================================================
Copyright (c) 2016-2017 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2015 Qualcomm Connected Experiences, Inc. All Rights Reserved.

LeTarget is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/


package cn.lenovo.letarget.SampleApplication;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;

import cn.lenovo.letarget.R;


public class SampleApplicationSession implements UpdateCallbackInterface
{
    
    private static final String LOGTAG = "SampleAppSession";
    
    // Reference to the current activity
    private Activity mActivity;
    private SampleApplicationControl mSessionControl;
    
    // Flags
    private boolean mStarted = false;
    private boolean mCameraRunning = false;
    
    // The async tasks to initialize the LeTarget SDK:
    private InitLeTargetTask mInitLeTargetTask;
    private InitTrackerTask mInitTrackerTask;
    private LoadTrackerTask mLoadTrackerTask;
    private StartLeTargetTask mStartLeTargetTask;
    private ResumeLeTargetTask mResumeLeTargetTask;
    
    // An object used for synchronizing LeTarget initialization, dataset loading
    // and the Android onDestroy() life cycle event. If the application is
    // destroyed while a data set is still being loaded, then we wait for the
    // loading operation to finish before shutting down LeTarget:
    private final Object mLifecycleLock = new Object();
    
    // LeTarget initialization flags:
    private int mLeTargetFlags = 0;
    
    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;

    private String LICENSE_KEY;

    public SampleApplicationSession(SampleApplicationControl sessionControl, String key)
    {
        mSessionControl = sessionControl;
        LICENSE_KEY = key;
    }
    
    
    // Initializes LeTarget and sets up preferences.
    public void initAR(Activity activity, int screenOrientation)
    {
        SampleApplicationException LeTargetException = null;
        mActivity = activity;
        
        if ((screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
            && (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO))
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
        
        // Use an OrientationChangeListener here to capture all orientation changes.  Android
        // will not send an Activity.onConfigurationChanged() callback on a 180 degree rotation,
        // ie: Left Landscape to Right Landscape.  LeTarget needs to react to this change and the
        // SampleApplicationSession needs to update the Projection Matrix.
        OrientationEventListener orientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int i) {
                int activityRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
                if(mLastRotation != activityRotation)
                {
                    mLastRotation = activityRotation;
                }
            }

            int mLastRotation = -1;
        };
        
        if(orientationEventListener.canDetectOrientation())
            orientationEventListener.enable();

        // Apply screen orientation
        mActivity.setRequestedOrientation(screenOrientation);
        
        // As long as this window is visible to the user, keep the device's
        // screen turned on and bright:
        mActivity.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        mLeTargetFlags = INIT_FLAGS.GL_20;
        
        // Initialize LeTarget SDK asynchronously to avoid blocking the
        // main (UI) thread.
        //
        // NOTE: This task instance must be created and invoked on the
        // UI thread and it can be executed only once!
        if (mInitLeTargetTask != null)
        {
            String logMessage = "Cannot initialize SDK twice";
            LeTargetException = new SampleApplicationException(
                SampleApplicationException.LATARGET_ALREADY_INITIALIZATED,
                logMessage);
            Log.e(LOGTAG, logMessage);
        }
        
        if (LeTargetException == null)
        {
            try {
                mInitLeTargetTask = new InitLeTargetTask();
                mInitLeTargetTask.execute();
            }
            catch (Exception e)
            {
                String logMessage = "Initializing LeTarget SDK failed";
                LeTargetException = new SampleApplicationException(
                    SampleApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (LeTargetException != null)
        {
            // Send LeTarget Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(LeTargetException);
        }
    }
    
    
    // Starts LeTarget, initialize and starts the camera and start the trackers
    private void startCameraAndTrackers(int camera) throws SampleApplicationException
    {
        String error;
        if(mCameraRunning)
        {
        	error = "Camera already running, unable to open again";
        	Log.e(LOGTAG, error);
            throw new SampleApplicationException(
                SampleApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mCamera = camera;
        if (!CameraDevice.getInstance().init(camera))
        {
            error = "Unable to open camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new SampleApplicationException(
                SampleApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
               
        if (!CameraDevice.getInstance().selectVideoMode(
            CameraDevice.MODE.MODE_DEFAULT))
        {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new SampleApplicationException(
                SampleApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        if (!CameraDevice.getInstance().start())
        {
            error = "Unable to start camera device: " + camera;
            Log.e(LOGTAG, error);
            throw new SampleApplicationException(
                SampleApplicationException.CAMERA_INITIALIZATION_FAILURE, error);
        }
        
        mSessionControl.doStartTrackers();
        
        mCameraRunning = true;
    }

    public void startAR(int camera)
    {
        mCamera = camera;
        SampleApplicationException LeTargetException = null;

        try {
            mStartLeTargetTask = new StartLeTargetTask();
            mStartLeTargetTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Starting LeTarget failed";
            LeTargetException = new SampleApplicationException(
                    SampleApplicationException.CAMERA_INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (LeTargetException != null)
        {
            // Send LeTarget Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(LeTargetException);
        }
    }

    
    // Stops any ongoing initialization, stops LeTarget
    public void stopAR() throws SampleApplicationException
    {
        // Cancel potentially running tasks
        if (mInitLeTargetTask != null
            && mInitLeTargetTask.getStatus() != InitLeTargetTask.Status.FINISHED)
        {
            mInitLeTargetTask.cancel(true);
            mInitLeTargetTask = null;
        }
        
        if (mLoadTrackerTask != null
            && mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }
        
        mInitLeTargetTask = null;
        mLoadTrackerTask = null;
        
        mStarted = false;
        
        stopCamera();
        
        // Ensure that all asynchronous operations to initialize LeTarget
        // and loading the tracker datasets do not overlap:
        synchronized (mLifecycleLock)
        {
            
            boolean unloadTrackersResult;
            boolean deinitTrackersResult;
            
            // Destroy the tracking data set:
            unloadTrackersResult = mSessionControl.doUnloadTrackersData();
            
            // Deinitialize the trackers:
            deinitTrackersResult = mSessionControl.doDeinitTrackers();
            
            // Deinitialize LeTarget SDK:
            Vuforia.deinit();
            
            if (!unloadTrackersResult)
                throw new SampleApplicationException(
                    SampleApplicationException.UNLOADING_TRACKERS_FAILURE,
                    "Failed to unload trackers\' data");
            
            if (!deinitTrackersResult)
                throw new SampleApplicationException(
                    SampleApplicationException.TRACKERS_DEINITIALIZATION_FAILURE,
                    "Failed to deinitialize trackers");
            
        }
    }
    

    // Resumes LeTarget, restarts the trackers and the camera
    private void resumeAR()
    {
        SampleApplicationException LeTargetException = null;

        try {
            mResumeLeTargetTask = new ResumeLeTargetTask();
            mResumeLeTargetTask.execute();
        }
        catch (Exception e)
        {
            String logMessage = "Resuming LeTarget failed";
            LeTargetException = new SampleApplicationException(
                    SampleApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (LeTargetException != null)
        {
            // Send LeTarget Exception to the application and call initDone
            // to stop initialization process
            mSessionControl.onInitARDone(LeTargetException);
        }
    }


    // Pauses LeTarget and stops the camera
    public void pauseAR() throws SampleApplicationException
    {
        if (mStarted)
        {
            stopCamera();
        }
        
        Vuforia.onPause();
    }
    
    
    // Callback called every cycle
    @Override
    public void Vuforia_onUpdate(State s)
    {
        mSessionControl.onLeTargetUpdate(s);
    }
    
    
    // Manages the configuration changes
    public void onConfigurationChanged()
    {
        if (mStarted)
        {
            Device.getInstance().setConfigurationChanged();
        }
    }
    
    
    // Methods to be called to handle lifecycle
    public void onResume()
    {
        if (mResumeLeTargetTask == null
                || mResumeLeTargetTask.getStatus() == ResumeLeTargetTask.Status.FINISHED)
        {
            // onResume() will sometimes be called twice depending on the screen lock mode
            // This will prevent redundant AsyncTasks from being executed
            resumeAR();
        }
    }
    
    
    public void onPause()
    {
        Vuforia.onPause();
    }
    
    
    public void onSurfaceChanged(int width, int height)
    {
        Vuforia.onSurfaceChanged(width, height);
    }
    
    
    public void onSurfaceCreated()
    {
        Vuforia.onSurfaceCreated();
    }
    
    // An async task to initialize LeTarget asynchronously.
    private class InitLeTargetTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int mProgressValue = -1;
        
        
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (mLifecycleLock)
            {
                Vuforia.setInitParameters(mActivity, mLeTargetFlags, LICENSE_KEY);

                do
                {
                    // LeTarget.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If LeTarget.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    mProgressValue = Vuforia.init();
                    
                    // Publish the progress value:
                    publishProgress(mProgressValue);
                    
                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                } while (!isCancelled() && mProgressValue >= 0
                    && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }
        
        
        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }
        
        
        protected void onPostExecute(Boolean result)
        {
            // Done initializing LeTarget, proceed to next application
            // initialization status:

            Log.d(LOGTAG, "InitLeTargetTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));
            
            SampleApplicationException LeTargetException = null;
            
            if (result)
            {
                try {
                    mInitTrackerTask = new InitTrackerTask();
                    mInitTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to initialize tracker.";
                    LeTargetException = new SampleApplicationException(
                            SampleApplicationException.TRACKERS_INITIALIZATION_FAILURE,
                            logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            } else
            {
                String logMessage;
                
                // NOTE: Check if initialization failed because the device is
                // not supported. At this point the user should be informed
                // with a message.
                logMessage = getInitializationErrorString(mProgressValue);
                
                // Log error:
                Log.e(LOGTAG, "InitLeTargetTask.onPostExecute: " + logMessage
                    + " Exiting.");

                LeTargetException = new SampleApplicationException(
                    SampleApplicationException.INITIALIZATION_FAILURE,
                    logMessage);
            }

            if (LeTargetException != null)
            {
                // Send LeTarget Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(LeTargetException);
            }
        }
    }

    // An async task to resume LeTarget asynchronously
    private class ResumeLeTargetTask extends AsyncTask<Void, Void, Void>
    {
        protected Void doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                Vuforia.onResume();
            }

            return null;
        }

        protected void onPostExecute(Void result)
        {
            Log.d(LOGTAG, "ResumeLeTargetTask.onPostExecute");

            // We may start the camera only if the LeTarget SDK has already been initialized
            if (mStarted && !mCameraRunning)
            {
                startAR(mCamera);
                mSessionControl.onLeTargetResumed();
            }
        }
    }

    // An async task to initialize trackers asynchronously
    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected  Boolean doInBackground(Void... params)
        {
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result)
        {

            SampleApplicationException LeTargetException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            if (result)
            {
                try {
                    mLoadTrackerTask = new LoadTrackerTask();
                    mLoadTrackerTask.execute();
                }
                catch (Exception e)
                {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    LeTargetException = new SampleApplicationException(
                            SampleApplicationException.LOADING_TRACKERS_FAILURE,
                            logMessage);
                }
            }
            else
            {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);

                // Error loading dataset
                LeTargetException = new SampleApplicationException(
                        SampleApplicationException.TRACKERS_INITIALIZATION_FAILURE,
                        logMessage);
            }

            if (LeTargetException != null)
            {
                // Send LeTarget Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(LeTargetException);
            }
        }
    }
    
    // An async task to load the tracker data asynchronously.
    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                // Load the tracker data set:
                return mSessionControl.doLoadTrackersData();
            }
        }
        
        protected void onPostExecute(Boolean result)
        {
            
            SampleApplicationException LeTargetException = null;
            
            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));
            
            if (!result)
            {
                String logMessage = "Failed to load tracker data.";
                // Error loading dataset
                Log.e(LOGTAG, logMessage);
                LeTargetException = new SampleApplicationException(
                    SampleApplicationException.LOADING_TRACKERS_FAILURE,
                    logMessage);
            } else
            {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();
                
                Vuforia.registerCallback(SampleApplicationSession.this);

                mStarted = true;
            }
            
            // Done loading the tracker, update application status, send the
            // exception to check errors
            mSessionControl.onInitARDone(LeTargetException);
        }
    }

    // An async task to start the camera and trackers
    private class StartLeTargetTask extends AsyncTask<Void, Void, Boolean>
    {
        SampleApplicationException LeTargetException = null;
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the concurrent lifecycle operations:
            synchronized (mLifecycleLock)
            {
                try {
                    startCameraAndTrackers(mCamera);
                }
                catch (SampleApplicationException e)
                {
                    Log.e(LOGTAG, "StartLeTargetTask.doInBackground: Could not start AR with exception: " + e);
                    LeTargetException = e;
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result)
        {
            Log.d(LOGTAG, "StartLeTargetTask.onPostExecute: execution "
                + (result ? "successful" : "failed"));

            mSessionControl.onLeTargetStarted();

            if (LeTargetException != null)
            {
                // Send LeTarget Exception to the application and call initDone
                // to stop initialization process
                mSessionControl.onInitARDone(LeTargetException);
            }
        }
    }
    
    
    // Returns the error message for each error code
    private String getInitializationErrorString(int code)
    {
        if (code == INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED)
            return mActivity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED);
        if (code == INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS)
            return mActivity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
        else
        {
            return mActivity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR);
        }
    }
    
    
    public void stopCamera()
    {
        if (mCameraRunning)
        {
            mSessionControl.doStopTrackers();
            mCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }
    
    
    // Returns true if LeTarget is initialized, the trackers started and the
    // tracker data loaded
    private boolean isARRunning()
    {
        return mStarted;
    }
    
}
