
package com.mijoro.photofunhouse;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class CameraPreviewSink implements Camera.PreviewCallback {
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

    public class TextureRatio {
        public float width, height;
        public TextureRatio(float w, float h) {
            width = w;
            height = h;
        }
    }
    
    public CameraPreviewSink() {
        initCamera(CameraInfo.CAMERA_FACING_FRONT);
    }
    
    public void switchCamera() {
        int newID = CameraInfo.CAMERA_FACING_FRONT == mCameraId ? 
                CameraInfo.CAMERA_FACING_BACK : CameraInfo.CAMERA_FACING_FRONT;
        initCamera(newID);
    }
    
    public boolean isFrontFacing() {
        return mCameraId == CameraInfo.CAMERA_FACING_FRONT;
    }
    
    private void initCamera(int cameraId) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        mCameraId = cameraId;
        mCamera = Camera.open(cameraId);
        
        Camera.Parameters p = mCamera.getParameters();
        if (cameraId == CameraInfo.CAMERA_FACING_BACK) p.setRotation(270);
        System.out.println("Preview framerate: " + p.getPreviewFrameRate());
        p.setPreviewFormat(ImageFormat.NV21);
        Size lowestSetting = p.getSupportedPreviewSizes().get(1);
        p.setPreviewSize(lowestSetting.width, lowestSetting.height);
        mPreviewSize = lowestSetting;
        textureSize = Utilities.nextPowerOfTwo(Math.max(mPreviewSize.width, mPreviewSize.height));
        mTextureRatio = new TextureRatio(((float)mPreviewSize.width) / (float)textureSize, 1.0f - (float)mPreviewSize.height / (float)textureSize);
        mCamera.setParameters(p);
        int bytelen = (int)(mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8.0f);
        cameraBytes = new byte[bytelen];
        rgbBytes = new byte[mPreviewSize.width * mPreviewSize.height * 3];
        rgbBuffer = ByteBuffer.wrap(rgbBytes);
        rgbBytes2 = new byte[mPreviewSize.width * mPreviewSize.height * 3];
        rgbBuffer2 = ByteBuffer.wrap(rgbBytes2);
        
        mCamera.addCallbackBuffer(cameraBytes);
        mCamera.startPreview();
        mCamera.setPreviewCallbackWithBuffer(this);
        
        cameraTexture = null;
        initialized = false;
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

}
