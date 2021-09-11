#version 150

uniform vec2 ScreenSize;
uniform float GameTime;

uniform float Scale1;
uniform vec3 Offset1;
uniform float Scale2;
uniform vec3 Offset2;
uniform float Color;

in vec4 posMS;
in vec2 texCoord;

out vec4 fragColor;

// skybox size
const float size = 200.0;

// borreal starfield
const int iterations = 16;
const float formuparam = 0.53; // 77

const int volsteps = 4;
const float stepsize = 0.00733;

const float zoom = 1.2700;
const float tile = 0.850;

const float brightness = 0.0007;
const float distfading = 1.75;

float mod289(float x)
{
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x)
{
	return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 perm(vec4 x)
{
	return mod289(((x * 34.0) + 1.0) * x);
}

float noise(vec3 p){
    vec3 a = floor(p);
    vec3 d = p - a;
    d = d * d * (3.0 - 2.0 * d);

    vec4 b = a.xxyy + vec4(0.0, 1.0, 0.0, 1.0);
    vec4 k1 = perm(b.xyxy);
    vec4 k2 = perm(k1.xyxy + b.zzww);

    vec4 c = k2 + a.zzzz;
    vec4 k3 = perm(c);
    vec4 k4 = perm(c + 1.0);

    vec4 o1 = fract(k3 * (1.0 / 41.0));
    vec4 o2 = fract(k4 * (1.0 / 41.0));

    vec4 o3 = o2 * d.z + o1 * (1.0 - d.z);
    vec2 o4 = o3.yw * d.x + o3.xz * (1.0 - d.x);

    return o4.y * d.y + o4.x * (1.0 - d.y);
}

float fractal(in vec3 p, float s, int iMax)
{
	float strength = 7.0 + 0.03 * log(1.e-6);
	float accum = s / 4.0;
	float prev = 0.0;
	float tw = 0.0;

	for (int i = 0; i < iMax; ++i)
	{
		float mag = dot(p, p);
		p = abs(p) / mag + vec3(-0.5, -0.4, -1.5);
		float w = exp(-float(i) / 7.0);
		accum += w * exp(-strength * pow(abs(mag - prev), 2.2));
		tw += w;
		prev = mag;
	}

	return max(0.0, 5.0 * accum / tw - 0.7);
}

float hash13(vec3 p3)
{
	p3  = fract(p3 * .1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

vec3 nrand3(vec2 co)
{
	vec3 a = fract(cos(co.x * 8.3e-3 + co.y) * vec3(1.3e5, 4.7e5, 2.9e5));
	vec3 b = fract(sin(co.x * 0.3e-3 + co.y) * vec3(8.1e5, 1.0e5, 0.1e5));
	vec3 c = mix(a, b, 0.5);
	return c;
}

float contrast(float valImg, float contrast)
{
	return clamp(contrast * (valImg - 0.5) + 0.5, 0.0, 1.0);
}

vec3  contrast(vec3 valImg, float contrast)
{
	return clamp(contrast * (valImg - 0.5) + 0.5, 0.0, 1.0);
}

float gammaCorrection(float imgVal, float gVal)
{
	return pow(imgVal, 1.0 / gVal);
}

vec3  gammaCorrection(vec3 imgVal, float gVal)
{
	return pow(imgVal, vec3(1.0 / gVal));
}

float starField(vec2 p)
{
	vec2 seed = floor(p * 200.0); // ScreenSize.x
	vec3 rnd = nrand3(seed);
	return pow(abs(rnd.x + rnd.y + rnd.z) / 2.93, 9.7);
}

vec3 stars(vec2 UV)
{
	float ratio = 1.0; // ScreenSize.y / ScreenSize.x; = 1 because we are using a skybox square
	//get coords and direction
	vec2 uv = UV * 0.3723 + vec2(0.0, -0.085); //-mouseL;
	uv.y *= ratio;
	vec3 dir = vec3(uv * zoom / ratio, 1.0);

	dir.xz *= mat2(0.803, 0.565, 0.565, 0.803);
	dir.xy *= mat2(0.9935, 0.0998, 0.0998, 0.9935);

    vec3 from = vec3(-0.4299, -0.7817, -0.3568);

	//volumetric rendering
	float s = 0.0902;
	float fade = 0.7;
	float v = 0.0;
	for (int r=0; r<volsteps; r++)
	{
		vec3 p = from + s * dir * -9.9;
		p = abs(vec3(tile) - mod(p, vec3(tile * 2.0))); // tiling fold
		float pa, a = pa = 0.0;

		for (int i=0; i<iterations; i++)
		{
			p = abs(p) / dot(p, p) - formuparam; // the magic formula
			a += abs(length(p) - pa); // absolute sum of average change
			pa = length(p);
		}
		a *= a * a; // add contrast
		v += fade;
		v += s * a * brightness * fade; // coloring based on distance
		fade *= distfading; // distance fading
		s += stepsize;
	}
	v = contrast(v *0.009, 0.95) - 0.05;
	vec3 col = vec3(0.43, 0.57, 0.97) * 1.7 * v;

	col = gammaCorrection(col, 0.7);

	return col; // + vec3(0.67, 0.83, 0.97) * vec3(starField(UV)) * 0.9;
}

void main()
{
	// fragColor = vec4(stars(texCoord / 4.0), 1.0);
	vec3 starcolor = vec3(stars(texCoord / 4.0));

	vec3 uv = posMS.xyz / size * 2.0;
	vec3 p = uv / Scale1 + Offset1;

	float freqs[4];
	freqs[0] = noise(vec3( 0.01*100.0, 0.25 , Color) );
	freqs[1] = noise(vec3( 0.07*100.0, 0.25 , Color) );
	freqs[2] = noise(vec3( 0.15*100.0, 0.25 , Color) );
	freqs[3] = noise(vec3( 0.30*100.0, 0.25 , Color) );

	float t = fractal(p, freqs[2], 26);
	float v = (1.0 - exp((abs(uv.x) - 1.0) * 6.0)) * (1.0 - exp((abs(uv.y) - 1.0) * 6.0)) * (1.0 - exp((abs(uv.z) - 1.0) * 6.0));

	// Second layer
	vec3 p2 = uv / Scale2 + Offset2;

	float t2 = fractal(p2, freqs[3], 18);
	vec3 c2 = mix(0.4, 1.0, v) * vec3(1.3 * t2 * t2 * t2, 1.8  * t2 * t2 , t2 * freqs[0]); // t2
		
	// vec3 seed = p.xyz;	
	// seed = floor(seed * ScreenSize.x / 4.0);
	// float rnd = hash13(seed);
	// vec3 starcolor = vec3(pow(rnd, 40.0));

	fragColor = vec4(mix(freqs[3] - 0.3, 1.0, v) * vec3(1.5 * freqs[2] * t * t * t, 1.2 * freqs[1] * t * t, freqs[3] * t) + c2 + starcolor, 1.0);
}