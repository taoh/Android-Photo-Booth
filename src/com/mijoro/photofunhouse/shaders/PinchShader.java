package com.mijoro.photofunhouse.shaders;

import android.opengl.GLES20;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class PinchShader extends ShaderProgram {
    private int muCenterHandle;
    public PinchShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderPinch);
        muCenterHandle = getUniformLoc("uCenter");
    }
    
    
    @Override
    protected void setupExtraVariables(float time, float touchX, float touchY) {
        super.setupExtraVariables(time, touchX, touchY);
        GLES20.glUniform2f(muCenterHandle, touchY, touchX);
    }

    private final String mFragmentShaderPinch =
        PROGRAM_HEADER +
        "uniform vec2 uCenter;\n" +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 normCenter = vec2(2.0) * uCenter - vec2(1.0);\n" +
        "  vec2 normCoord = vec2(2.0) * normalized - vec2(1.0) + normCenter;\n" +
        "  float r = length(normCoord);\n" +
        "  float phi = atan(normCoord.y, normCoord.x);\n" +
        "  r = pow(r, 1.0/ (1.0 - 1.0 * -1.0)) * 0.8;\n" + 
        "  normCoord.x = r* cos(phi);\n" + 
        "  normCoord.y = r* sin(phi);\n" +

        "  vec2 texCoord = ((normCoord / 2.0 + 0.5) - (normCenter / 2.0));\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(texCoord));\n" +
        "}\n";
}
