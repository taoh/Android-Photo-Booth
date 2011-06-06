void main() {
  vec2 normalized = norm(vTextureCoord);
  gl_FragColor = texture2D(sTexture, denorm(normalized));
}
 