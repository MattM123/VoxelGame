package com.marcuzzo;


import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    private final Map<String, Integer> uniforms;

    public ShaderProgram() {
        uniforms = new HashMap<>();
    }

    public void generateProgramID() {
        programId = glCreateProgram();
    }
    public int getProgramId() {
        return programId;
    }
    public void createVertexShader(String vertexShaderCode) {
        vertexShaderId = createShader(vertexShaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String fragmentShaderCode) {
        fragmentShaderId = createShader(fragmentShaderCode, GL_FRAGMENT_SHADER);
    }

    /**
     * Loads texture into shader
     * @param varName Name of texture
     * @param slot Texture unit to use
     */
    public void uploadTexture(String varName, int slot) {
        int varLocation = glGetUniformLocation(getProgramId(), varName);
       // glUse
        glUniform1f(varLocation, slot);

    }


    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId,
                uniformName);
        if (uniformLocation < 0) {
            throw new Exception("Could not find uniform:" +
                    uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }
    private int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        // Check for compilation errors
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Failed to compile shader:\n" + glGetShaderInfoLog(shaderId));
        }

        glAttachShader(programId, shaderId);
        return shaderId;
    }

    public void link() {
        glLinkProgram(programId);

        // Check if linking was successful
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            if (vertexShaderId != 0) {
                glDetachShader(programId, vertexShaderId);
                glDeleteShader(vertexShaderId);
            }
            if (fragmentShaderId != 0) {
                glDetachShader(programId, fragmentShaderId);
                glDeleteShader(fragmentShaderId);
            }

            System.err.println("Failed to link shader program:");
            System.err.println(glGetProgramInfoLog(programId));
            System.exit(1);
        }

        // Validate the shader program
        glValidateProgram(programId);

        // Check if validation was successful
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
            System.err.println("Failed to validate shader program:");
            System.err.println(glGetProgramInfoLog(programId));
            System.exit(1);
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteShader(vertexShaderId);
            glDeleteShader(fragmentShaderId);
            glDeleteProgram(programId);
        }
    }
}



