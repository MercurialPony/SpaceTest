#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;

uniform vec3 CameraPosition;
uniform mat4 ProjInverseMat;
uniform mat4 ViewInverseMat;
uniform vec3 Center;
uniform vec3 LightDirection;
uniform vec3 LightIntensity;
uniform float PlanetRadius;
uniform float AtmosphereRadius;
uniform vec3 BetaRay;
uniform vec3 BetaMie;
uniform vec3 BetaAmbient;
uniform float BetaE;
uniform float G;
uniform float HeightRay;
uniform float HeightMie;
uniform float DensityMultiplier;

in vec2 texCoord;

out vec4 fragColor;

#define PI 3.14159265359
#define MAX_FLOAT 1000000

vec2 ray_sphere(vec3 rayOrigin, vec3 rayDir, float radius)
{
	float b = dot(rayOrigin, rayDir);
	float c = dot(rayOrigin, rayOrigin) - radius * radius;

	float d = b * b - c;
	if (d > 0.0)
	{
		d = sqrt(d);
		float near = max(0.0, -b - d);
		float far = -b + d;
		if(far >= 0)
			return vec2(near, far);
	}

	return vec2(MAX_FLOAT, -MAX_FLOAT);
}

vec4 calculate_scattering(
	vec3 start,					// the start of the ray (the camera position)
	vec3 dir,					// the normalized direction of the ray (the camera vector)
	float max_dist,				// the maximum distance the ray can travel (because something is in the way, like an object)
	vec3 light_dir,				// the direction of the light
	vec3 light_intensity,		// how bright the light is, affects the brightness of the atmosphere
	float planet_radius,		// the radius of the planet
	float atmo_radius,			// the radius of the atmosphere
	vec3 beta_ray,				// the amount rayleigh scattering scatters the colors (for earth: causes the blue atmosphere)
	vec3 beta_mie,				// the amount mie scattering scatters colors
	vec3 beta_ambient,			// the amount of scattering that always occurs, can help make the back side of the atmosphere a bit brighter
	float beta_e,				// exponent, helps setting really small values of beta_ray, mie and ambient, as in beta_x * pow(10.0, beta_e) 
	float g,					// the direction mie scatters the light in (like a cone). closer to -1 means more towards a single direction
	float height_ray,			// how high do you have to go before there is no rayleigh scattering?
	float height_mie,			// the same, but for mie
	float density_multiplier,	// how much extra the atmosphere blocks light
	int steps_i,				// the amount of steps along the 'primary' ray, more looks better but slower
	int steps_l					// the amount of steps along the light ray, more looks better but slower
) {
	// calculate the start and end position of the ray, as a distance along the ray
	// we do this with a ray sphere intersect
	vec2 ray_length = ray_sphere(start, dir, atmo_radius);

	// if the ray did not hit the atmosphere, return a black color
	if (ray_length.x > ray_length.y)
		return vec4(0.0);

	// prevent the mie glow from appearing if there's an object in front of the camera
	bool allow_mie = max_dist > ray_length.y;

	// make sure the ray is no longer than allowed
	ray_length.y = min(ray_length.y, max_dist);

	// get the step size of the ray
	float step_size_i = (ray_length.y - ray_length.x) / float(steps_i);

	// helper for beta_e and mie
	float e = pow(10.0, beta_e);

	// next, set how far we are along the ray, so we can calculate the position of the sample
	// if the camera is outside the atmosphere, the ray should start at the edge of the atmosphere
	// if it's inside, it should start at the position of the camera
	// the min statement makes sure of that
	float ray_pos_i = ray_length.x;

	// these are the values we use to gather all the scattered light
	vec3 total_ray = vec3(0.0); // for rayleigh
	vec3 total_mie = vec3(0.0); // for mie

	// initialize the optical depth. This is used to calculate how much air was in the ray
	vec2 opt_i = vec2(0.0);

	// also init the scale height, avoids some vec2's later on
	vec2 scale_height = vec2(height_ray, height_mie);

	// Calculate the Rayleigh and Mie phases.
	// This is the color that will be scattered for this ray
	// mu, mumu and gg are used quite a lot in the calculation, so to speed it up, precalculate them
	float mu = dot(dir, light_dir);
	float mumu = mu * mu;
	float gg = g * g;
	float phase_ray = 3.0 / (50.2654824574 /* (16 * pi) */) * (1.0 + mumu);
	float phase_mie = allow_mie ? 3.0 / (25.1327412287 /* (8 * pi) */) * ((1.0 - gg) * (mumu + 1.0)) / (pow(1.0 + gg - 2.0 * mu * g, 1.5) * (2.0 + gg)) : 0.0;

	// now we need to sample the 'primary' ray. this ray gathers the light that gets scattered onto it
	for (int i = 0; i < steps_i; ++i)
	{
		// calculate where we are along this ray
		vec3 pos_i = start + dir * (ray_pos_i + step_size_i * 0.5);

		// and how high we are above the surface
		float height_i = length(pos_i) - planet_radius;

		// now calculate the density of the particles (both for rayleigh and mie)
		vec2 density = exp(-height_i / scale_height) * step_size_i;

		// Add these densities to the optical depth, so that we know how many particles are on this ray.
		opt_i += density;

		// Calculate the step size of the light ray.
		// again with a ray sphere intersect
		vec2 hitInfo = ray_sphere(pos_i, light_dir, atmo_radius);

		// no early stopping, this one should always be inside the atmosphere
		// calculate the ray length
		float step_size_l = hitInfo.y / float(steps_l);

		// and the position along this ray
		// this time we are sure the ray is in the atmosphere, so set it to 0
		float ray_pos_l = 0.0;

		// and the optical depth of this ray
		vec2 opt_l = vec2(0.0);

		// now sample the light ray
		// this is similar to what we did before
		for (int l = 0; l < steps_l; ++l)
		{
			// calculate where we are along this ray
			vec3 pos_l = pos_i + light_dir * (ray_pos_l + step_size_l * 0.5);

			// the heigth of the position
			float height_l = length(pos_l) - planet_radius;

			// calculate the particle density, and add it
			opt_l += exp(-height_l / scale_height) * step_size_l;

			// and increment where we are along the light ray.
			ray_pos_l += step_size_l;
		    
		}

		// Now we need to calculate the attenuation
		// this is essentially how much light reaches the current sample point due to scattering
		vec3 attn = exp(-((beta_mie * e * (opt_i.y + opt_l.y)) + (beta_ray * e * (opt_i.x + opt_l.x))));

		// accumulate the scattered light (how much will be scattered towards the camera)
		total_ray += density.x * attn;
		total_mie += density.y * attn;

		// and increment the position on this ray
		ray_pos_i += step_size_i; 	
	}

	// calculate how much light can pass through the atmosphere
	float opacity = length(exp(-((beta_mie * e * opt_i.y) + (beta_ray * e * opt_i.x)) * density_multiplier));

	// calculate and return the final color
	return vec4((
			phase_ray * beta_ray * e * total_ray // rayleigh color
			+ phase_mie * beta_mie * e * total_mie // mie
			+ opt_i.x * e * beta_ambient // and ambient
	) * light_intensity, 1.0 - opacity); // now make sure the background is rendered correctly
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

	vec3 rayOrigin = CameraPosition - Center;
	vec3 rayDir = normalize(posPS);

	vec4 atm = calculate_scattering(
		rayOrigin,					// start
		rayDir,						// dir
		dstToSurface,				// max_dist
		normalize(LightDirection),	// light_dir
		LightIntensity,				// light_intensity
		PlanetRadius,				// planet_radius
		AtmosphereRadius,			// atmo_radius
		BetaRay,					// beta_ray
		BetaMie,					// beta_mie
		BetaAmbient,				// beta_ambient
		BetaE,						// beta_e
		G,							// g
		HeightRay,					// height_ray
		HeightMie,					// height_mie
		DensityMultiplier,			// density_multiplier
		32,							// step_size_i
		4);							// step_size_l

	fragColor = vec4(color.rgb + atm.rgb, 1.0); // color.rgb * (1.0 - atm.a) + atm.rgb * atm.a
}
