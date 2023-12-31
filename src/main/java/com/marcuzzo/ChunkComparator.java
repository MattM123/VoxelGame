package com.marcuzzo;

import org.joml.Vector3f;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Since each chunk is identified by the three-dimensional point its located at
 * this object is used to sort the chunks for use with
 * binary search algorithms.
 */
public class ChunkComparator implements Comparator<Vector3f>, Serializable {
    @Override
    public int compare(Vector3f a, Vector3f b) {
        if (Float.compare(a.x, b.x) == -1) {
            return -1;
        }
        else if (Float.compare(a.x, b.x) == 1) {
            return 1;
        }

        //If x coordinates are equal
        else {
            float epsilon = Float.MIN_NORMAL;
            if (Math.abs(a.z - b.z) < epsilon)
                return 0;
            else
                return Float.compare(a.z, b.z);
        }
    }
}
