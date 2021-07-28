#version 150

in vec4 posMS;

uniform sampler2D DiffuseSampler;
uniform sampler2D Sampler0;
uniform vec2 ScreenSize;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform vec3 CameraPosition;

uniform float GameTime;

uniform vec3 Center;
uniform float Radius;
uniform float DiscWidth;
uniform float DiscOuterRadius;
uniform float DiscInnerRadius;
uniform vec3 DiscDirection;
uniform float DiscSpeed;
uniform vec4 DiscColor;
uniform float DopplerBeamingFactor;
uniform float HueRadius;
uniform float HueShiftFactor;
uniform float SSRadius;
uniform float G;

out vec4 fragColor;

const vec2 tilingScale = vec2(4, 1);

// FIXME adjust dynamically based on size
const float steps = 512;
const float stepSize = 0.4;

#define maxFloat 1000000
#define PI 3.1415926535897932384626433832795

/*
 * Shader based on https://kelvinvanhoorn.wordpress.com/2021/04/20/supermassive-black-hole-tutorial/#1-shader-setup
 */


// Based upon https://viclw17.github.io/2018/07/16/raytracing-ray-sphere-intersection/#:~:text=When%20the%20ray%20and%20sphere,equations%20and%20solving%20for%20t.
// Returns dstToSphere, dstThroughSphere
// If inside sphere, dstToSphere will be 0
// If ray misses sphere, dstToSphere = max float value, dstThroughSphere = 0
// Given rayDir must be normalized
vec2 intersectSphere(vec3 rayOrigin, vec3 rayDir, vec3 center, float radius)
{
	vec3 offset = rayOrigin - center;
	const float a = 1;
	float b = 2 * dot(offset, rayDir);
	float c = dot(offset, offset) - radius * radius;

	float discriminant = b * b - 4 * a * c;
	// No intersections: discriminant < 0
    // 1 intersection: discriminant == 0
    // 2 intersections: discriminant > 0
	if(discriminant > 0)
	{
		float s = sqrt(discriminant);
		float dstToSphereNear = max(0, (-b - s) / (2 * a));
		float dstToSphereFar = (-b + s) / (2 * a);

		if(dstToSphereFar >= 0)
			return vec2(dstToSphereNear, dstToSphereFar - dstToSphereNear);
	}
	// Ray did not intersect sphere
	return vec2(maxFloat, 0);
}

// Based upon https://mrl.cs.nyu.edu/~dzorin/rend05/lecture2.pdf
vec2 intersectInfiniteCylinder(vec3 rayOrigin, vec3 rayDir, vec3 cylinderOrigin, vec3 cylinderDir, float cylinderRadius)
{
	vec3 a0 = rayDir - dot(rayDir, cylinderDir) * cylinderDir;
	float a = dot(a0, a0);
 
	vec3 dP = rayOrigin - cylinderOrigin;
	vec3 c0 = dP - dot(dP, cylinderDir) * cylinderDir;
	float c = dot(c0, c0) - cylinderRadius * cylinderRadius;
 
	float b = 2 * dot(a0, c0);
 
	float discriminant = b * b - 4 * a * c;
 
	if (discriminant > 0)
	{
		float s = sqrt(discriminant);
		float dstToNear = max(0, (-b - s) / (2 * a));
		float dstToFar = (-b + s) / (2 * a);

		if (dstToFar >= 0)
			return vec2(dstToNear, dstToFar - dstToNear);
	}
	return vec2(maxFloat, 0);
}

// Based upon https://mrl.cs.nyu.edu/~dzorin/rend05/lecture2.pdf
float intersectInfinitePlane(vec3 rayOrigin, vec3 rayDir, vec3 planeOrigin, vec3 planeDir)
{
	float a = 0;
	float b = dot(rayDir, planeDir);
	float c = dot(rayOrigin, planeDir) - dot(planeDir, planeOrigin);

	float discriminant = b * b - 4 * a*c;

	return -c / b;
}

