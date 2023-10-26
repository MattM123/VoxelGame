package com.marcuzzo;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;

public class Player implements Serializable {
    private static Matrix4f modelViewMatrix;
    private static Vector3f position = null;
    private static Vector3f rotation = null;
  //  private final Vector3f cameraUp = new Vector3f(0, 1, 0);

    /**
     * Default player object is initialized at a position of 0,0,0 within
     * Region 0,0.
     *
     */
    public Player() {
        modelViewMatrix = new Matrix4f();
        position = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setTranslation(position);

        rotation = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setRotationXYZ(rotation.x, rotation.y, rotation.z);

        ChunkRenderer.setPlayerChunk(getChunkWithPlayer());
        RegionManager.enterRegion(getRegionWithPlayer());
    }

    public static Vector3f getPosition() {
        return position;
    }

    /*
    public void setPosition(float x, float y, float z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
        this.modelViewMatrix.setTranslation(position);
    }

     */

    public void movePosition(float offsetX, float offsetY, float offsetZ) {
        if ( offsetZ != 0 ) {
            position.x += (float)Math.sin(Math.toRadians(rotation.y)) * -1.0f * offsetZ;
            position.z += (float)Math.cos(Math.toRadians(rotation.y)) * offsetZ;
        }
        if ( offsetX != 0) {
            position.x += (float)Math.sin(Math.toRadians(rotation.y - 90)) * -1.0f * offsetX;
            position.z += (float)Math.cos(Math.toRadians(rotation.y - 90)) * offsetX;
        }
        position.y += offsetY;
        modelViewMatrix.setTranslation(position);
    }

    /*
    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.x = x;
        this.rotation.y = y;
        this.rotation.z = z;
        modelViewMatrix.setRotationXYZ(this.rotation.x, this.rotation.y, this.rotation.z);
    }

     */

    public void moveRotation(float offsetX, float offsetY, float offsetZ) {
        rotation.x += offsetX;
        rotation.y += offsetY;
        rotation.z += offsetZ;

        float rotationAngle = 5f;
        modelViewMatrix.rotate(rotationAngle, rotation.x, rotation.y, rotation.z);
        //modelViewMatrix.setRotationXYZ(rotation.x, rotation.y, rotation.z);
        //System.out.println(modelViewMatrix.rotation);
    }

    public static Vector3f getRotation() {
        return rotation;
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

    public static Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }



    public String toString() {
        return "Player at position (" + position.x + ", " + position.y + ", " + position.z + ")";
    }
}
