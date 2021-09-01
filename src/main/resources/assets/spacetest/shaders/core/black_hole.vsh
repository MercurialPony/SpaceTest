#version 150

in vec3 Position;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform mat4 ModelViewInverseMat;

out vec4 posMS;

void main()
{
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);

	posMS = ModelViewInverseMat * vec4(Position, 1);
}