// Based upon https://mrl.cs.nyu.edu/~dzorin/rend05/lecture2.pdf
float intersectDisc(vec3 rayOrigin, vec3 rayDir, vec3 p1, vec3 p2, vec3 discDir, float outerRadius, float innerRadius)
{
	float discDst = maxFloat;
	vec2 cylinderIntersection = intersectInfiniteCylinder(rayOrigin, rayDir, p1, discDir, outerRadius);
	float cylinderDst = cylinderIntersection.x;

	if(cylinderDst < maxFloat)
	{
		float finiteC1 = dot(discDir, rayOrigin + rayDir * cylinderDst - p1);
		float finiteC2 = dot(discDir, rayOrigin + rayDir * cylinderDst - p2);

		// Ray intersects with edges of the cylinder/disc
		if(finiteC1 > 0 && finiteC2 < 0 && cylinderDst > 0)
			discDst = cylinderDst;
		else
		{
			float outerRadiusSqr = outerRadius * outerRadius;
			float innerRadiusSqr = innerRadius * innerRadius;

			float p1Dst = max(intersectInfinitePlane(rayOrigin, rayDir, p1, discDir), 0);
			vec3 q1 = rayOrigin + rayDir * p1Dst;
			float p1q1DstSqr = dot(q1 - p1, q1 - p1);

			// Ray intersects with lower plane of cylinder/disc
			if(p1Dst > 0 && p1q1DstSqr < outerRadiusSqr && p1q1DstSqr > innerRadiusSqr && p1Dst < discDst)
				discDst = p1Dst;

			float p2Dst = max(intersectInfinitePlane(rayOrigin, rayDir, p2, discDir), 0);
			vec3 q2 = rayOrigin + rayDir * p2Dst;
			float p2q2DstSqr = dot(q2 - p2, q2 - p2);

			// Ray intersects with upper plane of cylinder/disc
			if(p2Dst > 0 && p2q2DstSqr < outerRadiusSqr && p2q2DstSqr > innerRadiusSqr && p2Dst < discDst)
				discDst = p2Dst;
		}
	}

	return discDst;
}

float remap(float v, float minOld, float maxOld, float minNew, float maxNew)
{
	return minNew + (v - minOld) * (maxNew - minNew) / (maxOld - minOld);
}

vec2 discUV(vec3 planarDiscPos, vec3 discDir, vec3 centre, float radius)
{
	vec3 planarDiscPosNorm = normalize(planarDiscPos);
	float sampleDist01 = length(planarDiscPos) / radius;

	vec3 tangentTestVector = vec3(1, 0, 0);
	if(abs(dot(discDir, tangentTestVector)) >= 1)
		tangentTestVector = vec3(0, 1, 0);

	vec3 tangent = normalize(cross(discDir, tangentTestVector));
	vec3 biTangent = cross(tangent, discDir);
	// atan2(x, y) -> atan(y, x)
	float phi = atan(dot(planarDiscPosNorm, biTangent), dot(planarDiscPosNorm, tangent)) / PI;
	phi = remap(phi, -1, 1, 0, 1);

	// Radial distance
	float u = sampleDist01;
	// Angular distance
	float v = phi;

	return vec2(u, v);
}

// Based upon UnityCG.cginc, used in hdrIntensity 
vec3 linearToGammaSpace (vec3 linRGB)
{
    linRGB = max(linRGB, vec3(0.0, 0.0, 0.0));
    // An almost-perfect approximation from http://chilliant.blogspot.com.au/2012/08/srgb-approximations-for-hlsl.html?m=1
    return max(1.055 * pow(linRGB, vec3(0.416666667)) - 0.055, 0.0);
}
 
// Based upon UnityCG.cginc, used in hdrIntensity 
vec3 gammaToLinearSpace (vec3 sRGB)
{
    // Approximate version from http://chilliant.blogspot.com.au/2012/08/srgb-approximations-for-hlsl.html?m=1
    return sRGB * (sRGB * (sRGB * 0.305306011 + 0.682171111) + 0.012522878);
}
 
// Based upon https://forum.unity.com/threads/how-to-change-hdr-colors-intensity-via-shader.531861/
vec3 hdrIntensity(vec3 emissiveColor, float intensity)
{
	// if not using gamma color space, convert from linear to gamma
	// ifndef UNITY_COLORSPACE_GAMMA
	emissiveColor.rgb = linearToGammaSpace(emissiveColor.rgb);
	// endif
	// apply intensity exposure
	emissiveColor.rgb *= pow(2.0, intensity);
	// if not using gamma color space, convert back to linear
	// ifndef UNITY_COLORSPACE_GAMMA
	emissiveColor.rgb = gammaToLinearSpace(emissiveColor.rgb);
	// endif

	return emissiveColor;
}
 
// Based upon Unity's shadergraph library functions
vec3 rgbToHSV(vec3 c)
{
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 0.0000000001;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
 
// Based upon Unity's shadergraph library functions
vec3 hsvToRGB(vec3 c)
{
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0, 1), c.y);
}
 
// Based upon Unity's shadergraph library functions
vec3 rotateAboutAxis(vec3 p, vec3 axis, float rotation)
{
	float s = sin(rotation);
	float c = cos(rotation);
	float one_minus_c = 1 - c;

	axis = normalize(axis);
	// column major in glsl, needed transposing
	mat3 rotMat = mat3(
		one_minus_c * axis.x * axis.x + c         , one_minus_c * axis.x * axis.y + axis.z * s, one_minus_c * axis.z * axis.x - axis.y * s,
		one_minus_c * axis.x * axis.y - axis.z * s, one_minus_c * axis.y * axis.y + c         , one_minus_c * axis.y * axis.z + axis.x * s,
		one_minus_c * axis.z * axis.x + axis.y * s, one_minus_c * axis.y * axis.z - axis.x * s, one_minus_c * axis.z * axis.z + c);
	return rotMat * p;
}

