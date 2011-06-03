package com.mijoro.photofunhouse.shaders;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class BulgeShader extends ShaderProgram {
    public BulgeShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderBulge);
    }

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
}
