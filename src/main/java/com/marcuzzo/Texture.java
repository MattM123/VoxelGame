package com.marcuzzo;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public enum Texture {
    DIRT(TextureLoader.getTextureAtlas().getSubimage(0, 0, 16, 16)),
    GRASS_SIDE(TextureLoader.getTextureAtlas().getSubimage(16, 0, 16, 16)),
    GRASS_TOP(TextureLoader.getTextureAtlas().getSubimage(32, 0, 16, 16));

    private final BufferedImage subImage;
    private static final Map<Texture, ByteBuffer> BY_TEXTURE = new HashMap<>();

    Texture(BufferedImage subImage) {
        this.subImage = subImage;
    }

    //Populates the map with textures when the enum class loads
    static {
        for (Texture e: values()) {
            BY_TEXTURE.put(e, TextureLoader.convertToByteBuffer(e.subImage));
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


}


