package com.mijoro.photofunhouse.shaders;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class InverseShader extends ShaderProgram {
    public InverseShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderInvert);
    }
    
    private final String mFragmentShaderInvert =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  gl_FragColor = vec4(1.0) - texture2D(sTexture, denorm(normalized));\n" +
        "}\n";
}
