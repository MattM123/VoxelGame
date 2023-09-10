package com.marcuzzo;


import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    public ShaderProgram() {}
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
            glDeleteProgram(programId);
        }
    }
}



