package com.mijoro.photofunhouse.shaders;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class KaleidomirrorShader extends ShaderProgram {

    public KaleidomirrorShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderKaleidoMirror);
    }
    
    private final String mFragmentShaderKaleidoMirror =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  vec2 fliph = vec2(1.0 - normalized.x, normalized.y);\n" +
        "  vec2 flipy = vec2(normalized.x, 1.0-normalized.y);\n" +
        "  vec2 flipboth = vec2(1.0 - normalized.x, 1.0 - normalized.y);\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized)) / 2.0 + \n" +
        "                 texture2D(sTexture, denorm(fliph)) / 2.0 + \n" +
        "                 texture2D(sTexture, denorm(flipy)) / 2.0 + \n" +
        "                 texture2D(sTexture, denorm(flipboth)) / 2.0;\n" +
        "}\n";
}

