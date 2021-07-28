#version 150

in vec2 texCoord;
// in vec2 oneTexel;

uniform sampler2D DiffuseSampler;

uniform mat4 SceneProjMat;
uniform mat4 SceneModelViewMat;
uniform vec2 OutSize;
uniform vec3 BHPos;
uniform float BHRadSq;

out vec4 fragColor;

float distSq(vec2 from, vec2 to)
{
	vec2 d = to - from;
	return d.x * d.x + d.y * d.y;
}

vec4 circle(vec2 center, float rad, vec3 color)
{
	float alpha = step(distSq(center.xy, texCoord.xy * OutSize.xy), rad);
	return vec4(color, alpha);
}

void main()
{
	vec4 center = SceneProjMat * SceneModelViewMat * vec4(BHPos, 1.0);
	// vec2 center = BHPos;
	vec4 color = circle(center.xy, BHRadSq, vec3(0.0, 0.0, 0.0));
	vec4 bg = texture(DiffuseSampler, texCoord);
	fragColor = mix(bg, color, color.a);
}