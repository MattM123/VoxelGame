package com.marcuzzo;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Serializable;

public class Player implements Serializable {
    private final Matrix4f modelViewMatrix;
    private final Vector3f position;
    private final Vector3f rotation;

    /**
     * Defauly player object is initalized at a position of 0,0,0 within
     * Region 0,0.
     *
     */
    public Player() {
      //  RegionManager.enterRegion(new Region ((int) coords.getX(), (int) coords.getY()));
        this.modelViewMatrix = new Matrix4f();
        this.position = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setTranslation(position);

        this.rotation = new Vector3f(0f, 0f, 0f);
        modelViewMatrix.setRotationXYZ(this.rotation.x, this.rotation.y, this.rotation.z);

      //  getRegionWithPlayer();
    }

    public Vector3f getPosition() {
        return this.position;
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
            this.position.x += (float)Math.sin(Math.toRadians(this.rotation.y)) * -1.0f * offsetZ;
            this.position.z += (float)Math.cos(Math.toRadians(this.rotation.y)) * offsetZ;
        }
        if ( offsetX != 0) {
            this.position.x += (float)Math.sin(Math.toRadians(this.rotation.y - 90)) * -1.0f * offsetX;
            this.position.z += (float)Math.cos(Math.toRadians(this.rotation.y - 90)) * offsetX;
        }
        this.position.y += offsetY;
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
        modelViewMatrix.setRotationXYZ(this.rotation.x, this.rotation.y, this.rotation.z);
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }



    public String toString() {
        return "Player at position (" + position.x + ", " + position.y + ", " + position.z + ")";
    }
}
