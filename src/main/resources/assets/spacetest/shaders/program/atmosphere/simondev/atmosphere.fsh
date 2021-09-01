#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;

uniform vec3 Center;
uniform float PlanetRadius;
uniform float AtmosphereHeight;
uniform vec3 LightDirection;

in vec2 texCoord;

out vec4 fragColor;

vec3 playerSpace(vec2 uv, float depth)
{
	vec3 ndc = vec3(uv, depth) * 2.0 - 1.0;
	vec4 posCS = vec4(ndc, 1.0);
	vec4 posVS = ProjInverseMat * posCS;
	posVS /= posVS.w;
	vec4 posPS = ViewInverseMat * posVS;
	return posPS.xyz;
}

float saturate(float x)
{
	return clamp(x, 0.0, 1.0);
}

vec3 saturate(vec3 x)
{
	return clamp(x, 0.0, 1.0);
}

bool intersectsSphere(
	vec3 rayStart,
	vec3 rayDir,
	vec3 sphereCenter,
	float sphereRadius, 
	out float t0,
	out float t1)
{
	vec3 oc = rayStart - sphereCenter;
	float a = dot(rayDir, rayDir);
	float b = 2.0 * dot(oc, rayDir);
	float c = dot(oc, oc) - sphereRadius * sphereRadius;
	float d =  b * b - 4.0 * a * c;

	// Also skip single point of contact
	if (d <= 0.0) {
		return false;
	}

	float r0 = (-b - sqrt(d)) / (2.0 * a);
	float r1 = (-b + sqrt(d)) / (2.0 * a);

	t0 = min(r0, r1);
	t1 = max(r0, r1);

	return t1 >= 0.0;
}

float softLight(float a, float b)
{
	return (b < 0.5 ?
		(2.0 * a * b + a * a * (1.0 - 2.0 * b)) :
		(2.0 * a * (1.0 - b) + sqrt(a) * (2.0 * b - 1.0)));
}

vec3 softLight(vec3 a, vec3 b)
{
	return vec3(
		softLight(a.x, b.x),
		softLight(a.y, b.y),
		softLight(a.z, b.z));
}

// source: https://github.com/selfshadow/ltc_code/blob/master/webgl/shaders/ltc/ltc_blit.fs
vec3 RRTAndODTFit( vec3 v )\
{
	vec3 a = v * ( v + 0.0245786 ) - 0.000090537;
	vec3 b = v * ( 0.983729 * v + 0.4329510 ) + 0.238081;
	return a / b;
}

// this implementation of ACES is modified to accommodate a brighter viewing environment.
// the scale factor of 1/0.6 is subjective. see discussion in #19621.
vec3 ACESFilmicToneMapping( vec3 color )
{
	// sRGB => XYZ => D65_2_D60 => AP1 => RRT_SAT
	const mat3 ACESInputMat = mat3(
		vec3( 0.59719, 0.07600, 0.02840 ), // transposed from source
		vec3( 0.35458, 0.90834, 0.13383 ),
		vec3( 0.04823, 0.01566, 0.83777 )
	);
	// ODT_SAT => XYZ => D60_2_D65 => sRGB
	const mat3 ACESOutputMat = mat3(
		vec3(  1.60475, -0.10208, -0.00327 ), // transposed from source
		vec3( -0.53108,  1.10813, -0.07276 ),
		vec3( -0.07367, -0.00605,  1.07602 )
	);
	color *= 1.0 / 0.6;
	color = ACESInputMat * color;
	// Apply RRT and ODT
	color = RRTAndODTFit( color );
	color = ACESOutputMat * color;
	// Clamp to [0, 1]
	return saturate( color );
}

vec3 groundFog(
	vec3 rgb,
	float distToPoint,
	vec3 rayOrigin,
	vec3 rayDir,
	vec3 sunDir)
{
	vec3 up = normalize(rayOrigin);

	float skyAmt = dot(up, rayDir) * 0.25 + 0.75;
	skyAmt = saturate(skyAmt);
	skyAmt *= skyAmt;

	vec3 DARK_BLUE = vec3(0.1, 0.2, 0.3);
	vec3 LIGHT_BLUE = vec3(0.5, 0.6, 0.7);
	vec3 DARK_ORANGE = vec3(0.7, 0.4, 0.05);
	vec3 BLUE = vec3(0.5, 0.6, 0.7);
	vec3 YELLOW = vec3(1.0, 0.9, 0.7);

	vec3 fogCol = mix(DARK_BLUE, LIGHT_BLUE, skyAmt);
	float sunAmt = max(dot(rayDir, sunDir), 0.0);
	fogCol = mix(fogCol, YELLOW, pow(sunAmt, 16.0));

	float be = 0.0025;
	float fogAmt = (1.0 - exp(-distToPoint * be));

	// Sun
	sunAmt = 0.5 * saturate(pow(sunAmt, 256.0));

	return mix(rgb, fogCol, fogAmt) + sunAmt * YELLOW;
}

