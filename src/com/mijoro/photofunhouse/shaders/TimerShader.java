package com.mijoro.photofunhouse.shaders;

import android.opengl.GLES20;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class TimerShader extends ShaderProgram {
    private int muTimeHandle;

    public TimerShader(TextureRatio ratio, String fshader) {
        super(ratio, fshader);
        muTimeHandle = getUniformLoc("uTime");
    }
    
    @Override
    protected void setupExtraVariables(float time, float touchX, float touchY) {
        super.setupExtraVariables(time, touchX, touchY);
        GLES20.glUniform1f(muTimeHandle, time * touchX * 2.0f);
    }

}
