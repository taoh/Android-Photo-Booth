void main() {
  vec2 normalized = norm(vTextureCoord);
  vec4 color = texture2D(sTexture, denorm(normalized));
  float oldr = color.r;
  color.r = color.b;
  color.b = color.g;
  color.g = oldr;
  gl_FragColor = color;
}
 