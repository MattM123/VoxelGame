package com.marcuzzo;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Serializable;

public class Player implements Serializable {
    private final Matrix4f modelViewMatrix;
    private static Vector3f position = null;
    private final Vector3f rotation;
    private final Vector3f cameraUp = new Vector3f(0, 1, 0);
    private final float rotationAngle = 5f;

    /**
     * Default player object is initialized at a position of 0,0,0 within
     * Region 0,0.
     *
     */
    public Player() {
        this.modelViewMatrix = new Matrix4f();
        position = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setTranslation(position);

        this.rotation = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setRotationXYZ(this.rotation.x, this.rotation.y, this.rotation.z);

      //  getRegionWithPlayer();
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
            position.x += (float)Math.sin(Math.toRadians(this.rotation.y)) * -1.0f * offsetZ;
            position.z += (float)Math.cos(Math.toRadians(this.rotation.y)) * offsetZ;
        }
        if ( offsetX != 0) {
            position.x += (float)Math.sin(Math.toRadians(this.rotation.y - 90)) * -1.0f * offsetX;
            position.z += (float)Math.cos(Math.toRadians(this.rotation.y - 90)) * offsetX;
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
        this.rotation.x += offsetX;
        this.rotation.y += offsetY;
        this.rotation.z += offsetZ;

      //  modelViewMatrix.rotate(rotationAngle, this.rotation.x, this.rotation.y, this.rotation.z);
        modelViewMatrix.setRotationXYZ(this.rotation.x, this.rotation.y, this.rotation.z);
        //System.out.println(modelViewMatrix.rotation);
    }


    public static Region getRegion() {
        Region playerRegion;

        for (Region r : RegionManager.visibleRegions) {
            if (r.regionBounds.intersects(position.x(), position.z(), 1, 1)) {
                   // && (int) r.regionBounds.getY() == (int) getPosition().y) {
                return r;
            }
        }

        //Returns new region if one does not exist


        playerRegion = new Region((int) ((512*(Math.floor(Math.abs(Player.position.x/512))))), (int) (512*(Math.floor(Math.abs(Player.position.z/512)))));

        //Enters region if not found in visible region list
        RegionManager.enterRegion(playerRegion);
        //RegionManager.visibleRegions.add(playerRegion);


        return playerRegion;

    }

    public static Chunk getChunk() {
        return getRegion().getChunkWithPlayer();
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }



    public String toString() {
        return "Player at position (" + position.x + ", " + position.y + ", " + position.z + ")";
    }
}
