package com.marcuzzo;

import org.apache.commons.lang3.ArrayUtils;
import org.fxyz3d.geometry.Point3D;

import java.io.Serializable;
import java.util.List;

public class ChunkManager extends GlueList<Chunk> implements Serializable {
    public final int RENDER_DISTANCE = RegionManager.RENDER_DISTANCE;
   // public static ChunkRenderer renderer;
    private final PointCompare pointCompare = new PointCompare();

    public ChunkManager() {

    }

    /**
     * Gets the chunk that the player currently inhabits.
     * @return The chunk that the player is in
     */
    public Chunk getChunkWithPlayer() {
        int x = (int) Window.getPlayer().getPosition().x;
        int xLowerLimit = ((x / RegionManager.CHUNK_BOUNDS) * RegionManager.CHUNK_BOUNDS);
        int xUpperLimit = xLowerLimit + RegionManager.CHUNK_BOUNDS;

        int z = (int) Window.getPlayer().getPosition().z;
        int zLowerLimit = ((z / RegionManager.CHUNK_BOUNDS) * RegionManager.CHUNK_BOUNDS);
        int zUpperLimit = zLowerLimit + RegionManager.CHUNK_BOUNDS;

        //Calculates chunk coordinates player inhabits
        int chunkXCoord = (x - xLowerLimit > xUpperLimit - x ? xUpperLimit : xLowerLimit);
        int chunkYCoord = (z - zLowerLimit > zUpperLimit - z ? zUpperLimit : zLowerLimit);


        Region r = RegionManager.getRegionWithPlayer();
        Chunk c =  r.getChunkWithLocation(new Point3D(chunkXCoord, chunkYCoord, 0));

        if (c == null) {
            Chunk d = new Chunk().initialize(chunkXCoord, chunkYCoord, 0);
            RegionManager.getRegionWithPlayer().add(d);
                return d;
        }
        return c;
    }

    /**
     * Gets a chunk from the manager that is located in a specific position. This location is the same
     * location that was used when the chunk was initialized. If no chunk is found with the location
     * null is returned
     *
     * @param loc The location of the chunk
     * @return Null if the chunk doesn't exist, else will return the chunk
     */
    public Chunk getChunkWithLocation(Point3D loc) {
        return binarySearchChunkWithLocation(0, this.size() - 1, loc);
    }

    /**
     * Updates the chunks surrounding the player to be added to the world if in renderer distance.
     * Also removes chunks from world that are no longer in render distance.
     */
    public void updateChunks() {
        RegionManager.getRegionWithPlayer();
        if (RegionManager.renderer == null)
            RegionManager.renderer = new ChunkRenderer(RENDER_DISTANCE, Chunk.CHUNK_BOUNDS,
                    RegionManager.getRegionWithPlayer().getChunkWithPlayer(), Window.getPlayer());

        GlueList<Chunk> chunks = ChunkRenderer.getChunksToRender();
        float[] vertices = new float[0];

        for (Chunk c : chunks) {
            Main.executor.execute(c::updateMesh);
            vertices = ArrayUtils.addAll(c.getZPlanarPoints(), vertices);
        }


/*
            //Removes chunks no longer in render distance and frees the memory
            for (Chunk chunk : cList) {
                Platform.runLater(() -> {

                    if (cList.parallelStream().filter(c -> c.equalsLocation(chunk)).findFirst().orElse(null) != null)
                        world.getChildren().remove(chunk);
                });
            }

            //Adds chunk to world if it is not visible
            for (Chunk chunk : cList) {
                Platform.runLater(() -> {
                    try {
                        chunk.updateMesh();
                        world.getChildren().add(chunk);
                    } catch (IllegalArgumentException ignored) {
                    }
              });
            }

            /*
                //Gets regions that are within render distance
                for (Chunk c : cList) {
                    MainApplication.executor.execute(() -> {
                        if (!rList.contains(c.getRegion()))
                            rList.add(c.getRegion());
                    });
                }



                //Updates regions to render
                List<Region> visible = RegionManager.visibleRegions;
                if (visible.size() > 0) {
                    if (rList.isEmpty()) {
                        rList.addAll(RegionManager.getSpawnRegions());
                    }

                    Platform.runLater(() -> {
                        //First leave regions no longer in render distance
                        for (Region r : visible) {
                            if (!rList.contains(r))
                                RegionManager.leaveRegion(r);
                        }
                        System.out.println(rList.toString());
                        //Then enter new regions within render distances
                        for (Region r : rList) {
                            if (!visible.contains(r))
                                RegionManager.enterRegion(r);
                        }
                    });
                }

             */
    }

    /**
     * Uses binary search to search for an index to insert a new chunk at.
     * Ensures the list is sorted as new objects are inserted into it.
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param c The chunk location to search for.
     * @return Returns the chunk object that was just inserted into the list.
     */
    public Chunk binaryInsertChunkWithLocation(int l, int r, Point3D c) {

        if (this.isEmpty()) {
            Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
            this.add(q);
        }
        if (this.size() == 1) {
            //Inserts element as first in list
            if (pointCompare.compare(c, this.get(0).getLocation()) < 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
                this.add(0, q);
                return q;
            }
            //Appends to end of list
            if (pointCompare.compare(c, this.get(0).getLocation()) > 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
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
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
                this.add(r, q);
                return q;
            }

            //If element is less than first element
            if (pointCompare.compare(c, this.get(0).getLocation()) < 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
                this.add(0, q);
                return q;
            }
            //If element is more than last element
            if (pointCompare.compare(c, this.get(this.size - 1).getLocation()) > 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
                this.add(q);
                return q;
            }

            // If the index is near the middle
            if (pointCompare.compare(c, this.get(mid - 1).getLocation()) > 0
                    && pointCompare.compare(c, this.get(mid).getLocation()) < 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
                this.add(mid, q);
                return q;
            }
            if (pointCompare.compare(c, this.get(mid + 1).getLocation()) < 0
                    && pointCompare.compare(c, this.get(mid).getLocation()) > 0) {
                Chunk q = new Chunk().initialize((int) c.getX(), (int) c.getY(), (int) c.getZ());
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

        }
        else {
            return null;
        }

    }

    /**
     * Uses binary search to search for a chunk that is in the list
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param c The chunk location to search for.
     * @return Returns the chunk if found.
     */
    public Chunk binarySearchChunkWithLocation(int l, int r, Point3D c) {
        if (r >= l) {
            int mid = l + (r - l) / 2;

            // If the element is present at the middle
            if (pointCompare.compare(c, this.get(mid).getLocation()) == 0) {
              //  System.out.println("CHUNK SEARCH SIZE: " + this.size);
             //   System.out.println("LEFT: " + l + " MID: " + mid + " RIGHT: " + r);
             //   System.out.println(this.get(mid));
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

    public List<Chunk> getChunks() {
        return this;
    }

}