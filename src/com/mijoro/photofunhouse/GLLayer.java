
package com.mijoro.photofunhouse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLSurfaceView.Renderer;
import android.view.SurfaceHolder;

/**
 * This class uses OpenGL ES to render the camera's viewfinder image on the
 * screen. Unfortunately I don't know much about OpenGL (ES). The code is mostly
 * copied from some examples. The only interesting stuff happens in the main
 * loop (the run method) and the onPreviewFrame method.
 */
public class GLLayer extends GLSurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback, Renderer {

    int[] cameraTexture;

    byte[] glCameraFrame = new byte[512 * 512]; // size of a texture must be a
                                                // power of 2

    public int previewFrameWidth = 352;

    public int previewFrameHeight = 288;
    public int textureSize = -1;
    
    private CamLayer camLayer;

    FloatBuffer cubeBuff;

    FloatBuffer texBuff;

    static {
        System.loadLibrary("yuv420sp2rgb");
    }


    /**
     * native function, that converts a byte array from ycbcr420 to RGB
     * 
     * @param in
     * @param width
     * @param height
     * @param textureSize
     * @param out
     */
    private native void yuv420sp2rgb(byte[] in, int width, int height, int textureSize, byte[] out);

    public GLLayer(Context c) {
        super(c);

        // this.setEGLConfigChooser(5, 6, 5, 8, 16, 0);
        this.setRenderer(this);
        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }
    
    public void setCamLayer(CamLayer c) {
        camLayer = c;
    }

    public void onDrawFrame(GL10 gl) {

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        bindCameraTexture(gl);

        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0, 0, 4.2f, 0, 0, 0, 0, 1, 0);

        gl.glNormal3f(0, 0, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 8, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 12, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 16, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 20, 4);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0, 0, 4.2f, 0, 0, 0, 0, 1, 0);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        gl.glClearColor(0, 0, 0, 0);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);

        cubeBuff = makeFloatBuffer(camObjCoord);
        texBuff = makeFloatBuffer(camTexCoords);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, cubeBuff);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, texBuff);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }

    /**
     * Generates a texture from the black and white array filled by the
     * onPreviewFrame method.
     */
    void bindCameraTexture(GL10 gl) {
        synchronized (this) {
            if (cameraTexture == null)  {
                cameraTexture = new int[1];
                gl.glGenTextures(1, cameraTexture, 0);
            }
            int tex = cameraTexture[0];
            gl.glBindTexture(GL10.GL_TEXTURE_2D, tex);
            gl.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGB, textureSize,
                    textureSize, 0, GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE ,
                    null);
                    
                    gl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, 0, 0, previewFrameWidth, previewFrameHeight,
                            GL10.GL_RGB, GL10.GL_UNSIGNED_BYTE, buffer);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        }
    }

    boolean dirty = false;
    ByteBuffer buffer = ByteBuffer.wrap(glCameraFrame);
    /**
     * This method is called if a new image from the camera arrived. The camera
     * delivers images in a yuv color format. It is converted to a black and
     * white image with a size of 256x256 pixels (only a fraction of the
     * resulting image is used). Afterwards Rendering the frame (in the main
     * loop thread) is started by setting the newFrameLock to true.
     */
    public void onPreviewFrame(byte[] yuvs, Camera camera) {
        System.out.println("onPreviewFrame");
        if (textureSize == -1) {
            textureSize = camLayer.textureSize;
            previewFrameHeight = camLayer.previewFrameHeight;
            previewFrameWidth = camLayer.previewFrameWidth;
            glCameraFrame = new byte[previewFrameHeight * previewFrameWidth * 3];
            buffer = ByteBuffer.wrap(glCameraFrame);
        }
        yuv420sp2rgb(yuvs, previewFrameWidth, previewFrameHeight, textureSize, glCameraFrame);
    }

    FloatBuffer makeFloatBuffer(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr);
        fb.position(0);
        return fb;
    }

    final static float camObjCoord[] = new float[] {
            // FRONT
            -2.0f, -1.5f, 2.0f, 2.0f, -1.5f, 2.0f, -2.0f, 1.5f, 2.0f, 2.0f, 1.5f, 2.0f,
            // BACK
            -2.0f, -1.5f, -2.0f, -2.0f, 1.5f, -2.0f, 2.0f, -1.5f, -2.0f, 2.0f, 1.5f, -2.0f,
            // LEFT
            -2.0f, -1.5f, 2.0f, -2.0f, 1.5f, 2.0f, -2.0f, -1.5f, -2.0f, -2.0f, 1.5f, -2.0f,
            // RIGHT
            2.0f, -1.5f, -2.0f, 2.0f, 1.5f, -2.0f, 2.0f, -1.5f, 2.0f, 2.0f, 1.5f, 2.0f,
            // TOP
            -2.0f, 1.5f, 2.0f, 2.0f, 1.5f, 2.0f, -2.0f, 1.5f, -2.0f, 2.0f, 1.5f, -2.0f,
            // BOTTOM
            -2.0f, -1.5f, 2.0f, -2.0f, -1.5f, -2.0f, 2.0f, -1.5f, 2.0f, 2.0f, -1.5f, -2.0f,
    };

    final static float camTexCoords[] = new float[] {
            // Camera preview
            0.0f, 0.0f, 0.9375f, 0.0f, 0.0f, 0.625f, 0.9375f, 0.625f,

            // BACK
            0.9375f, 0.0f, 0.9375f, 0.625f, 0.0f, 0.0f, 0.0f, 0.625f,
            // LEFT
            0.9375f, 0.0f, 0.9375f, 0.625f, 0.0f, 0.0f, 0.0f, 0.625f,
            // RIGHT
            0.9375f, 0.0f, 0.9375f, 0.625f, 0.0f, 0.0f, 0.0f, 0.625f,
            // TOP
            0.0f, 0.0f, 0.9375f, 0.0f, 0.0f, 0.625f, 0.9375f, 0.625f,
            // BOTTOM
            0.9375f, 0.0f, 0.9375f, 0.625f, 0.0f, 0.0f, 0.0f, 0.625f
    };

}
