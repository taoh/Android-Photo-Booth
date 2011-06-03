package com.mijoro.photofunhouse.shaders;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class DuotoneShader extends ShaderProgram {
    public DuotoneShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderDuotone);
    }
    
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
}
