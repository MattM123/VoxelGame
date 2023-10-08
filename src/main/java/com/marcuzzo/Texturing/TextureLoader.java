package com.marcuzzo.Texturing;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_load;

public class TextureLoader {

    public TextureLoader() {
    }


    public static void loadTexture(String path) {
        //Loading texture atlas as bytebuffer
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer image = stbi_load(path, width, height, channels, 0);

        //Binding texture ID
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);

        //Setting texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glGenerateMipmap(GL_TEXTURE_2D);


        //Uses bytebuffer as texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 512, 512,
                0, GL_RGBA, GL_UNSIGNED_BYTE,image);

    }

    public static void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
/*
    public static void initTextureAtlas() {
        //Loading texture atlas as bytebuffer
        String p = "src/main/resources/textures/texture_atlas.png";
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer image = stbi_load(p, width, height, channels, 0);


        if (image != null) {
            try {
                textureAtlas = ImageIO.read(new File("src/main/resources/textures/texture_atlas.png"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            logger.warning("Unable to load texture atlas: Image is null");
    }

 */
}

