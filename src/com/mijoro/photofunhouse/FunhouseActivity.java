package com.mijoro.photofunhouse;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.mijoro.photofunhouse.GLLayer.HostApplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

public class FunhouseActivity extends Activity implements HostApplication {
	private GLLayer glView;
	private ViewGroup mToolbar;
	private SurfaceView mSurfaceView;
	ImageButton mLastPicButton;
	private String mLastImageURI;
	private Handler mUIHandler;
	private CameraPreviewSink mCameraSink;
	private Animation mHideSlider, mShowSlider;
	private SeekBar mValueSlider;
	private boolean mHasFrontCamera = false;
	private boolean mUsingFrontCamera = false;
	
	private MyOrientationEventListener mOrientationListener;
	// The device orientation in degrees. Default is unknown.
	private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    // The orientation compensation for icons and thumbnails.
	private int mOrientationCompensation = 0;
	
	private static final String ANALYTICS_UA = "UA-24093011-1";
	private GoogleAnalyticsTracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTracker = GoogleAnalyticsTracker.getInstance();
        mTracker.start(ANALYTICS_UA, 30, this);
        // Create orientation listenter. This should be done first because it
        // takes some time to get first orientation.
        mOrientationListener = new MyOrientationEventListener(this);
        mOrientationListener.enable();
        mUIHandler = new Handler();
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        glView = (GLLayer)findViewById(R.id.FunhouseLayer);
        glView.setTracker(mTracker);
        mToolbar = (ViewGroup)findViewById(R.id.Toolbar);
        mLastPicButton = (ImageButton)findViewById(R.id.last_pic_button);
        mLastPicButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mLastImageURI)), "image/png");
                startActivity(intent);
            }
        });
        
        mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        mCameraSink = new CameraPreviewSink(mSurfaceView);
        mUsingFrontCamera = mCameraSink.isFrontFacing();
        glView.setCameraPreviewSink(mCameraSink);
        glView.setHostApplication(this);
        
        ImageButton cameraButton = (ImageButton)findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                glView.saveImage();
            }
        });
        
        mValueSlider = (SeekBar)findViewById(R.id.value_slider);
        mValueSlider.setOnSeekBarChangeListener(glView);
        mHideSlider = new AlphaAnimation(1.0f, 0.2f);
        mShowSlider = new AlphaAnimation(0.2f, 1.0f);
        mHideSlider.setFillAfter(true);
        mHideSlider.setDuration(1000);
        mShowSlider.setDuration(1000);
        mShowSlider.setFillAfter(true);
        mValueSlider.startAnimation(mHideSlider);
        mValueSlider.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    mValueSlider.startAnimation(mShowSlider);
                else if (event.getAction() == MotionEvent.ACTION_UP)
                    mValueSlider.startAnimation(mHideSlider);
                return false;
            }
        });
        try {
            Class<?> cameraClass = Class.forName("android.hardware.Camera");
            Method numCamsMethod = cameraClass.getMethod("getNumberOfCameras");
            if (numCamsMethod != null) {
                int numCams = (Integer)numCamsMethod.invoke(null);
                if (numCams > 1)
                    mHasFrontCamera = true;
            }
        } catch(ClassNotFoundException e) {
            
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
                !glView.isOverviewMode()) {
            glView.toggleOverview();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public void showSlider(boolean show) {
        mValueSlider.setProgress(50);
        mValueSlider.setVisibility(show ? View.VISIBLE : View.GONE);
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        if (!mHasFrontCamera) {
            menu.getItem(1).setVisible(false);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next_filter) {
            glView.nextProgram();
            return true;
        } else if (item.getItemId() == R.id.switch_camera) {
            mCameraSink.switchCamera();
            mUsingFrontCamera = mCameraSink.isFrontFacing();
            glView.setFrontFacing(mCameraSink.isFrontFacing());
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
        mTracker.dispatch();
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        mOrientationListener.enable();
        glView.onResume();
    }
    
    private Runnable mSDCardErrorRunnable = new Runnable() {
        public void run() {
            String message = "Please Unmount the SD Card before taking pictures.";
            Toast t = Toast.makeText(FunhouseActivity.this, message, 3000);
            t.show();
        }
    };

    public void photoTaken(final Bitmap b) {
        int rotation = 0;
        if (mOrientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            if (mUsingFrontCamera) {
                rotation = (-90 -mOrientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (-90 + mOrientation) % 360;
            }
        }
        final Bitmap bitmapToSave;
        if (rotation != 0) {
            Matrix m = new Matrix();
            m.postRotate(-rotation);
            bitmapToSave = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        } else {
            bitmapToSave = b;
        }
        
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            mUIHandler.post(mSDCardErrorRunnable);
            return;
        }
        mUIHandler.post(new Runnable() {
            public void run() {
                Bitmap smallBitmap = Bitmap.createScaledBitmap(b, 70, 70, true);
                mLastPicButton.setImageBitmap(smallBitmap);
                mLastPicButton.setVisibility(View.VISIBLE);
            }
        });

        File picsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        picsDir.mkdirs();
        String friendlydate = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date());
        friendlydate = friendlydate.replace(':', '_');
        String filename =  friendlydate+ ".jpg";
        File file = new File(picsDir, filename);
        
        mLastImageURI = file.toString();
        try {
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(file);
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out);
            ExifInterface exif = new ExifInterface(file.toString());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            
            scanFile(file);
            String date = dateFormat.format(new Date());
            exif.setAttribute(ExifInterface.TAG_DATETIME, date);
            exif.saveAttributes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void overviewModeShowing(boolean showing) {
        mToolbar.setVisibility(showing ? View.GONE : View.VISIBLE);
    }
    
    private void scanFile(File file) {
        MediaScannerConnection.scanFile(this,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
            public void onScanCompleted(String path, Uri uri) {
                Log.i("ExternalStorage", "Scanned " + path + ":");
                Log.i("ExternalStorage", "-> uri=" + uri);
            }
        });
    }
    
    private class MyOrientationEventListener
    extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }
        
        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = roundOrientation(orientation);
        }
       }

    
    private static int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTracker.stop();
    }

    public void setupComplete() {
        //mSurfaceView.setVisibility(View.GONE);
        
    }
}
