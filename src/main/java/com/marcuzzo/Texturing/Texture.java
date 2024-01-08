package com.marcuzzo.Texturing;

public enum Texture {
    DIRT(new TextureCoordinateStore(new float[]{0f, 0f}, new float[]{0.03125f, 0f},
            new float[]{0f, 0.03125f}, new float[]{0.03125f, 0.03125f})),

    GRASS_SIDE(new TextureCoordinateStore(new float[]{0.03125f, 0.03125f}, new float[]{0.0625f, 0.03125f},
            new float[]{0.03125f, 0f}, new float[]{0.0625f, 0f})),

    GRASS_FULL(new TextureCoordinateStore(new float[]{0.0625f, 0.03125f}, new float[]{0.09375f, 0.03125f},
            new float[]{0.0625f, 0f}, new float[]{0.09375f, 0.0f}));

    private final TextureCoordinateStore init;

    /**
     * TextureCoordinate object stores texture coordinates for a specific block face. Assigns each
     * texture a set of texture coordinate to be referenced by the BlockType
     */
    Texture(TextureCoordinateStore init) {
        this.init = init;
    }

    public TextureCoordinateStore getCoordinates() {
        return init;
    }

}


