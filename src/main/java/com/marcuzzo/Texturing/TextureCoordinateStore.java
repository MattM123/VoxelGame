package com.marcuzzo.Texturing;

public class TextureCoordinateStore {
    private final float[] BOTTOM_LEFT;
    private final float[] BOTTOM_RIGHT;
    private final float[] TOP_LEFT;
    private final float[] TOP_RIGHT;


    public TextureCoordinateStore(float[] BOTTOM_LEFT, float[] BOTTOM_RIGHT,
                                  float[] TOP_LEFT, float[] TOP_RIGHT){
        this.BOTTOM_LEFT = BOTTOM_LEFT;
        this.BOTTOM_RIGHT = BOTTOM_RIGHT;
        this.TOP_LEFT = TOP_LEFT;
        this.TOP_RIGHT = TOP_RIGHT;
    }

    public float[] getBottomLeft() {
        return BOTTOM_LEFT;
    }

    public float[] getBottomRight() {
        return BOTTOM_RIGHT;
    }

    public float[] getTopLeft() {
        return TOP_LEFT;
    }

    public float[] getTopRight() {
        return TOP_RIGHT;
    }
}
