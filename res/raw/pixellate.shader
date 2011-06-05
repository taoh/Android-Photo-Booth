void main() {
  vec2 normalized = norm(vTextureCoord);
  vec2 pixelP = vec2(0.0, 0.0);
  float scale = 0.02;
  pixelP.x = floor(normalized.x / scale) * scale;
  pixelP.y = floor(normalized.y / scale) * scale;
  gl_FragColor = texture2D(sTexture, denorm(pixelP));
}
 