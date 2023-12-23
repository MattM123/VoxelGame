#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoords;

uniform mat4 viewProjectionMatrix;


out vec4 fColor;
out vec2 fTexCoords;



void main() {
    gl_Position = viewProjectionMatrix * vec4(position, 1.0);
    fColor = aColor;
    fTexCoords = aTexCoords;
}
