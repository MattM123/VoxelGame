package com.marcuzzo;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class TextureRegion {

    public final TextureAtlas atlas;
    public final int x, y, width, height;
    private Image image;

    public TextureRegion(TextureAtlas atlas, int x, int y, int width, int height) {
        this.atlas = atlas;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TextureRegion(TextureAtlas atlas, Image image, int x, int y, int width, int height) {
        this.atlas = atlas;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
    }

    public int getColorAt(int x, int y) {
        return atlas.getColorAt(this.x + x, this.y + y);
    }

    public double[] getTextureCoordinates(double x, double y) {
        return new double[] {getU(x), getV(y)};
    }

    public double[] scaleTextureCoordinates(double u, double v, double max) {
        return new double[] {scaleU(u, max), scaleV(v, max)};
    }

    public double getU(double x) {
        return (this.x + x) / atlas.getWidth();
    }

    public double getV(double y) {
        return (this.y + y) / atlas.getHeight();
    }

    public double scaleU(double u, double max) { //For conversion from UV systems using a different max value than 1.0
        return getU(u / max * this.width);
    }

    public double scaleV(double v, double max) {
        return getV(v / max * this.height);
    }

    public Image getImage() {
        if(image != null) return image; //Lazily initialize
        else {
            WritableImage img = new WritableImage(width, height);
            PixelWriter writer = img.getPixelWriter();
            PixelReader reader = atlas.getImage().getPixelReader();
            for(int x = 0; x < width; x++)
                for(int y = 0; y < height; y++)
                    writer.setArgb(x, y, reader.getArgb(x + this.x, y + this.y));
            return this.image = img;
        }
    }

}