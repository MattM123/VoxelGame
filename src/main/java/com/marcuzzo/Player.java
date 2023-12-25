package com.marcuzzo;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;

public class Player implements Serializable {
    private static Vector3f position;
    private static float yaw = 0f;
    private static float pitch = 0f;

    /**
     * Default player object is initialized at a position of 0,0,0 within
     * Region 0,0.
     *
     */
    public Player() {
        position = new Vector3f(0, 0, 0);

        ChunkCache.setPlayerChunk(getChunkWithPlayer());
        RegionManager.enterRegion(getRegionWithPlayer());
    }

    public static Vector3f getPosition() {
        return position;
    }

    public static Vector2f getRotation() {
        return new Vector2f(yaw, pitch);
    }

    public void moveBackwards(float inc) {
        position.z += inc;
    }

    public void moveDown(float inc) {
        position.y -= inc;
    }

    public void moveForward(float inc) {
        position.z -= inc;
    }

    public void moveLeft(float inc) {
        position.x -= inc;
    }

    public void moveRight(float inc) {
        position.x += inc;
    }

    public void moveUp(float inc) {
        position.y += inc;
    }

    /**
     * Gets the region that the player currently inhabits.
     * If the region doesn't exist yet, generates and adds a new region
     * to the visible regions list
     * @return The region that the player is in
     */
    public static Region getRegionWithPlayer() {
        Region returnRegion = null;

        int x = (int) Player.getPosition().x;
        int xLowerLimit = ((x / RegionManager.REGION_BOUNDS) * RegionManager.REGION_BOUNDS);
        int xUpperLimit;
        if (x < 0)
            xUpperLimit = xLowerLimit - RegionManager.REGION_BOUNDS;
        else
            xUpperLimit = xLowerLimit + RegionManager.REGION_BOUNDS;


        int z = (int) Player.getPosition().z;
        int zLowerLimit = ((z / RegionManager.REGION_BOUNDS) * RegionManager.REGION_BOUNDS);
        int zUpperLimit;
        if (z < 0)
            zUpperLimit = zLowerLimit - RegionManager.REGION_BOUNDS;
        else
            zUpperLimit = zLowerLimit + RegionManager.REGION_BOUNDS;


        //Calculates region coordinates player inhabits
        int regionXCoord = xUpperLimit;
        int regionZCoord = zUpperLimit;

        for (Region region : RegionManager.visibleRegions) {
            Rectangle2D regionBounds = region.getBounds().getBounds2D();
            if (regionXCoord == regionBounds.getX() && regionZCoord == regionBounds.getY()) {
                returnRegion = region;
            }
        }

        if (returnRegion == null)
            returnRegion = new Region(regionXCoord, regionZCoord);

        return returnRegion;
    }


    /**
     * Gets the chunk that the player currently inhabits.
     * If the chunk doesn't exist yet, generates and adds a new chunk
     * to the region
     * @return The chunk that the player is in
     */
    public static Chunk getChunkWithPlayer() {
        int x = (int) Player.getPosition().x;
        int xLowerLimit = ((x / RegionManager.CHUNK_BOUNDS) * RegionManager.CHUNK_BOUNDS);
        int xUpperLimit;
        if (x < 0)
            xUpperLimit = xLowerLimit - RegionManager.CHUNK_BOUNDS;
        else
            xUpperLimit = xLowerLimit + RegionManager.CHUNK_BOUNDS;


        int z = (int) Player.getPosition().z;
        int zLowerLimit = ((z / RegionManager.CHUNK_BOUNDS) * RegionManager.CHUNK_BOUNDS);
        int zUpperLimit;
        if (z < 0)
            zUpperLimit = zLowerLimit - RegionManager.CHUNK_BOUNDS;
        else
            zUpperLimit = zLowerLimit + RegionManager.CHUNK_BOUNDS;


        //Calculates chunk coordinates player inhabits
        int chunkXCoord = xUpperLimit;
        int chunkZCoord = zUpperLimit;


        Region r = getRegionWithPlayer();
        Chunk c =  r.getChunkWithLocation(new Vector3f(chunkXCoord, 0, chunkZCoord));

        if (c == null) {
            Chunk d = new Chunk().initialize(chunkXCoord, 0, chunkZCoord);
            r.add(d);
            return d;
        }
        return c;
    }

    public void setLookDir(float x, float y) {
        yaw = x;
        pitch = y;
    }

    /**
     * Instantiates the players view matrix with is later
     * multiplied by the projection matrix. Typically,
     * a third matrix, model matrix, would be multiplied with these
     * matrices but since the chunk models are already defined
     * in three-dimensional space relative to the world, that
     * is not necessary.
     *
     * @return The players view matrix
     */
    public Matrix4f getViewMatrix() {
        Vector3f lookPoint = new Vector3f(0f, 0f, -1f);
        lookPoint.rotateX(Math.toRadians(pitch), lookPoint);
        lookPoint.rotateY(Math.toRadians(yaw), lookPoint);
        lookPoint.add(position);

        Matrix4f matrix = new Matrix4f();
        matrix.lookAt(position, lookPoint, new Vector3f(0, 1, 0), matrix);

        return matrix;
    }



    public String toString() {
        return "Player at position (" + position.x + ", " + position.y + ", " + position.z + ")";
    }
}
