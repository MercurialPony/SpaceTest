#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;

uniform vec3 Center;
uniform float PlanetRadius;
uniform float AtmosphereHeight;
uniform vec3 DayColor0;
uniform vec3 DayColor1;
uniform vec3 NightColor0;
uniform vec3 NightColor1;
uniform vec3 SunDirection;
uniform float Density;
uniform float AttenuationDistance;

in vec2 texCoord;

out vec4 fragColor;

vec2 ray_sphere(vec3 center, float radius, vec3 ray_origin, vec3 ray_dir)
{
	float t = max(dot(center - ray_origin, ray_dir), 0.0);
	float y = length(center - (ray_origin + ray_dir * t));

	float x = sqrt(max(radius * radius - y * y, 0.0));
	return vec2(t - x, t + x);
}

float ray_plane(vec3 plane_pos, vec3 plane_dir, vec3 ray_origin, vec3 ray_dir)
{
	float dp = dot(plane_dir, ray_dir);
	return dot(plane_pos - ray_origin, plane_dir) / (dp + 0.0001);
}

// atmo_factor, light_factor
vec2 get_atmo_factor(vec3 ray_origin, vec3 ray_dir, vec3 planet_center, float u_planet_radius, float u_atmosphere_height, float t_begin, float t_end, vec3 sun_dir, float u_attenuation_distance, float u_density)
{
	int steps = 16;
	float inv_steps = 1.0 / float(steps);
	float step_len = (t_end - t_begin) * inv_steps;
	vec3 stepv = step_len * ray_dir;
	vec3 pos = ray_origin + ray_dir * t_begin;
	float distance_from_ray_origin = t_begin;
	float attenuation_distance_inv = 1.0 / u_attenuation_distance;

	float factor = 1.0;
	float light_sum = 0.0;

	// TODO Some stuff can be optimized
	for (int i = 0; i < steps; ++i) {
		float d = distance(pos, planet_center);
		vec3 up = (pos - planet_center) / d;
		float sd = d - u_planet_radius;
		float h = clamp(sd / u_atmosphere_height, 0.0, 1.0);
		float y = 1.0 - h;
		
		float density = y * y * y * u_density;
		
		density *= min(1.0, attenuation_distance_inv * distance_from_ray_origin);
		distance_from_ray_origin += step_len;

		float light = clamp(1.2 * dot(sun_dir, up) + 0.5, 0.0, 1.0);
		light = light * light;
		
		light_sum += light * inv_steps;
		factor *= (1.0 - density * step_len);
		pos += stepv;
	}

	return vec2(1.0 - factor, light_sum);
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
	vec4 color = texture(DiffuseSampler, texCoord);
	float depth = texture(DiffuseDepthSampler, texCoord).r;

	vec3 posPS = playerSpace(texCoord, depth);
	float dstToSurface = length(posPS);

	vec3 rayOrigin = CameraPosition;
	vec3 rayDir = normalize(posPS);

	float atmosphereRadius = PlanetRadius + AtmosphereHeight;
	vec2 rs_atmo = ray_sphere(Center, atmosphereRadius, rayOrigin, rayDir);

	if(rs_atmo.x <= rs_atmo.y)
	{
		float t_begin = max(rs_atmo.x, 0.0);
		float t_end = max(rs_atmo.y, 0.0);
		t_end = min(t_end, dstToSurface);

		vec2 factors = get_atmo_factor(rayOrigin, rayDir, Center, PlanetRadius, AtmosphereHeight, t_begin, t_end, normalize(SunDirection), AttenuationDistance, Density);
		float atmo_factor = factors.x;
		float light_factor = factors.y;

		vec3 day_col = mix(DayColor0, DayColor1, atmo_factor);
		vec3 night_col = mix(NightColor0, NightColor1, atmo_factor);

		vec3 col = mix(night_col, day_col, clamp(light_factor * 2.0, 0.0, 1.0));

		float alpha = clamp(atmo_factor, 0.0, 1.0);
		fragColor = vec4(color.rgb * (1.0 - alpha) + col * alpha, 1.0);
	}
	else
		fragColor = color;
}
