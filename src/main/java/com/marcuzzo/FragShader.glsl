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
  //  color = vec4(1.0, 0.0, 1.0, 0.5);
    //color = vec3(1.0f ,0.0f ,1.0);

    //#if __VERSION__ > 120
    //    fragColor = texture(texture_sampler, outTexCoord);
    //#else
   //     fragColor = texture2D(texture_sampler, outTexCoord);
    //#endif



}