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
uniform float GameTime;

uniform vec3 CameraPosition;
uniform vec3 Corner;
uniform int FaceSize;
uniform int FaceIndex;
uniform float StartRadius;
uniform float RadiusRatio;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 normal;

#define PI 3.14159

/*
vec2 cubeToPlane(vec3 p)
{
	vec3 ap = abs(p);

	// Z
	if (ap.z >= ap.x && ap.z >= ap.y)
	{
		return p.z <= 0.0 ? vec2(p.x / ap.z, p.y / ap.z) : vec2(-p.x / ap.z, p.y / ap.z);
	}
	// X
	if (ap.x >= ap.y && ap.x >= ap.z)
	{
		return p.x > 0.0 ? vec2(p.z / ap.x, p.y / ap.x) : vec2(-p.z / ap.x, p.y / ap.x);
	}
	// Y
	if (ap.y >= ap.x && ap.y >= ap.z)
	{
		return p.y > 0.0 ? vec2(p.x / ap.y, p.z / ap.y) : vec2(p.x / ap.y, -p.z / ap.y);
	}

	return vec2(0.0, 0.0);
}

#define isqrt2 0.70710676908493042

vec2 aux(float s, float t)
{
	float A = 3.0;
	float R = 2.0 * (s * s - t * t);
	float S = sqrt( max(0.0, (A + R) * (A + R) - 8.0 * A * s * s) );
	float s_ = sign(s) * isqrt2 * sqrt(max(0.0, A + R - S));
	float t_ = sign(t) * isqrt2 * sqrt(max(0.0, A - R - S));
	return vec2(s_, t_);
}

vec3 sphereToCubeAdjusted(vec3 p)
{
	p = normalize(p);
	vec3 ap = abs(p);
	float max = max(max(ap.x, ap.y), ap.z);

	if (max == ap.x)
	{
		return vec3(sign(p.x), aux(p.y, p.z));
	}

	if (max == ap.y)
	{
		vec2 a = aux(p.z, p.x);
		return vec3(a.y, sign(p.y), a.x);
	}

	return vec3(aux(p.x, p.y), sign(p.z));
}

float logWithBase(float base, float x)
{
	return log(x) / log(base);
}
*/

vec3 planeToCube(int index, float u, float v)
{
	float uc = 2.0 * u - 1.0;
	float vc = 2.0 * v - 1.0;

	switch (index)
	{
		default: return vec3(uc, vc, -1.0);
		case 1: return vec3(-uc, vc, 1.0);
		case 2: return  vec3(1.0, vc, uc);
		case 3: return  vec3(-1.0, vc, -uc);
		case 4: return  vec3(uc, 1.0, vc);
		case 5: return  vec3(uc, -1.0, -vc);
	}
}

vec3 cubeToSphere(vec3 p)
{
	return normalize(p);
}

// Thanks
// https://catlikecoding.com/unity/tutorials/cube-sphere/
vec3 cubeToSphereAdjusted(vec3 p)
{
	vec3 p2 = p * p;
	return p * sqrt(1.0 - (p2.yxx + p2.zzy) / 2.0 + p2.yxx * p2.zzy / 3.0);
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
	float h = StartRadius * pow(RadiusRatio, pos.y);

	pos = h * cubeToSphereAdjusted(planeToCube(FaceIndex, u, v));

	//pos = mix(pos, pos1, sin(GameTime * 60 * 20) / 2.0 + 0.5);

	// back to player space
	pos += -CameraPosition;

	gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

	//vertexDistance = length((ModelViewMat * vec4(pos, 1.0)).xyz);
	vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
	texCoord0 = UV0;
	//normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}