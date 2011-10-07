void main() {
  vec2 normalized = norm(vTextureCoord);
  vec4 color = texture2D(sTexture, denorm(normalized));
  if (color.r > uValue) color.r = 1.0 - color.r;
  if (color.g > uValue) color.g = 1.0 - color.g;
  if (color.b > uValue) color.b = 1.0 - color.b;
  gl_FragColor = color;
}
 