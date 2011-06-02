
package com.mijoro.photofunhouse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView.Renderer;
import android.os.SystemClock;
import android.util.Log;
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
    
    int[] mPrograms = new int[3];

    public int previewFrameWidth = 0;
    public int previewFrameHeight = 0;
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
        setEGLContextClientVersion(2);
        this.setRenderer(this);
        mContext = c;
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);
    }
    
    public void setCamLayer(CamLayer c) {
        camLayer = c;
    }

    public void onDrawFrame(GL10 gl) {
     // Ignore the passed-in GL10 interface, and use the GLES20
        // class's static methods instead.
        GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        bindCameraTexture(gl);

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");
        
        GLES20.glUniform2f(muSizeHandle, texRatioWidth, texRatioHeight);
        
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        checkGlError("glDrawArrays");
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = 1.0f;
      //  Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mProgram = createProgram(mVertexShader, mFragmentShaderPinch);
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get uniform location for uMVPMatrix");
        }
        muSizeHandle = GLES20.glGetUniformLocation(mProgram, "uSize");
        checkGlError("glGetUniformLocation uSize");
        if (muSizeHandle == -1) {
            throw new RuntimeException("Could not get uniform location for uSize");
        }
        
        Matrix.setIdentityM(mMVPMatrix, 0);
    }
    float texRatioHeight = 1.0f;
    float texRatioWidth = 1.0f;
    /**
     * Generates a texture from the black and white array filled by the
     * onPreviewFrame method.
     */
    void bindCameraTexture(GL10 gl) {
        synchronized (this) {
            if (cameraTexture == null)  {
                cameraTexture = new int[1];
                GLES20.glGenTextures(1, cameraTexture, 0);
            }
            int tex = cameraTexture[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
            if (textureSize > -1) {
                if (!initialized) {
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, textureSize,
                            textureSize, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE ,
                            null);
                    initialized = true;
                }
                GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, previewFrameWidth, previewFrameHeight,
                        GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, buffer);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            }
        }
    }
    boolean initialized = false;

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
        if (textureSize == -1) {
            textureSize = camLayer.textureSize;
            previewFrameHeight = camLayer.previewFrameHeight;
            previewFrameWidth = camLayer.previewFrameWidth;
            glCameraFrame = new byte[previewFrameHeight * previewFrameWidth * 3];
            texRatioHeight = 1.0f - (1.0f *previewFrameHeight) / (1.0f * textureSize);
            texRatioWidth = (1.0f * previewFrameWidth) / (1.0f * textureSize);
            setTextureRatio(texRatioWidth, texRatioHeight);
            buffer = ByteBuffer.wrap(glCameraFrame);
        }
        yuv420sp2rgb(yuvs, previewFrameWidth, previewFrameHeight, textureSize, glCameraFrame);
    }
    
    public void setTextureRatio(float width, float height) {
        for (int index : mTriangleHeights) {
            mTriangleVertices.put(index, height);
        }
        for (int index : mTriangleWidths) {
            mTriangleVertices.put(index, width);
        }
    }

    FloatBuffer makeFloatBuffer(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr);
        fb.position(0);
        return fb;
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
            // X,   Y,   Z,  U,    V
            -1.0f, -1.0f, 0, 0.0f, 0.0f,// 0  1  2  3  4
            1.0f, -1.0f, 0, 0.0f, 1.0f,// 5  6  7  8  9
            1.0f, 1.0f, 0, 1.0f, 1.0f,// 10 11 12 13 14

            -1.0f, -1.0f, 0,  0.0f, 0.0f,// 15 16 17 18 19
            -1.0f, 1.0f,  0, 1.0f, 0.0f,// 20 21 22 23 24
            1.0f, 1.0f,  0, 1.0f, 1.0f,// 25 26 27 28 29
    };
    private int[] mTriangleHeights = {4 ,19, 24};
    private int[] mTriangleWidths = {13, 23, 28};

    private FloatBuffer mTriangleVertices;
    
    private final String NORMALIZATION_FUNCTIONS =
        "uniform vec2 uSize;\n\n" + // The size of the top left corner of the actual image in the texture.  Dimensions should be normalized between 0 and these values.
        "vec2 norm(vec2 inSize) {\n" +
        "  return inSize / uSize;\n" +
        "}\n" +
        "vec2 denorm(vec2 inSize) {\n" +
        "  return inSize * uSize;\n" +
        "}\n";

    private final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTextureCoord = vec2(aTextureCoord.x, 1.0-aTextureCoord.y);\n" +
        "}\n";

    private final String mFragmentShaderBulge =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        "void main() {\n" +
        "  vec2 cen = vec2(0.5,0.5) - vTextureCoord.xy;\n" +
        "  vec2 mcen =  0.07*log(length(cen))*normalize(cen);\n" +
        "  gl_FragColor = texture2D(sTexture, vTextureCoord.xy+mcen);\n" +
        "}\n";
    private final String mFragmentShader =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        NORMALIZATION_FUNCTIONS +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  normalized.x = 1.0 - normalized.x;\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized));\n" +
        "}\n";
    
    private final String mFragmentShaderMirror =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        NORMALIZATION_FUNCTIONS +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  if (normalized.x > 0.5) {normalized.x = 1.0 - normalized.x;}\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized));\n" +
        "}\n";

    private final String mFragmentShaderPinch =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        NORMALIZATION_FUNCTIONS +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 normCoord = vec2(2.0) * normalized - vec2(1.0);\n" +
        "  float r = length(normCoord);\n" +
        "  float phi = atan(normCoord.y, normCoord.x);\n" +
        "  r = pow(r, 1.0/ (1.0 - 1.0 * -1.0)) * 0.8;\n" + 
        "  normCoord.x = r* cos(phi);\n" + 
        "  normCoord.y = r* sin(phi);\n" +
        "  vec2 texCoord = (normCoord / 2.0 + 0.5);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(texCoord));\n" +
        "}\n";
    private float[] mMVPMatrix = new float[16];

    private int mProgram;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int muSizeHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private Context mContext;
    private static String TAG = "Photo Funhouse";

}