vec3 discColor(vec3 baseColor, vec3 planarDiscPos, vec3 discDir, vec3 cameraPos, float u, float radius)
{
	vec3 newColor = baseColor;

	// Distance intensity fall-off
	float intensity = remap(u, 0, 1, 0.5, -1.2);
	intensity *= abs(intensity);

	// Doppler beaming intensity change
	vec3 rotatePos = rotateAboutAxis(planarDiscPos, discDir, 0.01);
	float dopplerDistance = (length(rotatePos - cameraPos) - length(planarDiscPos - cameraPos)) / radius;
	intensity += dopplerDistance * DopplerBeamingFactor; // * DiscSpeed / 100

	newColor = hdrIntensity(baseColor, intensity);

	// Distance hue shift
	vec3 hueColor = rgbToHSV(newColor);
	float hueShift = clamp(remap(u, HueRadius, 1, 0, 1), 0, 1);
	hueColor.r += hueShift * HueShiftFactor;
	newColor = hsvToRGB(hueColor);

	return newColor;
}

void main()
{
	vec3 posWS = posMS.xyz + Center;

	// Initial ray information
	vec3 rayOrigin = CameraPosition;
	vec3 rayDir = normalize(posWS - rayOrigin);

	vec2 outerSphereIntersection = intersectSphere(rayOrigin, rayDir, Center, Radius);

	// Disc information
	vec3 p1 = Center - 0.5 * DiscWidth * DiscDirection;
	vec3 p2 = Center + 0.5 * DiscWidth * DiscDirection;
	float discRadius = Radius * DiscOuterRadius;
	float innerRadius = Radius * DiscInnerRadius;

	// Raymarching information
	float transmittance = 0;
	float blackHoleMask = 0;
	vec3 samplePos = vec3(maxFloat, 0, 0);
	vec3 currentRayPos = rayOrigin + rayDir * outerSphereIntersection.x;
	vec3 currentRayDir = rayDir;

	// Ray intersects with the outer sphere
	if(outerSphereIntersection.x < maxFloat)
	{
		for (int i = 0; i < steps; ++i)
		{
			vec3 dirToCenter = Center - currentRayPos;
			float dstToCenter = length(dirToCenter);
			dirToCenter /= dstToCenter;

			if(dstToCenter > Radius + stepSize)
				break;
	 
			float force = G / (dstToCenter * dstToCenter);
			currentRayDir = normalize(currentRayDir + dirToCenter * force * stepSize);
		

			// Move ray forward
			currentRayPos += currentRayDir * stepSize;

			float blackHoleDistance = intersectSphere(currentRayPos, currentRayDir, Center, SSRadius * Radius).x;
			if(blackHoleDistance <= stepSize)
			{
				blackHoleMask = 1;
				break;
			}

			// Check for disc intersection nearby
			float discDst = intersectDisc(currentRayPos, currentRayDir, p1, p2, DiscDirection, discRadius, innerRadius);
			if(transmittance < 1 && discDst < stepSize)
			{
				transmittance = 1;
				samplePos = currentRayPos + currentRayDir * discDst;
			}
		}
	}

	vec2 uv = vec2(0, 0);
	vec3 planarDiscPos = vec3(0, 0, 0);
	if(samplePos.x < maxFloat)
	{
		planarDiscPos = samplePos - dot(samplePos - Center, DiscDirection) * DiscDirection - Center;
		uv = discUV(planarDiscPos, DiscDirection, Center, DiscOuterRadius);
		uv.y += GameTime * DiscSpeed;
	}

	float texCol = texture(Sampler0, uv * tilingScale).r;

	vec2 screenUV = gl_FragCoord.xy / ScreenSize.xy;

	// Ray direction projection
	vec3 distortedRayDir = normalize(currentRayPos - rayOrigin);
	vec4 rayUVProjection = ProjMat * ModelViewMat * vec4(distortedRayDir, 0);
	vec2 distortedScreenUV = rayUVProjection.xy * 0.5 + 0.5;

	// Screen and object edge transitions
	float edgeFadex = smoothstep(0, 0.25, 1 - abs(remap(screenUV.x, 0, 1, -1, 1)));
	float edgeFadey = smoothstep(0, 0.25, 1 - abs(remap(screenUV.y, 0, 1, -1, 1)));
	float t = clamp(remap(outerSphereIntersection.y, Radius, 2 * Radius, 0, 1), 0, 1) * edgeFadex * edgeFadey;
	distortedScreenUV = mix(screenUV, distortedScreenUV, t);

	vec3 bgCol = texture(DiffuseSampler, distortedScreenUV).rgb * (1 - blackHoleMask);
	vec3 discCol = discColor(DiscColor.rgb, planarDiscPos, DiscDirection, rayOrigin, uv.x, discRadius);

	transmittance *= texCol * DiscColor.a;
	vec4 col = vec4(mix(bgCol, discCol, transmittance), 1);

	fragColor = col;
}