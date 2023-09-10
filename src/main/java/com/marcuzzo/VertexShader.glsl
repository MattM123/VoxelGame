#version 330 core

uniform mat4 modelViewProjectionMatrix;
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
//layout(location = 2) in vec3 normal;

//out vec3 vertexColor;
//out vec3 vertexNormal;

void main() {
    gl_Position = modelViewProjectionMatrix * vec4(position, 1.0);
 //   vertexColor = color;
  //  vertexNormal = normal;
}