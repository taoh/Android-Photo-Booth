
package com.mijoro.photofunhouse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;

public class CameraPreviewSink implements Camera.PreviewCallback, Callback {
    private Camera mCamera;
    private int mCameraId;

    boolean initialized = false;
    int[] cameraTexture;

    byte[] cameraBytes = new byte[0];
    byte[] rgbBytes = new byte[0];
    byte[] rgbBytes2;
    
    Size mPreviewSize;
    TextureRatio mTextureRatio;
    public int textureSize = -1;
    
    private boolean mDirty;
    ByteBuffer rgbBuffer, rgbBuffer2;
    boolean useOtherBuffer = false;
    
    private Method mOpenCameraMethod;
    
    
    private static final int CAMERA_FACING_FRONT = 1;
    private static final int CAMERA_FACING_BACK = 0;
    
    
    public class TextureRatio {
        public float width, height;
        public TextureRatio(float w, float h) {
            width = w;
            height = h;
        }
    }
    private SurfaceView mSurfaceView;
    public CameraPreviewSink(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        try {
            Class<?> cameraClass = Class.forName("android.hardware.Camera");
            mOpenCameraMethod = cameraClass.getMethod("open", Integer.TYPE);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            // It's ok, probably on Froyo
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        initCamera(CAMERA_FACING_FRONT);
    }
    
    public void switchCamera() {
        int newID = CAMERA_FACING_FRONT == mCameraId ? 
                CAMERA_FACING_BACK : CAMERA_FACING_FRONT;
        initCamera(newID);
    }
    
    public boolean isFrontFacing() {
        return mCameraId == CAMERA_FACING_FRONT;
    }
    
    private Camera openCamera(int id) {
        if (mOpenCameraMethod != null) {
            try {
                mCameraId = id;
                return (Camera)mOpenCameraMethod.invoke(null, id);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        mCameraId = CAMERA_FACING_BACK;
        return Camera.open();
    }
    
    private void initCamera(int cameraId) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mCamera = openCamera(cameraId);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Camera.Parameters p = mCamera.getParameters();

        if (cameraId == CAMERA_FACING_BACK) p.setRotation(270);
        p.setPreviewFormat(ImageFormat.NV21);
        Size lowestSetting = p.getSupportedPreviewSizes().get(1);
        p.setPreviewSize(lowestSetting.width, lowestSetting.height);
        mPreviewSize = p.getPreviewSize();
        textureSize = Utilities.nextPowerOfTwo(Math.max(mPreviewSize.width, mPreviewSize.height));
        mTextureRatio = new TextureRatio(((float)mPreviewSize.width) / (float)textureSize, 1.0f - (float)mPreviewSize.height / (float)textureSize);

        cameraTexture = null;
        initialized = false;
        finishCameraInit();
    }

    public TextureRatio getTextureRatio() {
        return mTextureRatio;
    }

    public void onPreviewFrame(byte[] yuvs, Camera camera) {
        byte[] rgb = useOtherBuffer ? rgbBytes2 : rgbBytes;
        GLLayer.decode(yuvs, mPreviewSize.width, mPreviewSize.height, textureSize, rgb);
        if (mCamera != null) mCamera.addCallbackBuffer(yuvs);
        mDirty = true;
        useOtherBuffer = !useOtherBuffer;
    }
    
    public void bindTexture() {
        synchronized (this) {
            if (cameraTexture == null)  {
                cameraTexture = new int[1];
                GLES20.glGenTextures(1, cameraTexture, 0);
            }
            int tex = cameraTexture[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
            if (cameraBytes.length > 0) {
                if (!initialized) {
                    // todo: i wish there was a better way to reliably clear out a texture.
                    Bitmap b = Bitmap.createBitmap(textureSize, textureSize, Bitmap.Config.RGB_565);
                    b.eraseColor(0);
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, b, 0);
                    b.recycle();
                    initialized = true;
                }
                if (mDirty) {
                    ByteBuffer buff = useOtherBuffer ? rgbBuffer : rgbBuffer2;
                    GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mPreviewSize.width, mPreviewSize.height,
                            GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, buff);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    mDirty = false;
                }
            }
        }
    }
    
    public void onPause() {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
    
    public void onResume() {
        if (mCamera == null)
            initCamera(mCameraId);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        finishCameraInit();
    }
    
    private void finishCameraInit() {
        mPreviewSize = mCamera.getParameters().getPreviewSize();
        textureSize = Utilities.nextPowerOfTwo(Math.max(mPreviewSize.width, mPreviewSize.height));
        mTextureRatio = new TextureRatio(((float)mPreviewSize.width) / (float)textureSize, 1.0f - (float)mPreviewSize.height / (float)textureSize);
        int bytelen = (int)(mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8.0f);
        cameraBytes = new byte[bytelen];
        rgbBytes = new byte[mPreviewSize.width * mPreviewSize.height * 3];
        rgbBuffer = ByteBuffer.wrap(rgbBytes);
        rgbBytes2 = new byte[mPreviewSize.width * mPreviewSize.height * 3];
        rgbBuffer2 = ByteBuffer.wrap(rgbBytes2);
        
        mCamera.addCallbackBuffer(cameraBytes);
        mCamera.setPreviewCallbackWithBuffer(this);
        
        mCamera.startPreview();
        
        mSurfaceView.setVisibility(View.GONE);
        
        System.out.println("camz CameraPreviewSink startPreview");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public void surfaceDestroyed(SurfaceHolder holder) {}

}
