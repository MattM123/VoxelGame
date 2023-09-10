package com.marcuzzo;

import javafx.geometry.Point3D;

import java.io.*;

public class Cube extends Point3DByComposition implements Serializable {
    private BlockType type;
    public double f;
    public Cube(int x, int y, int z, BlockType b) {
        super(x, y, z);
        type = b;
    }
    public Cube(int x, int y, int z) {
        super(x, y, z);
    }

    public BlockType getBlockType() {
        return type;
    }
    @Override
    public void setBlockType(BlockType type) {
        this.type = type;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeDouble(myPoint.getX());
        out.writeDouble(myPoint.getY());
        out.writeDouble(myPoint.getZ());
        out.writeObject(getBlockType());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        myPoint = new Point3D(in.readDouble(), in.readDouble(), in.readDouble());
        type = (BlockType) in.readObject();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Cube) {
            return this.myPoint.getX() == ((Cube) o).myPoint.getX() && this.myPoint.getY() == ((Cube) o).myPoint.getY()
                    && this.myPoint.getZ() == ((Cube) o).myPoint.getZ() && this.getBlockType() == ((Cube) o).getBlockType();
        } else return false;
    }

}