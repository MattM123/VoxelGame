package com.marcuzzo;

import org.apache.commons.lang3.ArrayUtils;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.polygon.PolygonMeshView;
import org.lwjgl.BufferUtils;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.*;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class Chunk extends PolygonMeshView implements Serializable {
    private Point3D location;
    public static final int CHUNK_BOUNDS = 16;
    public static final int CHUNK_HEIGHT = 320;
    private boolean didChange = false;
    private final List<Cube> cubes = new GlueList<>();
    public int[][] heightMap = new int[CHUNK_BOUNDS][CHUNK_BOUNDS];
    public List<Cube> heightMapPointList = new GlueList<>();
    private float[] meshPoints;
    private float[] zPlanarPoints;
    private int[] zFaces;
    private static final Logger logger = Logger.getLogger("Logger");

    public Chunk() {
        setOnMouseClicked(mouseEvent -> {
            didChange = true;
            Main.executor.execute(this::updateMesh);
        });
    }

    /**
     * Initializes a chunk at a given point and populates it's heightmap using Simplex noise
     * @param x coordinate of top left chunk corner
     * @param y coordinate of top left chunk corner
     * @param z coordinate of top left chunk corner
     * @return Returns the chunk
     */
    public Chunk initialize(int x, int y, int z) {
        location = new Point3D(x, y, z);
        //===================================
        //Generate chunk height map
        //===================================

        int xCount = 0;
        int yCount = 0;
        for (int x1 = x; x1 < x + CHUNK_BOUNDS; x1++) {
            for (int y1 = y; y1 < y + CHUNK_BOUNDS; y1++) {

                //Converts the raw noise value in the range of -1 to 1, to the range of 0 to 320 to match Z coordinate.
                int elevation = getGlobalHeightMapValue(x1, y1);

                heightMap[xCount][yCount] = elevation;
                Cube c = new Cube(x1, y1, elevation, BlockType.GRASS);
                heightMapPointList.add(c);

                yCount++;
                if (yCount > CHUNK_BOUNDS - 1)
                    yCount = 0;
            }
            xCount++;
            if (xCount > CHUNK_BOUNDS - 1)
                xCount = 0;
        }


        //How far down caves should start generating
        int caveStart = 50;

        //checks chunks for cubes to renderer based on noise value and heightmap
        for (int x1 = x; x1 < x + CHUNK_BOUNDS; x1++) {
            for (int y1 = y; y1 < y + CHUNK_BOUNDS; y1++) {
                for (int z1 = z; z1 <= heightMap[xCount][yCount]; z1++) {

                    Cube c = new Cube(x1, y1, z1);
                    c.f = OpenSimplex.noise3_ImproveXZ(RegionManager.WORLD_SEED, x1 * 0.05, z1 * 0.05, y1 * 0.05);
                    if (c.f > 0.00)
                        cubes.add(c);
                    if (c.f <= 0.00 && z1 >= heightMap[xCount][yCount] - caveStart)
                        cubes.add(c);


                    yCount++;
                    if (yCount > CHUNK_BOUNDS - 1)
                        yCount = 0;
                }
                xCount++;
                if (xCount > CHUNK_BOUNDS - 1)
                    xCount = 0;
            }
        }
        didChange = true;
        updateMesh();

        return this;
    }

    /**
     * Generates chunk mesh based on height-mapped and interpolated points on initial chunk creation
     * Also removes and adds points to mesh based on player actions post-initialization.
     * *
     * Points Array: Every 3 elements make up an XYZ coordinate in 3D space.
     * Faces Array:
     */
    public void updateMesh() {
     //   TextureAtlas textures = RegionManager.textures1;
       // Thread t = new Thread(() -> {
            if (cubes.size() > 0 && didChange) {

                /*
                //Defining block texture atlas
                PhongMaterial mat = new PhongMaterial();

                if (textures != null && mat.getDiffuseMap() != textures.getImage())
                    try {
                        mat.setDiffuseMap(RegionManager.textures1.getImage());
                    } catch (Exception ignore) {
                    }
                else {
                    System.out.println("Null Textures");
                    System.exit(0);
                }

                 */

        /*===============================================
             Texture coordinates for mesh section
         ================================================*/

                /*
                TextureRegion grass_top = textures.getRegion("grass_top");
                TextureRegion grass_side = textures.getRegion("grass_side");
                TextureRegion dirt = textures.getRegion("dirt");
                float[] texCoords = {
                        (float) grass_top.getTextureCoordinates(0, 0)[0], (float) grass_top.getTextureCoordinates(0, 0)[1],
                        (float) grass_top.getTextureCoordinates(256, 0)[0], (float) grass_top.getTextureCoordinates(256, 0)[1],
                        (float) grass_top.getTextureCoordinates(256, 256)[0], (float) grass_top.getTextureCoordinates(256, 256)[1],
                        (float) grass_top.getTextureCoordinates(0, 256)[0], (float) grass_top.getTextureCoordinates(0, 256)[1],


                        (float) grass_side.getTextureCoordinates(0, 0)[0], (float) grass_side.getTextureCoordinates(0, 0)[1],
                        (float) grass_side.getTextureCoordinates(256, 0)[0], (float) grass_side.getTextureCoordinates(256, 0)[1],
                        (float) grass_side.getTextureCoordinates(256, 256)[0], (float) grass_side.getTextureCoordinates(256, 256)[1],
                        (float) grass_side.getTextureCoordinates(0, 256)[0], (float) grass_side.getTextureCoordinates(0, 256)[1],

                        (float) dirt.getTextureCoordinates(0, 0)[0], (float) dirt.getTextureCoordinates(0, 0)[1],
                        (float) dirt.getTextureCoordinates(256, 0)[0], (float) dirt.getTextureCoordinates(256, 0)[1],
                        (float) dirt.getTextureCoordinates(256, 256)[0], (float) dirt.getTextureCoordinates(256, 256)[1],
                        (float) dirt.getTextureCoordinates(0, 256)[0], (float) dirt.getTextureCoordinates(0, 256)[1],

                };
                 */

                float[] points = new float[0];
                float[] zPoints = new float[0];
                int[] faces = new int[0];
                int[] zFaces = new int[0];


                List<Cube> cList = getInterpolatedCubes();
                for (Cube c : cList) {
                    if (c != null) {
                        if (!heightMapPointList.contains(c))
                            heightMapPointList.add(c);
                    }
                }

        /*===================================
          Rendering Z Axis Faces
        ==================================*/
                for (int i = 0; i < CHUNK_HEIGHT; i++) {
                    float finalI = i;
                    final int max = 10;
                    int count = 0;
                    List<Cube> p = null;

                    while (count < max) {
                        try {
                            count++;
                            p = heightMapPointList.stream().filter(q -> q.myPoint.getZ() == finalI).toList();
                            break;
                        } catch (ConcurrentModificationException e) {
                            logger.info("Failed chunk rendering attempt " + count);
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }


                    if (p == null) {
                        logger.warning("Failed all chunk rendering attempts. Chunk may not renderer properly.");
                        p = heightMapPointList.stream().filter(q -> q != null && q.myPoint.getZ() == finalI).toList();
                    }
                    if (!p.isEmpty()) {
                        for (Cube point3D : p) {
                            int[] face = new int[0];


                            //First face point 0
                            float[] t12 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t12) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t12));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getX());
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getY());
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }
                            /*
                            switch (point3D.getBlockType()) {
                                case GRASS -> face = ArrayUtils.add(face, 0);
                                case DIRT -> face = ArrayUtils.add(face, 8);
                            }
                             */


                            //Second face point 1
                            float[] t1 = {(float) point3D.myPoint.getX(), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t1) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t1));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getX());
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getY() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }
                            /*
                            switch (point3D.getBlockType()) {
                                case GRASS -> face = ArrayUtils.add(face, 1);
                                case DIRT -> face = ArrayUtils.add(face, 9);
                            }
                             */

                            //Third face point 2
                            float[] t2 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t2) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t2));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getX() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getY() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }
                            /*
                            switch (point3D.getBlockType()) {
                                case GRASS -> face = ArrayUtils.add(face, 2);
                                case DIRT -> face = ArrayUtils.add(face, 10);
                            }
                             */

