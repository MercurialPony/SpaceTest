#version 150 core

#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_parameters.glsl>
#import <sodium:include/chunk_matrices.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;

uniform int u_FogShape;
uniform vec3 u_RegionOffset;
uniform sampler2D u_LightTex; // The light map texture sampler

uniform vec3 CameraPosition;
uniform vec3 Corner;
uniform int FaceIndex;
uniform int FaceSize;
uniform float StartRadius;
uniform float RadiusRatio;

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

vec4 _sample_lightmap(sampler2D lightMap, ivec2 uv)
{
	return texture(lightMap, clamp(uv / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main()
{
	_vert_init();

	vec3 position = u_RegionOffset + _draw_translation + _vert_position;

	position += CameraPosition - Corner;

	float u = position.x / FaceSize;
	float v = position.z / FaceSize;
	float h = StartRadius * pow(RadiusRatio, position.y);

	position = h * cubeToSphereAdjusted(planeToCube(FaceIndex, u, v));

	position -= CameraPosition;

	// Transform the vertex position into model-view-projection space
	gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

	// Add the light color to the vertex color, and pass the texture coordinates to the fragment shader
	v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);
	v_TexCoord = _vert_tex_diffuse_coord;
}