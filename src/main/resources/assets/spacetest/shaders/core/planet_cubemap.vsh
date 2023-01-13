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
uniform int FaceSize;
uniform int FaceIndex;
uniform int SeaLevel;

uniform float GameTime;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

vec3 planeToCube(int index, float u, float v)
{
	float uc = 2.0f * u - 1.0f;
	float vc = 2.0f * v - 1.0f;

	vec3 res;
	switch (index)
	{
		case 0: res = vec3(uc, vc, -1.0); break;
		case 1: res = vec3(-uc, vc, 1.0); break;
		case 2: res =  vec3(1.0, vc, uc); break;
		case 3: res =  vec3(-1.0, vc, -uc); break;
		case 4: res =  vec3(uc, 1.0, vc); break;
		case 5: res =  vec3(uc, -1.0, -vc); break;
	}

	return res;
}

vec3 cubeToSphere(vec3 p, float h)
{
	return normalize(p) * h;
}

// Thanks
// https://catlikecoding.com/unity/tutorials/cube-sphere/
vec3 cubeToSphereAdjusted(vec3 p, float h)
{
	vec3 p2 = p * p;
	return p * h * sqrt(1.0 - (p2.yxx + p2.zzy) / 2.0 + p2.yxx * p2.zzy / 3.0);
}

void main()
{
	// player space
	vec3 pos = Position + ChunkOffset;
	// world space
	pos += CameraPosition;

	// plane space
	pos -= Corner;

	float u = pos.x / FaceSize;
	float v = pos.z / FaceSize;

	// arc length = radius difference
	float w = (FaceSize * 4.0) / (FaceSize * 4.0 - 2.0 * 3.14159);
	float s =  1.0 / (pow(w, SeaLevel + 64.0) - pow(w, SeaLevel + 63.0)); // precalculate
	float h = s * pow(w, pos.y);

	pos = cubeToSphereAdjusted(planeToCube(FaceIndex, u, v), h);

	// back to player space
	pos += -CameraPosition;

	gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

	//vertexDistance = length((ModelViewMat * vec4(pos, 1.0)).xyz);
	vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
	texCoord0 = UV0;
	//normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}