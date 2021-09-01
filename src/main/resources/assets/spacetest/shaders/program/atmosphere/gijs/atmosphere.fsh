#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;

uniform vec3 Center;
uniform float PlanetRadius;
uniform float AtmosphereFalloff;
uniform float AtmosphereBase;
uniform vec3 LightDirection;
uniform float LightIntensity;
uniform vec3 LightColor;
uniform vec3 ScatteringCoefficients;

in vec2 texCoord;

out vec4 fragColor;

const int DEPTH_SAMPLES = 5;
const int ATMOS_SAMPLES = 10;

vec3 playerSpace(vec2 uv, float depth)
{
	vec3 ndc = vec3(uv, depth) * 2.0 - 1.0;
	vec4 posCS = vec4(ndc, 1.0);
	vec4 posVS = ProjInverseMat * posCS;
	posVS /= posVS.w;
	vec4 posPS = ViewInverseMat * posVS;
	return posPS.xyz;
}

vec2 raySphere(vec3 rayOrigin, vec3 rayDir, vec3 center, float radius)
{
	vec3 offset = rayOrigin - center;
	float a = 1.0;
	float b = 2.0 * dot(offset, rayDir);
	float c = dot(offset, offset) - radius * radius;
	float d = b * b - 4.0 * a * c;

	if(d > 0.0)
	{
		float s = sqrt(d);
		float near = max(0.0, (-b - s) / (2.0 * a));
		float far = (-b + s) / (2.0 * a);

		if(far >= 0.0)
			return vec2(near, far);
	}

	return vec2(1.0, -1.0);
}

float atmosphereDensity(vec3 point, vec3 planetCenter, float planetRadius, float atmosphereFalloff, float atmosphereBase)
{
	float h = distance(point, planetCenter) - planetRadius;
	return exp(-h * atmosphereFalloff) * atmosphereBase;
}

float atmosphereDepth(vec3 ori, vec3 dir, float t1, float t2, vec3 planetCenter, float planetRadius, float atmosphereFalloff, float atmosphereBase)
{
	// midpoint riemann integration
	float density = 0.0;
	float dt = (t2 - t1) / float(DEPTH_SAMPLES);
	float t = t1 + dt * 0.5; // + dt * 0.5 so we are taking midpoints
	for(int i = 0; i < DEPTH_SAMPLES; ++i)
	{
		vec3 x = ori + dir * t;
		density += atmosphereDensity(x, planetCenter, planetRadius, atmosphereFalloff, atmosphereBase) * abs(dt);
		t += dt;
	}
	return density;
}

vec3 calculateLight(vec3 ori, vec3 dir, vec3 planetCenter, float planetRadius, float dstToSurface, float atmosphereFalloff, float atmosphereBase, vec3 lightDir, float lightIntensity, vec3 lightColor, vec3 scatteringCoeffs)
{
	float atmosphereCutoff = planetRadius - log(0.00001) / atmosphereFalloff;

	vec2 hit = raySphere(ori, dir, planetCenter, atmosphereCutoff);

	vec3 col = vec3(0);
	
	// if atmosphere is hit at all, and if it is hit in view
	if(hit.y > hit.x)
	{
		vec3 scatterlight = vec3(0);

		// calculate the actual place where the ray travels through the atmosphere
		// start of atmosphere keeping in mind the ray starts at 0
		float t1 = hit.x;
		// atmosphere interrupted by planet
		float t2 = min(hit.y, dstToSurface);

        // midpoint riemann integration
        float dt = (t2 - t1) / float(ATMOS_SAMPLES);
        float t = t1 + dt * 0.5; // + dt * 0.5 so we are taking midpoints
        for(int i = 0; i < ATMOS_SAMPLES; ++i)
		{
			vec3 x = ori + dir * t;
			hit = raySphere(x, lightDir, planetCenter, atmosphereCutoff);

			// the planet can be in the way of the sun
			// but when I implement this, although the result becomes lot more accurate, 
			// it comes at the cost that I need to increase the ATMOS_SAMPLES by alot to get rid the banding
			// if(true || !p2h || p2t1 < 0.0)
			float sunDepth  = atmosphereDepth(x, lightDir, 0.0, hit.y, planetCenter, planetRadius, atmosphereFalloff, atmosphereBase);
			float viewDepth = atmosphereDepth(ori, dir, t1, t, planetCenter, planetRadius, atmosphereFalloff, atmosphereBase);
			float density   = atmosphereDensity(x, planetCenter, planetRadius, atmosphereFalloff, atmosphereBase);

			vec3 transmittance = exp(-(sunDepth + viewDepth) * scatteringCoeffs);

			scatterlight += lightIntensity * lightColor * transmittance * density * scatteringCoeffs * dt;

			t += dt;
		}

		col = col * exp(-atmosphereDepth(ori, dir, t1, t2, planetCenter, planetRadius, atmosphereFalloff, atmosphereBase) * scatteringCoeffs) + scatterlight;
	}

	return col;
}

void main()
{
	vec3 color = texture(DiffuseSampler, texCoord).rgb;
	float depth = texture(DiffuseDepthSampler, texCoord).r;

	vec3 posPS = playerSpace(texCoord, depth);
	float dstToSurface = length(posPS);

	vec3 rayOrigin = CameraPosition;
	vec3 rayDir = normalize(posPS);

	//

	// scale atmosphere based on planet size
	float atmosphereFalloff = AtmosphereFalloff / PlanetRadius;
	float atmosphereBase = AtmosphereBase / PlanetRadius;
	vec3 light = calculateLight(rayOrigin, rayDir, Center, PlanetRadius, dstToSurface, atmosphereFalloff, atmosphereBase, LightDirection, LightIntensity, LightColor, ScatteringCoefficients);

	fragColor = vec4(color + light, 1.0);
}
