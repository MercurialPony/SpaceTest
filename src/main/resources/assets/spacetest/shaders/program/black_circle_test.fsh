#version 150

in vec2 texCoord;
in vec2 oneTexel;

uniform sampler2D DiffuseSampler;

uniform vec2 InSize;
uniform float RadiusSq;

out vec4 fragColor;

float distSq(vec2 from, vec2 to)
{
	vec2 d = to - from;
	return d.x * d.x + d.y * d.y;
}

vec4 circle(vec2 uv, vec2 pos, float rSq, vec3 color)
{
	float d = distSq(pos, uv) - rSq;
	float t = clamp(d, 0.0, 1.0);
	return vec4(color, 1.0 - t);
}

void main()
{
	vec4 bg = texture(DiffuseSampler, texCoord);

	vec3 black = vec3(0.0, 0.0, 0.0);
	vec4 circle = circle(texCoord, vec2(0.0, 0.0), RadiusSq, black);
	
	fragColor = mix(bg, circle, circle.a);
}