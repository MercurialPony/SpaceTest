#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;

uniform vec3 Center;
uniform float PlanetRadius;
uniform float AtmosphereHeight;
uniform vec3 SunDirection;
uniform float DepthMultiplier;
uniform float CID;

in vec2 texCoord;

out vec4 fragColor;

const vec3 catm01[6] = vec3[6] (vec3(1.0,0.3,0.0), vec3(1.0,0.96,0.7), vec3(0.0,1.0,0.0), vec3(0.0,1.0,1.0), vec3(0.3,0.76,1.0), vec3(1.0,1.0,1.0));
const vec3 catm02[6] = vec3[6] (vec3(1.0,.99,.94), vec3(1.0,0.96,0.7), vec3(0.0,1.0,0.0), vec3(0.0,1.0,1.0), vec3(0.3,0.76,1.0), vec3(1.0,1.0,1.0));

#define MAX_FLOAT 1000000

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

vec2 sphere(vec3 pos, vec3 ray, float r)
{
    float r2 = r * r;
    float cd = dot(pos, ray);
    vec3 p = ray * cd - pos;
    float t2 = dot(p, p);
    if (t2 >= r2)
        return vec2(-1.0, -1.0);
    float d = sqrt(r2 - t2);
    return cd + vec2(-d, d);
}

float atmosphere(vec3 rayOrigin, vec3 atmoSurface, vec3 lightDir, float planetRadius, float atmoRadius, float atmoDepth)
{
    float wall = sqrt(atmoRadius * atmoRadius - planetRadius * planetRadius);
    float acc = clamp(atmoDepth / wall, 0.0, 1.0);
    acc = acc * pow(clamp(dot(lightDir, normalize(atmoSurface - rayOrigin)), 0.0, 1.0), 0.4);
    acc *= acc;
    acc *= acc;
    
    return acc;
}

vec3 playerSpace(vec2 uv, float depth)
{
	vec3 ndc = vec3(uv, depth) * 2.0 - 1.0;
	vec4 posCS = vec4(ndc, 1.0);
	vec4 posVS = ProjInverseMat * posCS;
	posVS /= posVS.w;
	vec4 posPS = ViewInverseMat * posVS;
	return posPS.xyz;
}

void main()
{
	vec3 color = texture(DiffuseSampler, texCoord).rgb;
	float depth = texture(DiffuseDepthSampler, texCoord).r;

	vec3 posPS = playerSpace(texCoord, depth);
	float dstToSurface = length(posPS);

	vec3 rayOrigin = CameraPosition - Center;
	vec3 rayDir = -normalize(posPS);

	float atmoRadius = PlanetRadius + AtmosphereHeight;

	//

	vec2 hit = sphere(rayOrigin, rayDir, atmoRadius);
	float dstToAtmo = hit.x;
	float dstToAtmoFar = hit.y;

	if(dstToAtmo > 0.0)
	{
		// if intersects planet
		float adepth = dstToAtmoFar > dstToSurface ? (dstToSurface - dstToAtmo) * DepthMultiplier : abs(dot(rayOrigin, rayDir) - dstToAtmo);
		vec3 ssurface = rayDir * dstToAtmo;
		float ap = atmosphere(rayOrigin, ssurface, SunDirection, PlanetRadius, atmoRadius, adepth);
		int id = int(floor(CID));
		color = mix(color, mix(catm01[id], catm02[id], ap), ap);
	}

	fragColor = vec4(color, 1.0);
}
