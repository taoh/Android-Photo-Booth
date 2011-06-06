void main(void)
{
    vec2 p = -1.0 + 2.0 * norm(vTextureCoord);
    vec2 uv;
   
    float a = atan(p.y,p.x);
    float r = sqrt(dot(p,p));

    uv.x = r - .25*uTime;
    uv.y = cos(a*0.2 + 0.2*sin(uTime+7.0*r)) ;

    vec3 col = texture2D(sTexture,denorm(uv / 2.0 + 0.5)).xyz;

    gl_FragColor = vec4(col,1.0);
}