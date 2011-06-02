package com.mijoro.photofunhouse;

import java.io.IOException;
import java.util.List;


import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This class handles the camera. In particular, the method setPreviewCallback
 * is used to receive camera images. The camera images are not processed in
 * this class but delivered to the GLLayer. This class itself does
 * not display the camera images.
 * 
 * @author Niels
 *
 */
public class CamLayer extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {
    Camera mCamera;
    boolean isPreviewRunning = false;
    Camera.PreviewCallback callback;
    public int previewFrameWidth = 352;

    public int previewFrameHeight = 288;
    public int textureSize;
    
    static final public int nextPowerOfTwo(int x) {
        double val = (double) x;
        return (int) Math.pow(2, Math.ceil(log2(val)));
    }
    static final public double log2(double x) {
        return Math.log(x)/Math.log(2);
    }


    CamLayer(FunhouseActivity context, Camera.PreviewCallback callback) {
        super(context);
        this.callback=callback;
        
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        SurfaceHolder mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
    	synchronized(this) {
    	    
	        mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
	
	    	Camera.Parameters p = mCamera.getParameters();
	    	p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
	    	List<Size>sizes = p.getSupportedPreviewSizes();
	    	for (Size s : sizes) {
	    	    System.out.println(s.width + "/" + s.height);
	    	}
	    	Size previewSize = p.getPreviewSize();
	        previewFrameWidth = previewSize.width;
	        previewFrameHeight = previewSize.height;
	        textureSize = nextPowerOfTwo(Math.max(previewFrameWidth, previewFrameHeight));
	    	mCamera.setParameters(p);
	    	
	    	
	    	try {
				mCamera.setPreviewDisplay(holder);
			} catch (IOException e) {
				Log.e("Camera", "mCamera.setPreviewDisplay(holder);");
			}
			
	    	mCamera.startPreview();
    		mCamera.setPreviewCallback(this);
    	}
	}
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
    	synchronized(this) {
	    	try {
		    	if (mCamera!=null) {
		    		mCamera.stopPreview();  
		    		isPreviewRunning=false;
		    		mCamera.release();
		    	}
	    	} catch (Exception e) {
				Log.e("Camera", e.getMessage());
	    	}
    	}
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

    }

	public void onPreviewFrame(byte[] yuvs, Camera arg1) {
    	if (callback!=null)
    		callback.onPreviewFrame(yuvs, arg1);        
	}
}
