package com.mijoro.photofunhouse.shaders;

import android.opengl.GLES20;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class DuotoneShader extends ShaderProgram {
    private int muLevelHandle;
    public DuotoneShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderDuotone);
        muLevelHandle = getUniformLoc("uLevel");
    }
    
    @Override
    public boolean usesValueSlider() {
        return true;
    }
    
    @Override
    protected void setupExtraVariables(float time, float level) {
        super.setupExtraVariables(time, level);
        GLES20.glUniform1f(muLevelHandle, level);
    }
    
    private final String mFragmentShaderDuotone =
        PROGRAM_HEADER +
        "uniform float uLevel;\n" +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec4 color = texture2D(sTexture, denorm(normalized));\n" +
        "  vec4 result = vec4(color.r + color.g + color.b / 3.0);\n" +
        "  result = (result.r < uLevel ) ? vec4(0.0) : vec4(1.0, 0.0, 0.0, 1.0);\n" +
        "  result.a = color.a;\n" +
        "  gl_FragColor = result;\n" +
        "}\n"; 
}
