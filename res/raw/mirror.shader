void main() {
  vec2 normalized = norm(vTextureCoord);
  if (normalized.y < 0.5) {normalized.y = 1.0 - normalized.y;}
  gl_FragColor = texture2D(sTexture, denorm(normalized));
}