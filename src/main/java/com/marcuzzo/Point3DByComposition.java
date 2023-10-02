package com.marcuzzo;

import javafx.geometry.Point3D;

import java.io.Serializable;

abstract class Point3DByComposition implements Serializable {
    Point3D myPoint;
    BlockType type;

    public Point3DByComposition(double x, double y, double z) {
        myPoint = new Point3D(x, y, z);
    }
    public abstract void setBlockType(BlockType type);
}