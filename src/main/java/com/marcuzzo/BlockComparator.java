package com.marcuzzo;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Sorts blocks by coordinate for use in binary search algorithm
 */
public class BlockComparator implements Comparator<Block>, Serializable {

    @Override
    public int compare(Block a, Block b) {
        if (Float.compare(a.getLocation().x, b.getLocation().x) == -1) {
            return -1;
        }
        else if (Float.compare(a.getLocation().x, b.getLocation().x) == 1) {
            return 1;
        }

        //If x coordinates are equal
        else {
            float epsilon = Float.MIN_NORMAL;
            if (Math.abs(a.getLocation().z - b.getLocation().z) < epsilon) {
                if (Math.abs(a.getLocation().y - b.getLocation().y) < epsilon)
                    return 0;
                else
                    return Float.compare(a.getLocation().y, b.getLocation().y);
            } else {
                return Float.compare(a.getLocation().z, b.getLocation().z);
            }
        }
    }
}