package com.marcuzzo.Texturing;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class TextureLoader {
    private static final Logger logger = Logger.getLogger("Logger");
    private static int texId = 0;

    public TextureLoader() {
    }


    public static void loadTexture(String path) {
        //Loading texture atlas as bytebuffer

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer image = STBImage.stbi_load(path, width, height, channels, 0);

            //Binding texture ID
            texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);

            //Setting texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glGenerateMipmap(GL_TEXTURE_2D);

            //Uses bytebuffer as texture
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(), height.get(),
                    0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            //Frees memory after done using texture
            if (image != null) {
                STBImage.stbi_image_free(image);
                image.clear();
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
        }

    }

    public static void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(texId);
    }
}

