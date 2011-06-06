package com.mijoro.photofunhouse.shaders;

import android.opengl.GLES20;

import com.mijoro.photofunhouse.CameraPreviewSink.TextureRatio;

public class TrippyShader extends ShaderProgram {
    private int muTimeHandle;
    public TrippyShader(TextureRatio ratio) {
        super(ratio);
        initialize(mFragmentShaderTrippy);
        muTimeHandle = getUniformLoc("uTime");
    }
    
    @Override
    protected void setupExtraVariables(float time, float touchX, float touchY) {
        super.setupExtraVariables(time, touchX, touchY);
        GLES20.glUniform1f(muTimeHandle, time * touchX * 2.0f);
    }

    private final String mFragmentShaderTrippy =
        PROGRAM_HEADER +
        "  const float C_PI    = 3.1415;\n" +
        "  const float C_2PI   = 2.0 * C_PI;\n" +
        "  const float C_2PI_I = 1.0 / (2.0 * C_PI);\n" +
        "  const float C_PI_2  = C_PI / 2.0;\n" +
        "  const vec2 Freq = vec2(5.0, 5.0);\n" + 
        "  const vec2 Amplitude = vec2(.05, .05);\n" + 

        "float normalizeRad(float rad) {" +
        "  rad = rad * C_2PI_I;\n" +
        "  rad = fract(rad);\n" +
        "  rad = rad * C_2PI;\n" +
        
        "  if (rad > C_PI) rad = rad - C_2PI;\n" +
        "  if (rad < -C_PI) rad = rad + C_2PI;\n" +
        "  if (rad > C_PI_2) rad = C_PI - rad;\n" +
        "  if (rad < -C_PI_2) rad = -C_PI - rad;\n" +
        "  return rad;\n" +
        "}" +

        "void main() {\n" +
        "  vec2 perturb;\n" +
        "  vec4 color;\n" + 
        "  vec2 normalized = norm(vTextureCoord);\n" +
        "  float rad = (normalized.x + normalized.y - 1.0 + uTime) * Freq.x;\n" +
        "  rad = normalizeRad(rad);\n" +
        "  perturb.x = (rad - (rad * rad * rad / 6.0)) * Amplitude.x;\n" +
        
        "  rad = (normalized.x - normalized.y + uTime) * Freq.y;\n" +
        "  rad = normalizeRad(rad);\n" +
        "  perturb.y = (rad - (rad * rad * rad / 6.0)) * Amplitude.y;\n" +
        "  gl_FragColor = texture2D(sTexture, perturb + denorm(normalized));\n" +
        "}\n";

}
