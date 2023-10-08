package com.marcuzzo.Texturing;

public enum BlockType {
    GRASS(Texture.GRASS_FULL, Texture.DIRT, Texture.GRASS_SIDE, Texture.GRASS_SIDE, Texture.GRASS_SIDE, Texture.GRASS_SIDE),
    DIRT(Texture.DIRT);

    private Texture singleTexture;
    private Texture top;
    private Texture bottom;
    private Texture left;
    private Texture right;
    private Texture front;
    private Texture back;
    private final boolean isSingleTexture;

    /**
     * Defines potentially unique textures for all faces of the blocktype
     * @param top Texture rendered on top of block
     * @param bottom Texture rendered on bottom of block
     * @param left Texture rendered on left of block
     * @param right Texture rendered on right of block
     * @param front Texture rendered on front of block
     * @param back Texture rendered on back of block
     */
    BlockType(Texture top, Texture bottom, Texture left, Texture right, Texture front, Texture back) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.front = front;
        this.back = back;
        isSingleTexture = false;
    }

    /**
     * Defines a single texture for every block face of a block
     * @param singleTexture Texture rendered on all faces of block
     */
    BlockType(Texture singleTexture) {
        this.singleTexture = singleTexture;
        isSingleTexture = true;
    }

    private Texture getFront() {
         return isSingleTexture ? singleTexture : front;
    }
    public TextureCoordinateStore getFrontCoords() {
        return getFront().getCoordinates();
    }


    private Texture getBack() {
        return isSingleTexture ? singleTexture : back;
    }
    public TextureCoordinateStore getBackCoords() {
        return getBack().getCoordinates();
    }


    private Texture getBottom() {
        return isSingleTexture ? singleTexture : bottom;
    }
    public TextureCoordinateStore getBottomCoords() {
        return getBottom().getCoordinates();
    }

    private Texture getLeft() {
        return isSingleTexture ? singleTexture : left;
    }
    public TextureCoordinateStore getLeftCoords() {
        return getLeft().getCoordinates();
    }

    private Texture getTop() {
        return isSingleTexture ? singleTexture : top;
    }
    public TextureCoordinateStore getTopCoords() {
        return getTop().getCoordinates();
    }

    private Texture getRight() {
        return isSingleTexture ? singleTexture : right;
    }
    public TextureCoordinateStore getRightCoords() {
        return getRight().getCoordinates();
    }
}
