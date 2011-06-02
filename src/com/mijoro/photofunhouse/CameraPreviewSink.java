
package com.mijoro.photofunhouse;

import java.nio.ByteBuffer;

import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.opengl.GLES20;

public class CameraPreviewSink implements Camera.PreviewCallback {
    private Camera mCamera;
    float texRatioHeight = 1.0f;
    float texRatioWidth = 1.0f;
    boolean initialized = false;
    int[] cameraTexture;

    byte[] cameraBytes = new byte[0]; // size of a texture must be a
                                                // power of 2
    
    Size mPreviewSize;
    TextureRatio mTextureRatio;
    public int textureSize = -1;
    
    ByteBuffer buffer = ByteBuffer.wrap(cameraBytes);

    public class TextureRatio {
        public float width, height;
        public TextureRatio(float w, float h) {
            width = w;
            height = h;
        }
    }
    
    public CameraPreviewSink() {
        mCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
        mPreviewSize = p.getPreviewSize();
        textureSize = Utilities.nextPowerOfTwo(Math.max(mPreviewSize.width, mPreviewSize.height));
        mTextureRatio = new TextureRatio(((float)mPreviewSize.width) / (float)textureSize, (float)mPreviewSize.height / (float)textureSize);
        mCamera.setParameters(p);
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
    }

    public TextureRatio getTextureRatio() {
        return mTextureRatio;
    }

    public void onPreviewFrame(byte[] yuvs, Camera camera) {
        if (cameraBytes.length == 0) {
            cameraBytes = new byte[mPreviewSize.width * mPreviewSize.height * 3];
            buffer = ByteBuffer.wrap(cameraBytes);
        }
        GLLayer.decode(yuvs, mPreviewSize.width, mPreviewSize.height, textureSize, cameraBytes);
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
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, textureSize,
                            textureSize, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE ,
                            null);
                    initialized = true;
                }
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mPreviewSize.width, mPreviewSize.height,
                        GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, buffer);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            }
        }
    }

}
