void main(void)
{
    vec2 uv;

    vec2 p = -1.0 + 2.0 * denorm(gl_FragCoord.xy) / 4.0;
    float a = atan(p.y,p.x);
    float r = sqrt(dot(p,p));
    float s = r * (1.0+0.8*cos(uTime*1.0));

    uv.x =          .02*p.y+.03*cos(-uTime+a*3.0)/s;
    uv.y = .1*uTime +.02*p.x+.03*sin(-uTime+a*3.0)/s;

    float w = .9 + pow(max(1.5-r,0.0),4.0);

    w*=0.6+0.4*cos(uTime+3.0*a);

    vec3 col =  texture2D(sTexture,uv).xyz;

    gl_FragColor = vec4(col*w,1.0);
}