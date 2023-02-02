#version 150 core

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
// in float v_FragDistance; // The fragment's distance from the camera

uniform sampler2D u_BlockTex; // The block texture sampler

out vec4 fragColor; // The output fragment for the color framebuffer

void main()
{
	vec4 diffuseColor = texture(u_BlockTex, v_TexCoord);

	#ifdef ALPHA_CUTOFF
	if (diffuseColor.a < ALPHA_CUTOFF)
	{
		discard;
	}
	#endif

	diffuseColor *= v_Color;

	fragColor = diffuseColor;
}