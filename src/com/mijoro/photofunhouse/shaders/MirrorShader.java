package com.mijoro.photofunhouse.shaders;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class MirrorShader extends ShaderProgram {
    public MirrorShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderMirror);
    }

    private final String mFragmentShaderMirror =
        PROGRAM_HEADER +
        "void main() {\n" +
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  if (normalized.y < 0.5) {normalized.y = 1.0 - normalized.y;}\n" +
        "  gl_FragColor = texture2D(sTexture, denorm(normalized));\n" +
        "}\n";
}
