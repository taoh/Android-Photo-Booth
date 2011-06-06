float rand(float co){
    return fract(sin(co) * 43758.5453);
}
void main() {
  vec2 normalized = norm(vTextureCoord);
  float level = floor(normalized.x / 0.1);
  normalized.y += rand(level) / 3.0 - 0.25;
  gl_FragColor = texture2D(sTexture, denorm(normalized));
}
 