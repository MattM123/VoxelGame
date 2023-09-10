package com.marcuzzo;
import org.fxyz3d.geometry.Point3D;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Since each chunk is identified by the three-dimensional point its located at
 * this object is used to compare chunks to be sorted for use with
 * binary search algorithms.
 */
public class PointCompare implements Comparator<Point3D>, Serializable {
    @Override
    public int compare(Point3D a, Point3D b) {
        if (a.getX() < b.getX()) {
            return -1;
        }
        else if (a.getX() > b.getX()) {
            return 1;
        }

        //x coordinates are equal
        else {
            return Float.compare(a.getY(), b.getY());
        }

    }
}
