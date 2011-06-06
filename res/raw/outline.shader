void main()
{
float intensity;
vec3 n;
vec4 _color;
  vec2 normalized = norm(vTextureCoord);

_color = texture2D(sTexture, denorm(normalized));
intensity = min(min(_color.r, _color.g), _color.b);
intensity = _color.r + _color.g + _color.b;
if (intensity > 0.98)
	_color = vec4(1.0,1.0,1.0,1.0);
else if (intensity > 0.5)
	_color = vec4(0.8,0.8,0.8,1.0);
else if (intensity > 0.35)
	_color = vec4(0.4,0.4,0.4,1.0);
else
	_color = vec4(0.0,0.0,0.0,1.0);
gl_FragColor = texture2D(sTexture, denorm(normalized)) / intensity;
}