
package com.mijoro.photofunhouse;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView.Renderer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class GLLayer extends GLSurfaceView implements Renderer {
    private static final float ANIMATION_DURATION = 600.0f;

    private CameraPreviewSink sink;
    private TextureRatio mTexRatio;
    private int mWidth, mHeight;
    private boolean mSaveNextFrame = false;
    private boolean mOverviewMode = false;
    private float mTime = 0.0f;
    
    private long mAnimationStartTime = 0;
    
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
    public native void yuv420sp2rgb(byte[] in, int width, int height, int textureSize, byte[] out);
    static GLLayer layer;
    // What an ugly hack to make this a static method without having to refactor the jni code :)
    public static void decode(byte[] in, int width, int height, int textureSize, byte[] out) {
        if (layer == null) return;
        layer.yuv420sp2rgb(in, width, height, textureSize, out);
    }

    
    public GLLayer(Context c, AttributeSet attrs) {
        super(c, attrs);
        layer = this;
        setEGLContextClientVersion(2);
        this.setRenderer(this);
        mainQuadVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mainQuadVertices.put(mTriangleVerticesData).position(0);
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mOverviewMode && event.getAction() == MotionEvent.ACTION_UP) {
                    int xindex = (int) (3.0f * event.getX() / (float)getWidth());
                    int yindex = (int) (3.0f * event.getY() / (float)getHeight());
                    int index = 3 * yindex + xindex % 3;
                    if (index >= mPrograms.length) return false;
                    mProgramCounter = index;
                    mProgram = mPrograms[mProgramCounter];
                }
                return false;
            }
        });
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                
            }
        });
    }
    
    public void setCameraPreviewSink(CameraPreviewSink sink) {
        this.sink = sink;
        mTexRatio = sink.getTextureRatio();
        setTextureRatio(mTexRatio.width, mTexRatio.height);
    }
    
    @Override
    public void onPause() {
        sink.onPause();
        super.onPause();
    }
    
    @Override
    public void onResume() {
        sink.onResume();
        super.onResume();
    }
    
    public void toggleOverview() {
        mAnimationStartTime = System.currentTimeMillis();
        mOverviewMode = !mOverviewMode;
    }
    
    public void nextProgram() {
        ++mProgramCounter;
        if (mProgramCounter >= mPrograms.length) mProgramCounter = 0;
        mProgram = mPrograms[mProgramCounter]; 
    }
    public void previousProgram() {
        --mProgramCounter;
        if (mProgramCounter < 0) mProgramCounter = mPrograms.length - 1;
        mProgram = mPrograms[mProgramCounter]; 
    }
    public void saveImage() {
        int w = mWidth;
        int h = mHeight;
        int b[]=new int[w*h];
        int bt[]=new int[w*h];
        IntBuffer ib=IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        //remember, that OpenGL bitmap is incompatible with Android bitmap
        //and so, some correction need
        for(int i=0; i<h; i++) {    
             for(int j=0; j<w; j++)
             {
                  int pix=b[i*w+j];
                  int pb=(pix>>16)&0xff;
                  int pr=(pix<<16)&0x00ff0000;
                  int pix1=(pix&0xff00ff00) | pr | pb;
                  bt[(h-i-1)*w+j]=pix1;
             }
        }                  
        Bitmap sb = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        try {
            FileOutputStream out = new FileOutputStream("/sdcard/1.png");
            sb.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onDrawFrame(GL10 gl) {
        mTime += 0.01f;
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        sink.bindTexture();
        if (!mOverviewMode) {
            Matrix.setIdentityM(mMVPMatrix, 0);
            drawQuad(mainQuadVertices, mMVPMatrix);
        } else {
            long time = System.currentTimeMillis();
            float percent = (time - mAnimationStartTime) / ANIMATION_DURATION;
            if (percent < 1.0f) {
                Matrix.setIdentityM(mMVPMatrix, 0);
                Matrix.scaleM(mMVPMatrix, 0, 1.0f - 0.666f * percent, 1.0f - 0.666f * percent, 1.0f);
                float x = -2.0f + 2.0f * (mProgramCounter % 3);
                float y = 2.0f - 2.0f * (float)(Math.floor(mProgramCounter / 3));
                Matrix.translateM(mMVPMatrix, 0, x*percent, y*percent, 0.0f);
                GLES20.glUseProgram(mProgram);
                drawQuad(mainQuadVertices, mMVPMatrix);
            } else {
                for (int i = 0; i < mPrograms.length; ++i) {
                    Matrix.setIdentityM(mMVPMatrix, 0);
                    Matrix.scaleM(mMVPMatrix, 0, 0.333f, 0.333f, 1.0f);
                    float x = -2.0f + 2.0f * (i % 3);
                    float y = 2.0f - 2.0f * (float)(Math.floor(i / 3));
                    Matrix.translateM(mMVPMatrix, 0, x, y, 0.0f);
                    GLES20.glUseProgram(mPrograms[i]);
                    drawQuad(mainQuadVertices, mMVPMatrix);
                }
            }
        }
        
        if (mSaveNextFrame) {
            saveImage();
            mSaveNextFrame = false;
        }
    }
    
    private void drawQuad(FloatBuffer buffer, float[] mvpMatrix) {
        buffer.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, buffer);
        checkGlError("glVertexAttribPointer maPosition");
        buffer.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, buffer);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");
        if (mTexRatio != null)
            GLES20.glUniform2f(muSizeHandle, mTexRatio.width, mTexRatio.height);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0);
        checkGlError("Before GlDrawArrays");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        checkGlError("glDrawArrays");
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mPrograms = new int[mShaders.length];
        int i = 0;
        for (String fshader : mShaders) {
            mPrograms[i++] = createProgram(mVertexShader, fshader);
        }
        mProgram = mPrograms[0];
        mNormalProgram = createProgram(mVertexShader, mFragmentShaderNormal);
        maTextureHandle = getAttribLoc("aTextureCoord");
        maPositionHandle = getAttribLoc("aPosition");
       
        muMVPMatrixHandle = getUniformLoc("uMVPMatrix");
        muSizeHandle = getUniformLoc("uSize");
        
        Matrix.setIdentityM(mMVPMatrix, 0);
    }
    
    private int getUniformLoc(String name) {
        int handle = GLES20.glGetUniformLocation(mProgram, name);
        checkGlError("glGetUniformLocation " + name);
        if (handle == -1) {
            throw new RuntimeException("Could not get uniform location for " + name);
        }
        return handle;
    }
    
    private int getAttribLoc(String name) {
        int handle = GLES20.glGetAttribLocation(mProgram, name);
        checkGlError("glGetAttribLocation " + name);
        if (handle == -1) {
            throw new RuntimeException("Could not get attrib location for " + name);
        }
        return handle;
    }
    
    public void setTextureRatio(float width, float height) {
        for (int index : mTriangleHeights) {
            mainQuadVertices.put(index, height);
        }
        for (int index : mTriangleWidths) {
            mainQuadVertices.put(index, width);
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
    // Indices of the widths and heights to update in the vertex data buffer in order to set
    // the texture bounds since we need to use a power of two texture, but have non-power-of-two
    // images.
    private int[] mTriangleHeights = {4 ,19, 24};
    private int[] mTriangleWidths = {13, 23, 28};

    private FloatBuffer mainQuadVertices;
    
    private final String PROGRAM_HEADER =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        "uniform vec2 uSize;\n" + // The size of the top left corner of the actual image in the texture.  Dimensions should be normalized between 0 and these values.
        //"uniform float uTime;\n" +
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

    private final String mFragmentShaderCreepy =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 cen = vec2(0.5,0.5) - normalized;\n" +
        "  vec2 mcen =  0.07*log(length(cen))*normalize(cen);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized+mcen));\n" +
        "}\n";
    
    private final String mFragmentShaderTrippy =
        PROGRAM_HEADER +
        "  const float C_PI    = 3.1415;\n" +
        "  const float C_2PI   = 2.0 * C_PI;\n" +
        "  const float C_2PI_I = 1.0 / (2.0 * C_PI);\n" +
        "  const float C_PI_2  = C_PI / 2.0;\n" +
        "  const vec2 Freq = vec2(5.0, 5.0);\n" + 
        "  const vec2 Amplitude = vec2(.05, .05);\n" + 
        "  const float uTime = 0.0;\n" +

        "float normalizeRad(float rad) {" +
        "  rad = rad * C_2PI_I;\n" +
        "  rad = fract(rad);\n" +
        "  rad = rad * C_2PI;\n" +
        
        "  if (rad > C_PI) rad = rad - C_2PI;\n" +
        "  if (rad < -C_PI) rad = rad + C_2PI;\n" +
        "  if (rad > C_PI_2) rad = C_PI - rad;\n" +
        "  if (rad < -C_PI_2) rad = -C_PI - rad;\n" +
        "  return rad;\n" +
        "}" +

        "void main() {\n" +
        "  vec2 perturb;\n" +
        "  vec4 color;\n" + 
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  float rad = (normalized.x + normalized.y - 1.0 + uTime) * Freq.x;\n" +
        "  rad = normalizeRad(rad);\n" +
        "  perturb.x = (rad - (rad * rad * rad / 6.0)) * Amplitude.x;\n" +
        
        "  rad = (normalized.x - normalized.y + uTime) * Freq.y;\n" +
        "  rad = normalizeRad(rad);\n" +
        "  perturb.y = (rad - (rad * rad * rad / 6.0)) * Amplitude.y;\n" +
        "  gl_FragColor = texture2D(sTexture, perturb + denorm(normalized));\n" +
        "}\n";

    private final String mFragmentShaderBulge =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 normCoord = vec2(2.0) * normalized - vec2(1.0);\n" +
        "  float r = length(normCoord);\n" +
        "  float phi = atan(normCoord.y, normCoord.x);\n" +
        "  r = pow(r, 1.4) * 0.8;\n" + 
        "  normCoord.x = r* cos(phi);\n" + 
        "  normCoord.y = r* sin(phi);\n" +
        "  vec2 texCoord = (normCoord / 2.0 + 0.5);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(texCoord));\n" +
        "}\n";
    
   
    private final String mFragmentShaderDuotone =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec4 color = texture2D(sTexture, denorm(normalized));\n" +
        "  vec4 result = vec4(color.r + color.g + color.b / 3.0);\n" +
        "  result = (result.r < 0.2 ) ? vec4(0.0) : vec4(1.0, 0.0, 0.0, 1.0);\n" +
        "  result.a = color.a;\n" +
        "  gl_FragColor = result;\n" +
        "}\n";   
    
    private final String mFragmentShaderNormal =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized));\n" +
        "}\n";
    
    private final String mFragmentShaderInvert =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  gl_FragColor = vec4(1.0) - texture2D(sTexture, denorm(normalized));\n" +
        "}\n";
    
    private final String mFragmentShaderPsychadelic =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec4 color = texture2D(sTexture, denorm(normalized));\n" +
        "  color.r = fract(color.r + .2);\n" +
        "  color.g = fract(color.g + .2);\n" +
        "  color.b = fract(color.b + .2);\n" +
        "  gl_FragColor = color;\n" +
        "}\n";
    
    private final String mFragmentShaderMirror =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  if (normalized.x > 0.5) {normalized.x = 1.0 - normalized.x;}\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized));\n" +
        "}\n";

    private final String mFragmentShaderPinch =
        PROGRAM_HEADER +
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
    private int mNormalProgram;
    private int[] mPrograms;
    private int mProgramCounter = 0;
    private String[] mShaders = {
            mFragmentShaderTrippy,
            mFragmentShaderPsychadelic,
            mFragmentShaderPinch,
            mFragmentShaderDuotone,
            mFragmentShaderInvert,
            mFragmentShaderMirror,
            mFragmentShaderBulge
    };
    private int muMVPMatrixHandle;
    private int muSizeHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private static String TAG = "Photo Funhouse";
}
