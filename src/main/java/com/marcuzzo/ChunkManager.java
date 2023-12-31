package com.marcuzzo;

import org.joml.Vector3f;

import java.io.Serializable;

public class ChunkManager extends GlueList<Chunk> implements Serializable {

    /**
     * Since the order of chunks within a region matters the ChunkManager object
     * provides methods of chunk insertion and retrieval within a Region object
     */
    public ChunkManager() {
    }

    /**
     * Gets a chunk from the ChunkManager that is located in a specific position
     * in O(log n) time complexity. This location is the same location that
     * was used when the chunk was initialized. If no chunk is found with the location
     * null is returned
     *
     * @param loc The location of the chunk
     * @return Null if the chunk doesn't exist, else will return the chunk
     */
    public Chunk getChunkWithLocation(Vector3f loc) {
        return binarySearchChunkWithLocation(0, this.size() - 1, loc);
    }

    /**
     * Searches for an index to insert a new Chunk at in O(log n) time complexity.
     * Ensures the list is sorted by the Chunks location as new Chunks are inserted into it.
     *
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param c The chunk location to search for.
     * @return Returns the chunk object that was just inserted into the list.
     */
    public Chunk binaryInsertChunkWithLocation(int l, int r, Vector3f c) {
        ChunkComparator pointCompare = new ChunkComparator();
        Chunk q = new Chunk().initialize(c.x, c.y, c.z);

        if (this.isEmpty()) {
            this.add(q);
        }
        if (this.size() == 1) {
            //Inserts element as first in list
            if (pointCompare.compare(c, this.get(0).getLocation()) < 0) {
                this.add(0, q);
                return q;
            }
            //Appends to end of list
            if (pointCompare.compare(c, this.get(0).getLocation()) > 0) {
                this.add(q);
                return q;
            }
        }

        if (r >= l && this.size > 1) {
            int mid = l + (r - l) / 2;
            //When an index has been found, right and left will be very close to each other
            //Insertion of the right index will shift the right element
            //and all subsequent ones to the right.
            if (Math.abs(r - l) == 1) {
                this.add(r, q);
                return q;
            }

            //If element is less than first element insert at front of list
            if (pointCompare.compare(c, this.get(0).getLocation()) < 0) {
                this.add(0, q);
                return q;
            }
            //If element is more than last element insert at end of list
            if (pointCompare.compare(c, this.get(this.size - 1).getLocation()) > 0) {
                this.add(q);
                return q;
            }

            //If the index is near the middle
            if (pointCompare.compare(c, this.get(mid - 1).getLocation()) > 0
                    && pointCompare.compare(c, this.get(mid).getLocation()) < 0) {
                this.add(mid, q);
                return q;
            }
            if (pointCompare.compare(c, this.get(mid + 1).getLocation()) < 0
                    && pointCompare.compare(c, this.get(mid).getLocation()) > 0) {
                this.add(mid + 1, q);
                return q;
            }

            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (pointCompare.compare(c, this.get(mid).getLocation()) < 0) {
                return binaryInsertChunkWithLocation(l, mid - 1, c);
            }

            // Else the element can only be present
            // in right subarray
            return binaryInsertChunkWithLocation(mid + 1, r, c);

        } else {
            return null;
        }
    }

    /**
     * Searches for a chunk in O(log n) time complexity and returns it.
     *
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param c The chunk location to search for.
     * @return Returns the chunk if found. Else null.
     */
    public Chunk binarySearchChunkWithLocation(int l, int r, Vector3f c) {
        ChunkComparator pointCompare = new ChunkComparator();
        if (r >= l) {
            int mid = l + (r - l) / 2;

            // If the element is present at the middle
            if (pointCompare.compare(c, this.get(mid).getLocation()) == 0) {
                return this.get(mid);
            }


            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (pointCompare.compare(c, this.get(mid).getLocation()) < 0) {
                return binarySearchChunkWithLocation(l, mid - 1, c);
            }

            // Else the element can only be present
            // in right subarray
            if (pointCompare.compare(c, this.get(mid).getLocation()) > 0) {
                return binarySearchChunkWithLocation(mid + 1, r, c);
            }
        }
        return null;

    }

    /**
     * @return Returns a string containing all the chunks that are inside the
     * region associated with this ChunkManager.
     */
    public String getChunks() {
        StringBuilder s = new StringBuilder();
        for (Chunk c : this)
            s.append(c.toString()).append(", ");

        return s.toString();
    }

}