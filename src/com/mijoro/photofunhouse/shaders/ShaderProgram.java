package com.mijoro.photofunhouse.shaders;

import java.nio.FloatBuffer;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;
import com.mijoro.photofunhouse.Utilities;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

public class ShaderProgram {
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;

    int mProgram;
    private int maPositionHandle;
    private int maTextureHandle;
    private int muMVPMatrixHandle;
    private int muSizeHandle;
    private int muTimeHandle;
    private int muValueHandle;

    private TextureRatio mTexRatio;

    public ShaderProgram(TextureRatio ratio) {
        mTexRatio = ratio;
    }
    
    public ShaderProgram(TextureRatio ratio, String fshader) {
        mTexRatio = ratio;
        initialize(fshader);
    }
    
    public static String buildFShader(Context c, int id) {
        return PROGRAM_HEADER + Utilities.readRawFile(c, id);
    }
    
    public boolean usesValueSlider() {
        return muValueHandle != -1;
    }
    
    protected void initialize(String fShader){
        mProgram = createProgram(mVertexShader, fShader);
        maPositionHandle = getAttribLoc("aPosition");
        maTextureHandle = getAttribLoc("aTextureCoord");
        muMVPMatrixHandle = getUniformLoc("uMVPMatrix");
        muSizeHandle = getUniformLoc("uSize");
        muTimeHandle = getUniformLoc("uTime");
        muValueHandle = getUniformLoc("uValue");
    }
    
    protected void setupExtraVariables(float time, float level) {}
    
    public void drawQuad(FloatBuffer buffer, float[] mvpMatrix, float time, float level) {
        GLES20.glUseProgram(mProgram);
        setupExtraVariables(time, level);
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
        GLES20.glUniform2f(muSizeHandle, mTexRatio.width, mTexRatio.height);
        if (muTimeHandle != -1) GLES20.glUniform1f(muTimeHandle, time);
        if (muValueHandle != -1) GLES20.glUniform1f(muValueHandle, level);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0);
        checkGlError("Before GlDrawArrays");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        checkGlError("glDrawArrays");
    }
    
    protected int getUniformLoc(String name) {
        int handle = GLES20.glGetUniformLocation(mProgram, name);
        checkGlError("glGetUniformLocation " + name);
        return handle;
    }
    
    protected int getAttribLoc(String name) {
        int handle = GLES20.glGetAttribLocation(mProgram, name);
        checkGlError("glGetAttribLocation " + name);
        if (handle == -1) {
            throw new RuntimeException("Could not get attrib location for " + name);
        }
        return handle;
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

    protected int createProgram(String vertexSource, String fragmentSource) {
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
            //throw new RuntimeException(op + ": glError " + error);
        }
    }
    
    protected static final String mVertexShader =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform float uTime;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTextureCoord;\n" +
        "varying vec2 vTextureCoord;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vTextureCoord = vec2(aTextureCoord.x, 1.0-aTextureCoord.y);\n" +
        "}\n";
    
    public static final String PROGRAM_HEADER =
        "precision mediump float;\n" +
        "varying vec2 vTextureCoord;\n" +
        "uniform sampler2D sTexture;\n" +
        "uniform vec2 uSize;\n" + // The size of the top left corner of the actual image in the texture.  Dimensions should be normalized between 0 and these values.
        "uniform float uTime;\n" +
        "uniform float uValue;\n" +
        "vec2 norm(vec2 inSize) {\n" +
        "  return inSize / uSize;\n" +
        "}\n" +
        "vec2 denorm(vec2 inSize) {\n" +
        "  return inSize * uSize;\n" +
        "}\n";
    
    private static String TAG = "ShaderProgram";
}
