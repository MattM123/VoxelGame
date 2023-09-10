package com.marcuzzo;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public class TextureAtlas implements Serializable {

    private final Image image;

    private final Map<String, TextureRegion> regions;

    public TextureAtlas(Image image) {
        this.image = image;
        regions = new HashMap<>();
    }

    /**
     * Creates an extremely primitive texture atlas.
     * Could use bin packing eventually.
     */
    public TextureAtlas(Map<String, Image> images) {
        this.regions = new HashMap<>();
        int height = (int) Math.ceil(images.values().stream().mapToDouble(Image::getHeight).sum());
        OptionalDouble w = images.values().stream().mapToDouble(Image::getWidth).max();
        WritableImage i = new WritableImage(w.isPresent() ? (int) w.getAsDouble() : 0, height);
        int h = 0;
        PixelWriter writer = i.getPixelWriter();
        for(Map.Entry<String, Image> entry : images.entrySet()) {
            Image img = entry.getValue();
            PixelReader reader = img.getPixelReader();
            for(int x = 0; x < img.getWidth(); x++)
                for(int y = 0; y < img.getHeight(); y++)
                    writer.setColor(x, y + h, reader.getColor(x, y));
            createRegion(entry.getKey(), img, 0, h, (int) img.getWidth(), (int) img.getHeight());
            h += img.getHeight();
        } this.image = i;
    }

    public TextureRegion createRegion(String name, int x, int y, int width, int height) {
        TextureRegion reg;
        regions.put(name, reg = new TextureRegion(this, x, y, width, height));
        return reg;
    }

    private TextureRegion createRegion(String name, Image image, int x, int y, int width, int height) {
        TextureRegion reg;
        regions.put(name, reg = new TextureRegion(this, x, y, width, height));
        return reg;
    }

    public TextureRegion getRegion(String name) {
        return regions.get(name);
    }

    public Map<String, TextureRegion> getRegions() {
        return Collections.unmodifiableMap(regions);
    }

    public int getWidth() {
        return (int) image.getWidth();
    }

    public int getHeight() {
        return (int) image.getHeight();
    }

    public int getColorAt(int x, int y) {
        if(x >= image.getWidth() || y >= image.getHeight()) return -1;
        return image.getPixelReader().getArgb(x, y);
    }

    public Image getImage() {
        return image;
    }

}