const float C_PI    = 3.1415;
const float C_2PI   = 2.0 * C_PI;
const float C_2PI_I = 1.0 / (2.0 * C_PI);
const float C_PI_2  = C_PI / 2.0;
const vec2 Freq = vec2(5.0, 5.0); 
const vec2 Amplitude = vec2(.05, .05); 

float normalizeRad(float rad) {
  rad = rad * C_2PI_I;
  rad = fract(rad);
  rad = rad * C_2PI;

  if (rad > C_PI) rad = rad - C_2PI;
  if (rad < -C_PI) rad = rad + C_2PI;
  if (rad > C_PI_2) rad = C_PI - rad;
  if (rad < -C_PI_2) rad = -C_PI - rad;
  return rad;
}

void main() {
  vec2 perturb;
  vec4 color; 
  vec2 normalized = norm(vTextureCoord);
  float rad = (normalized.x + normalized.y - 1.0 + uTime) * Freq.x;
  rad = normalizeRad(rad);
  perturb.x = (rad - (rad * rad * rad / 6.0)) * Amplitude.x;

  rad = (normalized.x - normalized.y + uTime) * Freq.y;
  rad = normalizeRad(rad);
  perturb.y = (rad - (rad * rad * rad / 6.0)) * Amplitude.y;
  gl_FragColor = texture2D(sTexture, perturb + denorm(normalized));
}