/*
                            //First face point 0
                            float[] t123 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t12) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t123));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getX());
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getY());
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }

                            //Third face point 2
                            float[] t21 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t2) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t21));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getX() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getY() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }

 */

                            //Fourth face point 3
                            float[] t3 = {(float) (point3D.myPoint.getX() - 1), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                            if (getPointIndex(points, t3) > -1)
                                face = ArrayUtils.add(face, getPointIndex(points, t3));
                            else {
                                zPoints = ArrayUtils.add(zPoints, (float) (point3D.myPoint.getX() - 1));
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getY());
                                zPoints = ArrayUtils.add(zPoints, (float) point3D.myPoint.getZ());
                                face = ArrayUtils.add(face, zPoints.length / 3 - 1);
                            }
                            /*
                            switch (point3D.getBlockType()) {
                                case GRASS -> face = ArrayUtils.add(face, 3);
                                case DIRT -> face = ArrayUtils.add(face, 11);
                            }
                             */
                     //       zPoints = ArrayUtils.add(zPoints, 65535);
                            face = ArrayUtils.add(face, 65535);
                            zFaces = ArrayUtils.addAll(face, zFaces);

                        }
                    }
                }
        /*===================================
          Rendering X and Y Axis Faces
        ==================================*/
                for (int i = 0; i < CHUNK_BOUNDS; i++) {
                    float finalI = i;
                    final int max = 10;
                    int count = 0;
                    List<Cube> x = null;
                    List<Cube> y = null;

                    while (count < max) {
                        try {
                            count++;
                            x = heightMapPointList.stream().filter(q -> q.myPoint.getX() == (getLocation().getX() + finalI)).toList();
                            y = heightMapPointList.stream().filter(q -> q.myPoint.getY() == (getLocation().getY() + finalI)).toList();
                            break;
                        } catch (ConcurrentModificationException e) {
                            logger.info("Failed chunk rendering attempt " + count);
                            if (count == max)
                                e.printStackTrace();
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

                    if (x == null) {
                        logger.warning("Failed all chunk rendering attempts. Chunk may not renderer properly.");
                        x = heightMapPointList.stream().filter(q -> q != null && q.myPoint.getX() == (getLocation().getX() + finalI)).toList();
                    }
                    if (!x.isEmpty()) {
                        for (Cube point3D : x) {
                            int[] face = new int[0];
                            int[] face1 = new int[0];

                            //Determines if faces should be added to mesh
                            int base = (int) point3D.myPoint.getZ();
                            int plus1 = getGlobalHeightMapValue((int) point3D.myPoint.getX() + 1, (int) point3D.myPoint.getY());
                            int minus1 = getGlobalHeightMapValue((int) point3D.myPoint.getX() - 1, (int) point3D.myPoint.getY());

                            //First face set
                            if (plus1 != base || minus1 != base) {
                                float[] t12 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, t12) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t12));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w = {(float) (point3D.myPoint.getX() - 1), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, w) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 4);
                                        face = ArrayUtils.add(face, 4);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 8);
                                        face = ArrayUtils.add(face, 8);
                                    }
                                }

                                 */
                            }

                            //Second face set
                            if (plus1 != base || minus1 != base) {
                                float[] t1 = {(float) point3D.myPoint.getX(), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, t1) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t1));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w1 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, w1) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w1));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 5);
                                        face = ArrayUtils.add(face, 5);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 9);
                                        face = ArrayUtils.add(face, 9);
                                    }
                                }

                                 */
                            }


                            //Third face set
                            if (plus1 != base || minus1 != base) {
                                float[] t2 = {(float) point3D.myPoint.getX(), (float) (point3D.myPoint.getY() - 1), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, t2) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t2));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w2 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, w2) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w2));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 6);
                                        face = ArrayUtils.add(face, 6);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 10);
                                        face = ArrayUtils.add(face, 10);
                                    }
                                }

                                 */
                            }

                            //Fourth face set
                            if (plus1 != base || minus1 != base) {
                                float[] t3 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, t3) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t3));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w3 = {(float) (point3D.myPoint.getX() - 1), (float) point3D.myPoint.getY(), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, w3) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w3));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 7);
                                        face = ArrayUtils.add(face, 7);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 11);
                                        face = ArrayUtils.add(face, 11);
                                    }
                                }

                                 */
                            }

                            if (face.length == 8)
                                faces = ArrayUtils.addAll(faces, face);
                            if (face1.length == 8)
                                faces = ArrayUtils.addAll(faces, face1);
                        }
                    }

                    if (y == null) {
                        logger.warning("Failed all chunk rendering attempts. Chunk may not renderer properly.");
                        y = heightMapPointList.stream().filter(q -> q != null && q.myPoint.getY() == (getLocation().getY() + finalI)).toList();
                    }
                    if (!y.isEmpty()) {
                        for (Cube point3D : y) {
                            int[] face = new int[0];
                            int[] face1 = new int[0];

                            //Determines faces should be rendered
                            double base = point3D.myPoint.getZ();
                            double plus1 = getGlobalHeightMapValue((int) point3D.myPoint.getX(), (int) point3D.myPoint.getY() + 1);
                            double minus1 = getGlobalHeightMapValue((int) point3D.myPoint.getX(), (int) point3D.myPoint.getY() - 1);

                            //First face set
                            if (plus1 != base || minus1 != base) {
                                float[] t12 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, t12) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t12));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w = {(float) point3D.myPoint.getX(), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, w) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 4);
                                        face = ArrayUtils.add(face, 4);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 8);
                                        face = ArrayUtils.add(face, 8);
                                    }
                                }

                                 */
                            }


                            //Second face set
                            if (plus1 != base || minus1 != base) {
                                float[] t1 = {(float) (point3D.myPoint.getX() - 1), (float) point3D.myPoint.getY(), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, t1) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t1));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w1 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) point3D.myPoint.getZ()};
                                if (getPointIndex(points, w1) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w1));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getZ());
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 5);
                                        face = ArrayUtils.add(face, 5);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 9);
                                        face = ArrayUtils.add(face, 9);
                                    }
                                }

                                 */
                            }


                            //Third face set
                            if (plus1 != base || minus1 != base) {
                                float[] t2 = {(float) (point3D.myPoint.getX() - 1), (float) point3D.myPoint.getY(), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, t2) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t2));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w2 = {(float) (point3D.myPoint.getX() - 1), (float) (point3D.myPoint.getY() - 1), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, w2) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w2));
                                else {
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getX() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 6);
                                        face = ArrayUtils.add(face, 6);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 10);
                                        face = ArrayUtils.add(face, 10);
                                    }
                                }

                                 */
                            }

                            //Fourth face set
                            if (plus1 != base || minus1 != base) {
                                float[] t3 = {(float) point3D.myPoint.getX(), (float) point3D.myPoint.getY(), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, t3) > -1)
                                    face = ArrayUtils.add(face, getPointIndex(points, t3));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getY());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face = ArrayUtils.add(face, points.length / 3 - 1);
                                }

                                float[] w3 = {(float) point3D.myPoint.getX(), (float) (point3D.myPoint.getY() - 1), (float) (point3D.myPoint.getZ() - 1)};
                                if (getPointIndex(points, w3) > -1)
                                    face1 = ArrayUtils.add(face1, getPointIndex(points, w3));
                                else {
                                    points = ArrayUtils.add(points, (float) point3D.myPoint.getX());
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getY() - 1));
                                    points = ArrayUtils.add(points, (float) (point3D.myPoint.getZ() - 1));
                                    face1 = ArrayUtils.add(face1, points.length / 3 - 1);
                                }
                                /*
                                switch (point3D.getBlockType()) {
                                    case GRASS -> {
                                        face1 = ArrayUtils.add(face1, 7);
                                        face = ArrayUtils.add(face, 7);
                                    }
                                    case DIRT -> {
                                        face1 = ArrayUtils.add(face1, 11);
                                        face = ArrayUtils.add(face, 11);
                                    }
                                }

                                 */
                            }

                            if (face.length == 8)
                                faces = ArrayUtils.addAll(faces, face);
                            if (face1.length == 8)
                                faces = ArrayUtils.addAll(faces, face1);
                        }
                    }
                }

              //  System.out.println(Arrays.toString(zFaces).substring(0, 300));
                this.zFaces = zFaces;
                meshPoints = points;
                zPlanarPoints = zPoints;
             //   System.out.println("Points: " + meshPoints.length);
               // System.out.println(ArrayUtils.toString(points));
                /*
                PolygonMesh mesh = new PolygonMesh(points, new float[]{0f, 0f}, faces);
                int[] smooth = new int[faces.length];
                Arrays.setAll(smooth,i -> i + 1);
                mesh.getFaceSmoothingGroups().setAll(smooth);

                setCullFace(CullFace.NONE);
                setMaterial(mat);
                try {
                    setMesh(mesh);
                } catch (Exception ignored) {
                }

                 */
                didChange = false;
            }


      //  });
      //  Main.executor.execute(t);

    }

    /**
     * For any given array of 3 coordinates, checks if it is present in the larger
     * points array that stores all points of a mesh as 3 individual float values, x, y, z.
     * @param mainArr The points array of the mesh
     * @param subArr An array of three xyz coordinates to search for in the points array
     * @return If the subArr is found within mainArr, the index of the first element
     * of subArr is returned else -1 is returned
     */
    private int getPointIndex(float[] mainArr, float[] subArr) {
        int out = -1;
        for (int i = 0; i < mainArr.length; i += 3) {
            float[] temp = {mainArr[i], mainArr[i + 1], mainArr[i + 2]};
            if (temp[0] == subArr[0] && temp[1] == subArr[1] && temp[2] == subArr[2]) {
                out = i / 3;
                break;
            }
        }
        return out;
    }

    /**
     * Given a chunk heightmap, interpolates between cubes to fill in vertical gaps in terrain generation.
     * Makes comparisons between the 4 cardinal cubes of each cube
     *       |-----|              |-----|
     *       |  d  |              |  a  |
     * |-----|-----|-----|        |-----|
     * |  c  |base |  a  |        |  ?  | <- unknown
     * |-----|-----|-----|  |-----|-----|
     *       |  b  |        | base|
     *       |-----|        |-----|
     */
    private List<Cube> getInterpolatedCubes() {
        List<Cube> copy = new GlueList<>(heightMapPointList);
        List<Cube> interpolation = Collections.synchronizedList(new GlueList<>());
        final List<Future<?>> futures = Collections.synchronizedList(new GlueList<>());
        for (Cube base : copy) {
            Future<?> f = Main.executor.submit(() -> {
                List<Cube> comparisons = new GlueList<>();
                comparisons.add(new Cube((int) base.myPoint.getX() + 1, (int) base.myPoint.getY(), getGlobalHeightMapValue((int)  base.myPoint.getX() + 1, (int) base.myPoint.getY())));
                comparisons.add(new Cube((int) base.myPoint.getX(), (int) base.myPoint.getY() + 1, getGlobalHeightMapValue((int)  base.myPoint.getX(), (int) base.myPoint.getY() + 1)));
                comparisons.add(new Cube((int) base.myPoint.getX() - 1, (int) base.myPoint.getY(), getGlobalHeightMapValue((int)  base.myPoint.getX() - 1, (int) base.myPoint.getY())));
                comparisons.add(new Cube((int) base.myPoint.getX(), (int) base.myPoint.getY() - 1, getGlobalHeightMapValue((int)  base.myPoint.getX(), (int) base.myPoint.getY() - 1)));

                for (Cube compare : comparisons) {
                    //Get the tallest column and the number of cubes to interpolate
                    int taller = (int) compare.myPoint.getZ() - (int) base.myPoint.getZ();
                    int numOfCubes = Math.abs(taller) - 1;
                    boolean compareTaller = taller > 0;

                    for (int j = 1; j < numOfCubes + 1; j++) {
                        Cube newCube;
                        if (compareTaller) {
                            newCube = new Cube((int) compare.myPoint.getX(), (int) compare.myPoint.getY(), (int) compare.myPoint.getZ() - j);
                        } else {
                            newCube = new Cube((int) base.myPoint.getX(), (int) base.myPoint.getY(), (int) base.myPoint.getZ() - j);
                        }
                        newCube.setBlockType(BlockType.DIRT);

                        if (!interpolation.contains(newCube))
                            interpolation.add(newCube);
                    }
                }
            });
            futures.add(f);
        }

        try {
            for (Future<?> w : futures) {
                w.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return interpolation;
    }

    /**
     * Retrieves the Z value for any given x,y column in any chunk
     * @param x coordinate of column
     * @param y coordinate of column
     * @return Returns the noise value which is scaled between 0 and CHUNK_HEIGHT
     */
    public static int getGlobalHeightMapValue(int x, int y) {
        //Affects height of terrain. A higher value will result in lower, smoother terrain while a lower value will result in
        // a rougher, raised terrain
        float var1 = 12;

        //Affects coalescence of terrain. A higher value will result in more condensed, sharp peaks and a lower value will result in
        //more smooth, spread out hills.
        double var2 = 0.01;

        float f = (1 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * var2), (y * var2)) / (var1 + 2)) //Noise Octave 1
                + (float) (0.5 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * (var2 * 2)), (y * (var2 * 2))) / (var1 + 4)) //Noise Octave 2
                + (float) (0.25 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * (var2 * 2)), (y * (var2 * 2))) / (var1 + 6)); //Noise Octave 3

        return (int) Math.floor(((f + 1) / 2) * (CHUNK_HEIGHT - 1));

    }
    /**
     * Since the location of each chunk is unique this is used as an identifier by the chunk
     * renderer to retrieve and insert chunks
     * @return The corner vertex of this chunks mesh view.
     */
    public Point3D getLocation() {
        return location;
    }

    public Region getRegion() {
        return new Region(
                (int) (location.getX() - Math.floorMod((int) location.getX(), 512)),
                (int) (location.getY() - Math.floorMod((int) location.getY(), 512)));
    }

    /*
    public float[] getVertices() {
        return meshPoints;
    }
    
     */
    public float[] getZPlanarPoints() {
        return zPlanarPoints;
    }
    public IntBuffer getZFaces() {
      //  updateMesh();
      //  System.out.println(Arrays.toString(zFaces));
        return BufferUtils.createIntBuffer(this.zFaces.length).put(this.zFaces).flip();
    }

    @Serial
    private void writeObject(ObjectOutputStream o) {
        writeChunk(o, this);
    }

    @Serial
    private void readObject(ObjectInputStream o) {
        this.heightMapPointList = readChunk(o).heightMapPointList;
    }

    private void writeChunk(OutputStream stream, Chunk c) {
        if (this.equals(c)) return;
        System.out.println("Writing chunk for " + c.getRegion());
        Main.executor.execute(() -> {
            try {
                FSTObjectOutput out = Main.getInstance().getObjectOutput(stream);
                c.setMaterial(null);
                c.setMesh(null);
                out.writeObject(c, Chunk.class);
                out.flush();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        });
    }

    public boolean didChange() {
        return didChange;
    }
    private Chunk readChunk(InputStream stream) {
        AtomicReference<Chunk> c = new AtomicReference<>();
        Main.executor.execute(() -> {
            FSTObjectInput in = Main.getInstance().getObjectInput(stream);
            try {
                c.set((Chunk) in.readObject(Chunk.class));
                stream.close();
            } catch (Exception ignored) {
            }

            try { in.close();
            } catch (Exception e) { e.printStackTrace(); }
        });
        return c.get();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Chunk chunk) {
            if (this.heightMapPointList.equals(chunk.heightMapPointList) && this.getLocation() == ((Chunk) obj).getLocation())
                return true;
        } else return false;
        return true;
    }

    @Override
    public String toString() {
        return "Chunk: (" + location.getX() + "," + location.getY() + ")";
    }
}