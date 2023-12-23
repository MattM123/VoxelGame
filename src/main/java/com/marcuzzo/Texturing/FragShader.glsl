#version 330 core

//in vec2 outTexCoord;
//ut vec3 fragColor;
uniform sampler2D texture_sampler;

in vec4 fColor;
in vec2 fTexCoords;

out vec4 color;




void main()
{
    color = texture(texture_sampler, fTexCoords);
}