package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;
import org.joml.Vector3f;

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

    public Vector3f getLocation() {
        return new Vector3f(x, y, z);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
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
            return getLocation().x == ((Block) o).getLocation().x && getLocation().y == ((Block) o).getLocation().y
                    && getLocation().z == ((Block) o).getLocation().z;// && this.getBlockType() == ((Block) o).getBlockType();
        } else return false;
    }

    @Override
    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";

    }

}