vec3 spaceFog(
	vec3 rgb,
	float distToPoint,
	vec3 rayOrigin,
	vec3 rayDir,
	vec3 sunDir,
	vec3 planetPosition,
	float planetRadius,
	float atmosphereThickness)
{
	float t0 = -1.0;
	float t1 = -1.0;
	// This is a hack since the world mesh has seams that we haven't fixed yet.
	if (intersectsSphere(rayOrigin, rayDir, planetPosition, planetRadius, t0, t1))
		if (distToPoint > t0)
			distToPoint = t0;

	if (!intersectsSphere(rayOrigin, rayDir, planetPosition, planetRadius + atmosphereThickness * 5.0, t0, t1))
		return rgb; // * 0.5

	// Figure out a better way to do this
	float silhouette = saturate((distToPoint - 10000.0) / 10000.0);

	// Glow around planet
	float scaledDistanceToSurface = 0.0;

	// Calculate the closest point between ray direction and planet. Use a point in front of the
	// camera to force differences as you get closer to planet.
	vec3 fakeOrigin = rayOrigin + rayDir * atmosphereThickness;
	float t = max(0.0, dot(rayDir, planetPosition - fakeOrigin) / dot(rayDir, rayDir));
	vec3 pb = fakeOrigin + t * rayDir;

	scaledDistanceToSurface = saturate((distance(pb, planetPosition) - planetRadius) / atmosphereThickness);
	scaledDistanceToSurface = smoothstep(0.0, 1.0, 1.0 - scaledDistanceToSurface);
	//scaledDistanceToSurface = smoothstep(0.0, 1.0, scaledDistanceToSurface);

	float scatteringFactor = scaledDistanceToSurface * silhouette;

	// Fog on surface
	t0 = max(0.0, t0);
	t1 = min(distToPoint, t1);

	vec3 intersectionPoint = rayOrigin + t1 * rayDir;
	vec3 normalAtIntersection = normalize(intersectionPoint);

	float distFactor = exp(-distToPoint * 0.0005 / (atmosphereThickness));
	float fresnel = 1.0 - saturate(dot(-rayDir, normalAtIntersection));
	fresnel = smoothstep(0.0, 1.0, fresnel);

	float extinctionFactor = saturate(fresnel * distFactor) * (1.0 - silhouette);

	// Front/Back Lighting
	vec3 BLUE = vec3(0.5, 0.6, 0.75);
	vec3 YELLOW = vec3(1.0, 0.9, 0.7);
	vec3 RED = vec3(0.035, 0.0, 0.0);

	float NdotL = dot(normalAtIntersection, sunDir);
	float wrap = 0.5;
	float NdotL_wrap = max(0.0, (NdotL + wrap) / (1.0 + wrap));
	float RdotS = max(0.0, dot(rayDir, sunDir));
	float sunAmount = RdotS;

	vec3 backLightingColour = YELLOW * 0.1;
	vec3 frontLightingColour = mix(BLUE, YELLOW, pow(sunAmount, 32.0));

	vec3 fogColour = mix(backLightingColour, frontLightingColour, NdotL_wrap);
	extinctionFactor *= NdotL_wrap;

	// Sun
	float specular = pow((RdotS + 0.5) / (1.0 + 0.5), 64.0);

	fresnel = 1.0 - saturate(dot(-rayDir, normalAtIntersection));
	fresnel *= fresnel;

	float sunFactor = (length(pb) - planetRadius) / (atmosphereThickness * 5.0);
	sunFactor = (1.0 - saturate(sunFactor));
	sunFactor *= sunFactor;
	sunFactor *= sunFactor;
	sunFactor *= specular * fresnel;

	vec3 baseColour = mix(rgb, fogColour, extinctionFactor);
	vec3 litColour = baseColour + softLight(fogColour * scatteringFactor + YELLOW * sunFactor, baseColour);
	vec3 blendedColour = mix(baseColour, fogColour, scatteringFactor);
	blendedColour += blendedColour + softLight(YELLOW * sunFactor, blendedColour);
	return mix(litColour, blendedColour, scaledDistanceToSurface * 0.25);
}

vec3 fog(
	vec3 rgb,
	float distToPoint,
	vec3 rayOrigin,
	vec3 rayDir,
	vec3 sunDir,
	vec3 planetPosition,
	float planetRadius,
	float atmosphereThickness)
{
	float distToPlanet = max(0.0, length(rayOrigin) - planetRadius);

	vec3 groundCol = groundFog(rgb, distToPoint, rayOrigin, rayDir, sunDir);
	vec3 spaceCol = spaceFog(rgb, distToPoint, rayOrigin, rayDir, sunDir, planetPosition, planetRadius, atmosphereThickness);

	float blendFactor = saturate(distToPlanet / (atmosphereThickness * 0.5));

	blendFactor = smoothstep(0.0, 1.0, blendFactor);
	blendFactor = smoothstep(0.0, 1.0, blendFactor);

	return mix(groundCol, spaceCol, blendFactor);
}

void main()
{
	vec3 color = texture(DiffuseSampler, texCoord).rgb;
	float depth = texture(DiffuseDepthSampler, texCoord).r;

	vec3 posPS = playerSpace(texCoord, depth);
	float dstToSurface = length(posPS);

	vec3 rayOrigin = CameraPosition - Center;
	vec3 rayDir = normalize(posPS);

	//

	color = fog(color, dstToSurface, rayOrigin, rayDir, LightDirection, vec3(0.0), PlanetRadius, AtmosphereHeight);

	color = ACESFilmicToneMapping(color);

	fragColor = vec4(color, 1.0);
}
