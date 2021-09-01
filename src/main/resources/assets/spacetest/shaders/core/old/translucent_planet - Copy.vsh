#version 150

#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform mat4 SceneModelViewMat;
uniform mat4 ModelViewInverseMat;
uniform vec3 CameraPosition;
uniform vec3 Center; // remove?
uniform float Radius;
uniform vec2 MaxUV;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

#define HALF_PI 1.57079632679
#define PI 3.14159265359
#define TWO_PI 6.28318530718

// First solution
vec3 toSphere(float u, float v, float radius)
{
	float lon = TWO_PI * u;
	float lat = PI * v;
	float x = radius * cos(lon) * sin(lat);
	float y = radius * sin(lon) * sin(lat);
	float z = radius * cos(lat);
	return vec3(x, y, z);
}

// Second solution
vec3 toSphere1(vec3 pos)
{
	return normalize(pos) * pos.y;
}

// https://stackoverflow.com/questions/12732590/how-map-2d-grid-points-x-y-onto-sphere-as-3d-points-x-y-z
vec3 toSphere2(vec3 pos)
{
	float radius = pos.y;
	float lon = pos.x / radius;
	float lat = 2 * atan(exp(pos.z / radius)) - HALF_PI; // gudermannian
	float x = radius * cos(lat) * cos(lon);
	float y = radius * cos(lat) * sin(lon);
	float z = radius * sin(lat);
	return vec3(x, y, z);
}

void main()
{
	vec3 posMS = (ModelViewInverseMat * vec4(Position, 1)).xyz;

	float u = (posMS.x / MaxUV.x) * 0.5 + 0.5;
	float v = (posMS.z / MaxUV.y) * 0.5 + 0.5;
	float radius = posMS.y;
	vec4 pos = SceneModelViewMat * vec4(toSphere(u, v, radius), 1);
	// vec4 pos = SceneModelViewMat * vec4(toSphere1(posMS), 1);
	// vec4 pos = vec4(Position, 1);
	// vec4 pos = SceneModelViewMat * vec4(toSphere2(posMS), 1);
	
	gl_Position = ProjMat * ModelViewMat * pos;

	vertexDistance = length((ModelViewMat * pos).xyz);
	vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
	texCoord0 = UV0;
	normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}