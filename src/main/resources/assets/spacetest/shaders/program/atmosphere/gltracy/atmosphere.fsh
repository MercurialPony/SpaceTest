#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;
uniform vec3 Center;
uniform float PlanetRadius;
uniform float AtmosphereRadius;
uniform vec3 DirToSun;

in vec2 texCoord;

out vec4 fragColor;

const float DensityFalloff = 3.0;
const int IN_SCATTERING_POINTS = 10;
const int OPTICAL_DENSITY_POINTS = 10;

const int NUM_OUT_SCATTER = 8;
const int NUM_IN_SCATTER = 80;

#define PI 3.14159265359
#define MAX_FLOAT 1000000

vec3 playerSpace(vec2 uv, float depth)
{
	vec3 ndc = vec3(uv, depth) * 2.0 - 1.0;
	vec4 posCS = vec4(ndc, 1.0);
	vec4 posVS = ProjInverseMat * posCS;
	posVS /= posVS.w;
	vec4 posPS = ViewInverseMat * posVS;
	return posPS.xyz;
}

// Written by GLtracy

vec2 intersectSphere(vec3 rayOrigin, vec3 rayDir, float radius)
{
	float b = dot(rayOrigin, rayDir);
	float c = dot(rayOrigin, rayOrigin) - radius * radius;

	float d = b * b - c;
	if ( d > 0.0 )
	{
		d = sqrt(d);
		float near = max(0.0, -b - d);
		float far = -b + d;
		if(far >= 0)
			return vec2(near, far);
	}

	return vec2(MAX_FLOAT, -MAX_FLOAT);
}

float miePhase(float g, float c, float cc)
{
	float gg = g * g;

	float a = (1.0 - gg) * (1.0 + cc);

	float b = 1.0 + gg - 2.0 * g * c;
	b *= sqrt(b);
	b *= 2.0 + gg;	

	return (3.0 / 8.0 / PI) * a / b;
}

float rayleighPhase(float cc)
{
	return (3.0 / 16.0 / PI) * (1.0 + cc);
}

float density(vec3 pos, float planetRadius, float atmosphereRadius, float ph)
{
	return exp(-max((length(pos) - planetRadius) / (atmosphereRadius - planetRadius), 0.0) / ph);
}

float optic(vec3 pos, vec3 q, float planetRadius, float atmosphereRadius, float ph)
{
	vec3 s = (q - pos) / float(NUM_OUT_SCATTER);
	vec3 v = pos + s * 0.5;

	float sum = 0.0;
	for (int i = 0; i < NUM_OUT_SCATTER; ++i)
	{
		sum += density(v, planetRadius, atmosphereRadius, ph);
		v += s;
	}
	sum *= length(s);

	return sum;
}

vec3 inScatter(vec3 rayOrigin, vec3 rayDir, float planetRadius, float atmosphereRadius, vec2 hit, vec3 sunDir)
{
	const float ph_ray = 0.02;
	const float ph_mie = 0.01;

	const vec3 k_ray = vec3(3.8, 13.5, 33.1);
	const vec3 k_mie = vec3(21.0);
	const float k_mie_ex = 1.1;

	vec3 sum_ray = vec3(0.0);
	vec3 sum_mie = vec3(0.0);

	float n_ray0 = 0.0;
	float n_mie0 = 0.0;

	float len = (hit.y - hit.x) / float(NUM_IN_SCATTER);
	vec3 s = rayDir * len;
	vec3 v = rayOrigin + rayDir * (hit.x + len * 0.5);

	for (int i = 0; i < NUM_IN_SCATTER; ++i, v += s)
	{   
		float d_ray = density(v, planetRadius, atmosphereRadius, ph_ray) * len;
		float d_mie = density(v, planetRadius, atmosphereRadius, ph_mie) * len;

		n_ray0 += d_ray;
		n_mie0 += d_mie;

		vec2 f = intersectSphere(v, sunDir, atmosphereRadius);
		vec3 u = v + sunDir * f.y;

		float n_ray1 = optic(v, u, planetRadius, atmosphereRadius, ph_ray);
		float n_mie1 = optic(v, u, planetRadius, atmosphereRadius, ph_mie);

		vec3 att = exp(-( n_ray0 + n_ray1 ) * k_ray - (n_mie0 + n_mie1) * k_mie * k_mie_ex);

		sum_ray += d_ray * att;
		sum_mie += d_mie * att;
	}

	float c  = dot(rayDir, -sunDir);
	float cc = c * c;
	vec3 scatter =
		sum_ray * k_ray * rayleighPhase(cc) +
		sum_mie * k_mie * miePhase(-0.78, c, cc);

	return 10.0 * scatter;
}

void main()
{
	vec4 color = texture(DiffuseSampler, texCoord);
	float depth = texture(DiffuseDepthSampler, texCoord).r;

	vec3 posPS = playerSpace(texCoord, depth);
	float dstToSurface = length(posPS);

	vec3 rayOrigin = CameraPosition - Center;
	vec3 rayDir = normalize(posPS);

	vec2 hitInfo = intersectSphere(rayOrigin, rayDir, AtmosphereRadius);
	hitInfo.y = min(hitInfo.y, dstToSurface);

	if(hitInfo.x <= hitInfo.y)
	{
		vec3 i = inScatter(rayOrigin, rayDir, PlanetRadius, AtmosphereRadius, hitInfo, DirToSun);
		vec3 light = pow(i, vec3(1.0 / 2.2));
		color = vec4(color.xyz * (1.0 - light / 2.0) + light, 1.0);
	}

	fragColor = color;
}
