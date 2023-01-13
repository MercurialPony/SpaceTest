#version 150

in vec4 vertexColor;
in vec2 texCoord0;

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

out vec4 fragColor;

void main()
{
	// same this as the default rendertype fragment shaders minus fog
	fragColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
}