package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;

import java.io.*;

public class Block implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private BlockType type;
    private float x;
    private float y;
    private float z;


    /**
     * Constructs a new Block with a specified BlockType
     * @param x coordinate of Block
     * @param y coordinate of Block
     * @param z coordinate of Block
     * @param b BlockType of Block
     */
    public Block(float x, float y, float z, BlockType b) {
        this.x = x;
        this.y = y;
        this.z = z;
        type = b;
    }

    /**
     * Constructs a new Block who's BlockType is specified
     * by a default value defined in a chunks initialization
     * method
     * @param x coordinate of Block
     * @param y coordinate of Block
     * @param z coordinate of Block
     */
    public Block(float x, float y, float z) {
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
        if (o instanceof Block) {
            return this.getX() == ((Block) o).getX() && this.getY() == ((Block) o).getY()
                    && this.getZ() == ((Block) o).getZ() && this.getBlockType() == ((Block) o).getBlockType();
        } else return false;
    }

    @Override
    public String toString() {
        return "[" + this.getX() + ", " + this.getY() + ", " + this.getZ() + "]";

    }

}