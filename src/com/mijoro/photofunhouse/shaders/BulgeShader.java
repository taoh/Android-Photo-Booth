package com.mijoro.photofunhouse.shaders;

import android.opengl.GLES20;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class BulgeShader extends ShaderProgram {
    private int muLevelHandle;
    public BulgeShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderBulge);
        muLevelHandle = getUniformLoc("uLevel");
    }
    
    @Override
    protected void setupExtraVariables(float time, float touchX, float touchY) {
        super.setupExtraVariables(time, touchX, touchY);
        GLES20.glUniform1f(muLevelHandle, touchX);
    }

    private final String mFragmentShaderBulge =
        PROGRAM_HEADER +
        "uniform float uLevel;\n" +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 normCoord = vec2(2.0) * normalized - vec2(1.0);\n" +
        "  float r = length(normCoord);\n" +
        "  float phi = atan(normCoord.y, normCoord.x);\n" +
        "  r = pow(r, 1.0 + uLevel) * 0.8;\n" + 
        "  normCoord.x = r* cos(phi);\n" + 
        "  normCoord.y = r* sin(phi);\n" +
        "  vec2 texCoord = (normCoord / 2.0 + 0.5);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(texCoord));\n" +
        "}\n";
}
