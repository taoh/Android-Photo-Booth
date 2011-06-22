uniform sampler2D tex;
 
const float sampleStrength = 2.2; 
 
void main(void)
{ 
    float sampleDist = uValue;
	vec2 uv = norm(vTextureCoord.xy);
    // 0.5,0.5 is the center of the screen
    // so substracting uv from it will result in
    // a vector pointing to the middle of the screen
    vec2 dir = 0.5 - uv; 

    // calculate the distance to the center of the screen
    float dist = sqrt(dir.x*dir.x + dir.y*dir.y); 
 
    // normalize the direction (reuse the distance)
    dir = dir/dist; 
 
    // this is the original colour of this fragment
    // using only this would result in a nonblurred version
    vec4 color = texture2D(tex,gl_FragCoord.xy) * 4.0; 

    vec4 sum = color;
    uv = denorm(uv);
    sum += texture2D( tex, uv + dir * -0.08 * sampleDist );
    sum += texture2D( tex, uv + dir * -0.05 * sampleDist );
    sum += texture2D( tex, uv + dir * -0.03 * sampleDist );
    sum += texture2D( tex, uv + dir * -0.02 * sampleDist );
    sum += texture2D( tex, uv + dir * -0.01 * sampleDist );
    sum += texture2D( tex, uv + dir * 0.01 * sampleDist );
    sum += texture2D( tex, uv + dir * 0.02 * sampleDist );
    sum += texture2D( tex, uv + dir * 0.03 * sampleDist );
    sum += texture2D( tex, uv + dir * 0.05 * sampleDist );
    sum += texture2D( tex, uv + dir * 0.08 * sampleDist );
 
    // we have taken eleven samples
    sum *= 1.0/14.0;
 
    // weighten the blur effect with the distance to the
    // center of the screen ( further out is blurred more)
    float t = dist * sampleStrength;
    t = clamp( t ,0.0,1.0); //0 &lt;= t &lt;= 1
 
    //Blend the original color with the averaged pixels
    gl_FragColor = mix( color, sum, t );
} 