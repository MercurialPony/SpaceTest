#version 150

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform vec2 OutSize;
uniform vec3 Position;

mat2 rot(float a){
	return mat2(cos(a), -sin(a),
               sin(a), cos(a));
}

// Rodrigues' rotation formula : rotates v around u
vec3 rot(vec3 v, vec3 u, float a){
    float c = cos(a);
    float s = sin(a);
    return v * c + cross(u, v) * s + u * dot(u, v) * (1. - c);
}

const float G = 0.01;
const float c = 100.;

void main()
{
    vec2 uv = texCoord;
    uv -= 0.5;
    uv.x *= OutSize.x / OutSize.y;

    vec3 ro = vec3(0., 0., 0.); // origin of the ray
    vec3 rd = normalize(vec3(uv, 1.)); // inital direction of the ray
    
    // float time = iTime;
    
    vec3 col = vec3(0.); // pixel color
    
    vec4 bh = vec4(Position, 100000.); // black hole (x, y, z, mass)
    
    // calculate the distance to the closest point between the ray and the black hole
    float t = dot(rd, bh.xyz - ro);
    
    vec3 v = ro + rd * t - bh.xyz; // vector between closest point and blackhole
    float r = length(v); // its distance


    // Schwarzfield radius
    float rs = 2. * G * bh.w / (c*c);
    if(r >= rs){
        vec3 nml = normalize(cross(v, rd)); // axis of rotation of the ray
        float a = 4. * bh.w * G / (r * c * c); // angle of deflection caused by gravitational field

        rd = rot(rd, nml, a);

        col = texture(DiffuseSampler, rd.xy + 0.5).rgb;
    }
    
    // Output to screen
    fragColor = vec4(col,1.0);
}