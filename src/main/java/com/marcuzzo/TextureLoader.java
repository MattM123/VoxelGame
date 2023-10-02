package com.marcuzzo;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.stbi_load;

public class TextureLoader {

    private static final Logger logger = Logger.getLogger("Logger");
    private static BufferedImage textureAtlas;
    public TextureLoader() {
    }

    public static void loadTexture(ByteBuffer buff) {

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
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 16, 16,
                0, GL_RGBA, GL_UNSIGNED_BYTE, buff);

    }

    public static void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

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

    public static BufferedImage getTextureAtlas() {
        return textureAtlas;
    }

    //Converts image to bytebuffer to be loaded to GPU
    public static ByteBuffer convertToByteBuffer(BufferedImage bi) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(bi, "png", out);
       //     ImageIO.write(bi, "png", new File("src/main/resources/textures/test.png"));
            ByteBuffer buff = BufferUtils.createByteBuffer(out.toByteArray().length);
            buff.put(out.toByteArray());
            buff.flip();
            return buff;
        } catch (IOException ex) {
            logger.warning("Unable to convert texture to ByeBuffer: " + ex.getMessage());
        }
        return BufferUtils.createByteBuffer(1);
    }

    //Converts image to bufferedimage
    //Desnt work???
    public static BufferedImage convertToBufferedImage(ByteBuffer imageData) {
        byte[] byteArr = new byte[imageData.remaining()];
        imageData.get(byteArr);


        // Create an InputStream from the byte array
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArr);

        // Read the InputStream and obtain a BufferedImage
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);

          //  System.out.println(bufferedImage.getType());

            if (bufferedImage == null) {
                logger.warning("Image is null");
            }

            return bufferedImage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

