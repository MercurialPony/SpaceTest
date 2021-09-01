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
uniform vec3 ChunkOffset;

uniform vec3 CameraPosition;
uniform vec3 Corner;
uniform vec2 MaxUV;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

#define PI 3.14159265359
#define TWO_PI 6.28318530718

vec3 toSphere(float u, float v, float radius)
{
	float lon = TWO_PI * u;
	float lat = PI * v;
	float x = radius * cos(lon) * sin(lat);
	float y = radius * sin(lon) * sin(lat);
	float z = radius * cos(lat);
	return vec3(x, y, z);
}

void main()
{
	// player space
	vec3 pos = Position + ChunkOffset;
	// world space
	pos += CameraPosition;
	// plane space
	pos -= Corner;

	float u = pos.x / MaxUV.x;
	float v = pos.z / MaxUV.y;
	float r = pos.y;
	
	pos = toSphere(u, v, r);
	
	// back to player space
	pos -= CameraPosition - Corner;

	gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

	vertexDistance = length((ModelViewMat * vec4(pos, 1.0)).xyz);
	vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
	texCoord0 = UV0;
	normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}
