#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform mat4 ModelViewInverseMat;

out vec4 posMS;
out vec2 texCoord;

void main()
{
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);

	posMS = ModelViewInverseMat * vec4(Position, 1);
	texCoord = UV0;
}
