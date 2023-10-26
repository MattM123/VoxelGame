package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;
import com.marcuzzo.Texturing.TextureCoordinateStore;
import org.apache.commons.lang3.ArrayUtils;
import org.fxyz3d.shapes.polygon.PolygonMeshView;
import org.joml.Vector3f;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class Chunk extends PolygonMeshView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Vector3f location;
    public static final int CHUNK_BOUNDS = 16;
    public static final int CHUNK_HEIGHT = 320;
    private boolean didChange = false;
    private final List<Cube> cubes = new GlueList<>();
    private final int[][] heightMap = new int[CHUNK_BOUNDS][CHUNK_BOUNDS];
    private List<Cube> heightMapPointList = new GlueList<>();
    private boolean isInitialized = false;
    private static final Logger logger = Logger.getLogger("Logger");
    private float[] vertexCache = new float[0];
    private int[] elementCache = new int[0];

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
    public Chunk initialize(float x, float y, float z) {
        location = new Vector3f(x, y, z);
        //===================================
        //Generate chunk height map
        //===================================

        int xCount = 0;
        int zCount = 0;
        for (int x1 = (int) x; x1 < x + CHUNK_BOUNDS; x1++) {
            for (int y1 = (int) y; y1 < y + CHUNK_BOUNDS; y1++) {

                //Converts the raw noise value in the range of -1 to 1, to the range of 0 to 320 to match Z coordinate.
                int elevation = getGlobalHeightMapValue(x1, y1);

                heightMap[xCount][zCount] = elevation;
                Cube c = new Cube(x1, y1, elevation, BlockType.GRASS);
                heightMapPointList.add(c);

                zCount++;
                if (zCount > CHUNK_BOUNDS - 1)
                    zCount = 0;
            }
            xCount++;
            if (xCount > CHUNK_BOUNDS - 1)
                xCount = 0;
        }


        //How far down caves should start generating
        int caveStart = 50;

        //checks chunks for cubes to render based on noise value and heightmap
        for (int x1 = (int) x; x1 < x + CHUNK_BOUNDS; x1++) {
            for (int z1 = (int) z; z1 < z + CHUNK_BOUNDS; z1++) {
                for (int y1 = (int) y; y1 <= heightMap[xCount][zCount]; y1++) {

                    Cube c = new Cube(x1, y1, z1);
                //    c.f = OpenSimplex.noise3_ImproveXZ(RegionManager.WORLD_SEED, x1 * 0.05, y1 * 0.05, z1 * 0.05);
                 //   if (c.f > 0.00)
                        cubes.add(c);
              //      if (c.f <= 0.00 && y1 >= heightMap[xCount][zCount] - caveStart)
             //           cubes.add(c);

                    zCount++;
                    if (zCount > CHUNK_BOUNDS - 1)
                        zCount = 0;
                }
                xCount++;
                if (xCount > CHUNK_BOUNDS - 1)
                    xCount = 0;
            }
        }
        isInitialized = true;
        didChange = true;
        updateMesh();

        return this;
    }

    /**
     * Generates or updates chunk mesh when a block is removed, added, or otherwise modified
     */
    public void updateMesh() {

        if (cubes.size() > 0 && didChange) {

            //Every vertex contained inside the chunk mesh in no particular order
            float[] points = new float[0];

            //Contains all cubes and interpolations
            List<Cube> cList = getInterpolatedCubes();
            for (Cube c : cList) {
                if (c != null) {
                    if (!heightMapPointList.contains(c))
                        heightMapPointList.add(c);
                }
            }

            //Populating array that holds the surface points of the chunk.
            for (Cube cube : heightMapPointList) {
                float[] coordArr = {cube.getX(), cube.getY(), cube.getZ()};
                points = ArrayUtils.addAll(points, coordArr);
            }

            //Updates data caches when the chunk mesh has changed
            Future<Map.Entry<float[], int[]>> temp;
            temp = Main.executor.submit(() -> getChunkData().entrySet().stream().findFirst().orElse(null));
            try {
                vertexCache = temp.get().getKey();
                elementCache = temp.get().getValue();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
            didChange = false;
        }
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
                comparisons.add(new Cube((int) base.getX() + 1, (int) base.getY(), getGlobalHeightMapValue((int)  base.getX() + 1, (int) base.getY())));
                comparisons.add(new Cube((int) base.getX(), (int) base.getY() + 1, getGlobalHeightMapValue((int)  base.getX(), (int) base.getY() + 1)));
                comparisons.add(new Cube((int) base.getX() - 1, (int) base.getY(), getGlobalHeightMapValue((int)  base.getX() - 1, (int) base.getY())));
                comparisons.add(new Cube((int) base.getX(), (int) base.getY() - 1, getGlobalHeightMapValue((int)  base.getX(), (int) base.getY() - 1)));

                for (Cube compare : comparisons) {
                    //Get the tallest column and the number of cubes to interpolate
                    int taller = (int) compare.getY() - (int) base.getY();
                    int numOfCubes = Math.abs(taller) - 1;
                    boolean compareTaller = taller > 0;

                    for (int j = 1; j < numOfCubes + 1; j++) {
                        Cube newCube;
                        if (compareTaller) {
                            newCube = new Cube((int) compare.getX(), (int) compare.getY() -  j, (int) compare.getZ());
                        } else {
                            newCube = new Cube((int) base.getX(), (int) base.getY() - j, (int) base.getZ());
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
     * Retrieves the Y value for any given x,z column in any chunk
     * @param x coordinate of column
     * @param z coordinate of column
     * @return Returns the noise value which is scaled between 0 and CHUNK_HEIGHT
     */
    public static int getGlobalHeightMapValue(int x, int z) {
        //Affects height of terrain. A higher value will result in lower, smoother terrain while a lower value will result in
        // a rougher, raised terrain
        float var1 = 12;

        //Affects coalescence of terrain. A higher value will result in more condensed, sharp peaks and a lower value will result in
        //more smooth, spread out hills.
        double var2 = 0.01;

        float f = (1 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * var2), (z * var2)) / (var1 + 2)) //Noise Octave 1
                + (float) (0.5 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * (var2 * 2)), (z * (var2 * 2))) / (var1 + 4)) //Noise Octave 2
                + (float) (0.25 * OpenSimplex.noise2(RegionManager.WORLD_SEED, (x * (var2 * 2)), (z * (var2 * 2))) / (var1 + 6)); //Noise Octave 3

        return (int) Math.floor(((f + 1) / 2) * (CHUNK_HEIGHT - 1));

    }
    /**
     * Since the location of each chunk is unique this is used as an identifier by the chunk
     * renderer to retrieve and insert chunks
     * @return The corner vertex of this chunks mesh view.
     */
    public Vector3f getLocation() {
        return location;
    }

    public Region getRegion() {
        Region returnRegion = null;

        int x = (int) getLocation().x;
        int xLowerLimit = ((x / RegionManager.REGION_BOUNDS) * RegionManager.REGION_BOUNDS);
        int xUpperLimit;
        if (x < 0)
            xUpperLimit = xLowerLimit - RegionManager.REGION_BOUNDS;
        else
            xUpperLimit = xLowerLimit + RegionManager.REGION_BOUNDS;


        int z = (int) getLocation().z;
        int zLowerLimit = ((z / RegionManager.REGION_BOUNDS) * RegionManager.REGION_BOUNDS);
        int zUpperLimit;
        if (z < 0)
            zUpperLimit = zLowerLimit - RegionManager.REGION_BOUNDS;
        else
            zUpperLimit = zLowerLimit + RegionManager.REGION_BOUNDS;


        //Calculates region coordinates chunk inhabits
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
     * For each cube in a chunk, checks the 6 adjacent cubes to calculate which
     * block faces to render then stores and returns this vertex and element
     * information to send to the GPU for rendering.
     *
     * @return A map object consisting of the vertex and element information
     * required to graphically render each block face
     */
    public Map<float[], int[]> getChunkData() {
        float[] vertices = new float[0];
        int[] elements = new int[0];
        int elementCounter = 0;

        if (didChange || elementCache.length == 0 || vertexCache.length == 0) {

            //Calculate faces to render given cube origin
            for (int i = 0; i < heightMapPointList.size(); i++) {
                Cube cube = heightMapPointList.get(i);
                Cube c1 = null, c2 = null, c3 = null, c4 = null, c5 = null, c6 = null;

                for (Cube otherCube : heightMapPointList) {
                    if (otherCube.getX() == cube.getX() + 1 && otherCube.getY() == cube.getY() && otherCube.getZ() == cube.getZ()) {
                        c1 = otherCube;
                    } else if (otherCube.getX() == cube.getX() && otherCube.getY() == cube.getY() + 1 && otherCube.getZ() == cube.getZ()) {
                        c2 = otherCube;
                    } else if (otherCube.getX() == cube.getX() && otherCube.getY() == cube.getY() && otherCube.getZ() == cube.getZ() + 1) {
                        c3 = otherCube;
                    } else if (otherCube.getX() == cube.getX() - 1 && otherCube.getY() == cube.getY() && otherCube.getZ() == cube.getZ()) {
                        c4 = otherCube;
                    } else if (otherCube.getX() == cube.getX() && otherCube.getY() == cube.getY() - 1 && otherCube.getZ() == cube.getZ()) {
                        c5 = otherCube;
                    } else if (otherCube.getX() == cube.getX() && otherCube.getY() == cube.getY() && otherCube.getZ() == cube.getZ() - 1) {
                        c6 = otherCube;
                    }
                }

                //If c1 is null, positive X face should be rendered
                if (c1 == null) {
                    float[] origin = {cube.getX() + 1, cube.getY(), cube.getZ()};
                    TextureCoordinateStore right = cube.getBlockType().getRightCoords();
                    float[] posXFace = {
                            //Position                                  Color                       Texture
                            origin[0], origin[1] - 1, origin[2] - 1,    0.0f, 0.0f, 0.0f, 1.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0], origin[1], origin[2] - 1,        0.0f, 0.0f, 0.0f, 1.0f,     right.getTopRight()[0], right.getTopRight()[1],
                            origin[0], origin[1] - 1, origin[2],        0.0f, 0.0f, 0.0f, 1.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0], origin[1], origin[2],            0.0f, 0.0f, 0.0f, 1.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                    };
                    int[] posXElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, posXFace);
                    elements = ArrayUtils.addAll(elements, posXElements);
                }
            }
            elementCache = elements;
            vertexCache = vertices;
        }

        Map<float[], int[]> out = new HashMap<>();
        out.put(vertexCache, elementCache);
        return out;
    }

    @Serial
    private void writeObject(ObjectOutputStream o) {
        writeChunk(o, this);
    }

    @Serial
    private void readObject(ObjectInputStream o) {
        if (!isInitialized) {
            return;
        }
        this.heightMapPointList = readChunk(o).heightMapPointList;
    }

    private void writeChunk(OutputStream stream, Chunk c) {
        if (this.equals(c)) return;
        System.out.println("Writing chunk for " + c.getRegion());
        Main.executor.execute(() -> {
            try {
                FSTObjectOutput out = Main.getInstance().getObjectOutput(stream);
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
        return "Chunk[" + location.x + "," + location.z + "]";
    }
}