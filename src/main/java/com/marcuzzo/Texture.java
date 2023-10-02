package com.marcuzzo;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public enum Texture {
    DIRT(RegionManager.textureAtlas.getSubimage(0, 0, 16, 16)),
    GRASS_SIDE(RegionManager.textureAtlas.getSubimage(16, 0, 16, 16)),
    GRASS_TOP(RegionManager.textureAtlas.getSubimage(32, 0, 16, 16));

    public final BufferedImage subImage;

    private static final Map<Texture, ByteBuffer> BY_TEXTURE = new HashMap<>();
    private static final Logger logger = Logger.getLogger("Logger");

    Texture(BufferedImage subImage) {
        this.subImage = subImage;
    }

    //Populates the map with textures when the enum class loads
    static {
        for (Texture e: values()) {
            BY_TEXTURE.put(e, convertToByteBuffer(e.subImage));
        }
    }

    /**
     * Gets the filepath of the texture given its enum
     * @param texture The enum to search
     * @return The filepath of that enum
     */
    public static ByteBuffer getByteBufferTexture(Texture texture) {
        return BY_TEXTURE.get(texture);
    }

    //Converts image to bytebuffer to be loaded to GPU
    private static ByteBuffer convertToByteBuffer(BufferedImage bi) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(bi, "png", out);
            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException ex) {
            logger.warning("Unable to convert texture to ByeBuffer: " + ex.getMessage());
        }
        return BufferUtils.createByteBuffer(1);
    }
}


