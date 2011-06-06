void main() {
  vec2 normalized = norm(vTextureCoord);
  normalized = normalized * 2.0 - 1.0;
  gl_FragColor = texture2D(sTexture,denorm(abs(normalized) / 2.0 + 0.5));
}
 