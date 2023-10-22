package com.marcuzzo;

import org.joml.Vector3f;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Since each chunk is identified by the three-dimensional point its located at
 * this object is used to compare chunks to be sorted for use with
 * binary search algorithms.
 */
public class PointCompare implements Comparator<Vector3f>, Serializable {
    @Override
    public int compare(Vector3f a, Vector3f b) {
        if (a.x < b.x) {
            return -1;
        }
        else if (a.x > b.x) {
            return 1;
        }

        //If x coordinates are equal
        else {
            return Float.compare(a.z, b.z);
        }

    }
}
