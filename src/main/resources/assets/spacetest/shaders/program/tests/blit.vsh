#version 150

in vec4 Position;

uniform mat4 ProjMat;

// out vec2 fragCoords;

void main()
{
	gl_Position = vec4(ProjMat * vec4(Position.xy, 0.0, 1.0), 0.2, 1.0);
	//fragCoords = Position.xy;
}
