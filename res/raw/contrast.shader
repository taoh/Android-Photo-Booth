void main() {
  vec2 normalized = norm(vTextureCoord);
  vec4 color = texture2D(sTexture, denorm(normalized));
  color += -0.5;
  color *= 2.0;
  color += 1.0;
  color *= 1.0/2.0;
  gl_FragColor = color;
}
 