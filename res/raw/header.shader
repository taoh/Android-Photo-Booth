precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D sTexture;
uniform vec2 uSize; // The size of the top left corner of the actual image in the texture.  Dimensions should be normalized between 0 and these values.
uniform float uTime;
vec2 norm(vec2 inSize) {
  return inSize / uSize;
}
vec2 denorm(vec2 inSize) {
  return inSize * uSize;
}