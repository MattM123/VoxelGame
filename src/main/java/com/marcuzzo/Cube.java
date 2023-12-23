package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;

import java.io.*;

public class Cube implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private BlockType type;
    private float f;
    private float x;
    private float y;
    private float z;

    public Cube(float x, float y, float z, BlockType b) {
        this.x = x;
        this.y = y;
        this.z = z;
        type = b;
    }
    public Cube(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockType getBlockType() {
        return type;
    }

    public void setBlockType(BlockType type) {
        this.type = type;
    }

    public float getF() {
        return f;
    }
    public float getY() {
        return y;
    }
    public float getX() {
        return x;
    }
    public float getZ() { return z; }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(getX());
        out.writeFloat(getY());
        out.writeFloat(getZ());
        out.writeObject(getBlockType());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.x = in.readFloat();
        this.y = in.readFloat();
        this.z = in.readFloat();
        type = (BlockType) in.readObject();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Cube) {
            return this.getX() == ((Cube) o).getX() && this.getY() == ((Cube) o).getY()
                    && this.getZ() == ((Cube) o).getZ() && this.getBlockType() == ((Cube) o).getBlockType();
        } else return false;
    }

    @Override
    public String toString() {
        return "[" + this.getX() + ", " + this.getY() + ", " + this.getZ() + "]";

    }

}