package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;
import com.marcuzzo.Texturing.TextureCoordinateStore;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Vector3f;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

//public class Chunk extends PolygonMeshView implements Serializable {

public class Chunk implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Vector3f location;
    public static final int CHUNK_BOUNDS = 16;
    public static final int CHUNK_HEIGHT = 320;
    private boolean rerender = false;
    private final List<Block> blocks = new GlueList<>();
    private final int[][] heightMap = new int[CHUNK_BOUNDS][CHUNK_BOUNDS];
    private List<Block> heightMapPointList = new GlueList<>();
    private boolean isInitialized = false;
    private static final Logger logger = Logger.getLogger("Logger");
    private float[] vertexCache = new float[0];
    private int[] elementCache = new int[0];
    private int vbo = 0;
    private int ebo = 0;

    public Chunk() {

      //  setOnMouseClicked(mouseEvent -> {
     //       rerender = true;
     //       Main.executor.execute(this::updateMesh);
    //    });
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
                Block c = new Block(x1, y1, elevation, BlockType.GRASS);
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

        //checks chunks for blocks to render based on noise value and heightmap
        for (int x1 = (int) x; x1 < x + CHUNK_BOUNDS; x1++) {
            for (int z1 = (int) z; z1 < z + CHUNK_BOUNDS; z1++) {
                for (int y1 = (int) y; y1 <= heightMap[xCount][zCount]; y1++) {

                    Block c = new Block(x1, y1, z1);
                //    c.f = OpenSimplex.noise3_ImproveXZ(RegionManager.WORLD_SEED, x1 * 0.05, y1 * 0.05, z1 * 0.05);
                 //   if (c.f > 0.00)
                        blocks.add(c);
              //      if (c.f <= 0.00 && y1 >= heightMap[xCount][zCount] - caveStart)
             //           blocks.add(c);

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
        rerender = true;
        updateMesh();

        return this;
    }

    /**
     * Re-generates this chunks vertices, RenderTask, and marks this
     * chunk for re-rendering next frame. This occurs when a block is
     * removed, added, or otherwise modified
     */
    public void updateMesh() {

        if (blocks.size() > 0 && rerender) {

            //Every vertex contained inside the chunk mesh in no particular order
            float[] points = new float[0];

            //Contains all blocks and interpolations
            List<Block> cList = getInterpolatedBlocks();
            for (Block c : cList) {
                if (c != null) {
                    if (!heightMapPointList.contains(c))
                        heightMapPointList.add(c);
                }
            }

            //Populating array that holds the surface points of the chunk.
            for (Block block : heightMapPointList) {
                float[] coordArr = {block.getX(), block.getY(), block.getZ()};
                points = ArrayUtils.addAll(points, coordArr);
            }

            //Updates data caches when the chunk mesh has changed
            Future<RenderTask> temp;
            temp = Main.executor.submit(this::getRenderTask);
            try {
                vertexCache = temp.get().getVertexData();
                elementCache = temp.get().getElementData();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }

            //Flags chunk for rerendering
            rerender = true;
        }
    }

    /**
     * Given a chunk heightmap, interpolates between blocks to fill in vertical gaps in terrain generation.
     * Makes comparisons between the 4 cardinal blocks of each block
     *       |-----|              |-----|
     *       |  d  |              |  a  |
     * |-----|-----|-----|        |-----|
     * |  c  |base |  a  |        |  ?  | <- unknown
     * |-----|-----|-----|  |-----|-----|
     *       |  b  |        | base|
     *       |-----|        |-----|
     */
    private List<Block> getInterpolatedBlocks() {
        List<Block> copy = new GlueList<>(heightMapPointList);
        List<Block> interpolation = Collections.synchronizedList(new GlueList<>());
        final List<Future<?>> futures = Collections.synchronizedList(new GlueList<>());
        for (Block base : copy) {
            Future<?> f = Main.executor.submit(() -> {
                List<Block> comparisons = new GlueList<>();
                comparisons.add(new Block((int) base.getX() + 1, (int) base.getY(), getGlobalHeightMapValue((int)  base.getX() + 1, (int) base.getY())));
                comparisons.add(new Block((int) base.getX(), (int) base.getY() + 1, getGlobalHeightMapValue((int)  base.getX(), (int) base.getY() + 1)));
                comparisons.add(new Block((int) base.getX() - 1, (int) base.getY(), getGlobalHeightMapValue((int)  base.getX() - 1, (int) base.getY())));
                comparisons.add(new Block((int) base.getX(), (int) base.getY() - 1, getGlobalHeightMapValue((int)  base.getX(), (int) base.getY() - 1)));

                for (Block compare : comparisons) {
                    //Get the tallest column and the number of blocks to interpolate
                    int taller = (int) compare.getY() - (int) base.getY();
                    int numOfBlocks = Math.abs(taller) - 1;
                    boolean compareTaller = taller > 0;

                    for (int j = 1; j < numOfBlocks + 1; j++) {
                        Block newBlock;
                        if (compareTaller) {
                            newBlock = new Block((int) compare.getX(), (int) compare.getY() -  j, (int) compare.getZ());
                        } else {
                            newBlock = new Block((int) base.getX(), (int) base.getY() - j, (int) base.getZ());
                        }
                        newBlock.setBlockType(BlockType.DIRT);

                        if (!interpolation.contains(newBlock))
                            interpolation.add(newBlock);
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
     * Since the location of each chunk is unique this is used as an
     * identifier by the ChunkManager to retrieve and insert chunks
     * @return The corner vertex of this chunks mesh view.
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * If this region is currently visible to the player (in-memory), this region will
     * be returned populated with chunk data. This region will otherwise be an empty object
     * which would need to be populated with data by the RegionManager when the player
     * enters the region.
     *
     * @return Returns the region that the chunk belongs to.
     */
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
     * Generates or regenerates this chunks RenderTask. The RenderTask is
     * used to graphically render the Chunk. Calling this method will
     * automatically update this chunks vertex and element data and
     * return a new RenderTask that can be passed to the GPU through a draw call.
     *
     * @return A RenderTask that can be used in GL draw calls to render this Chunk
     */
    public RenderTask getRenderTask() {

        float[] vertices = new float[0];
        int[] elements = new int[0];
        int elementCounter = 0;

        //Refresh data caches if needed. Don't re-calculate block faces if not necessary
        if (rerender || elementCache.length == 0 || vertexCache.length == 0) {

            //Calculate faces to render given block origin
            for (int i = 0; i < heightMapPointList.size(); i++) {
                Block block = heightMapPointList.get(i);
                Block c1 = null, c2 = null, c3 = null, c4 = null, c5 = null, c6 = null;

                for (Block otherBlock : heightMapPointList) {
                    if (otherBlock.getX() == block.getX() + 1 && otherBlock.getY() == block.getY() && otherBlock.getZ() == block.getZ()) {
                        c1 = otherBlock;
                    } else if (otherBlock.getX() == block.getX() && otherBlock.getY() == block.getY() + 1 && otherBlock.getZ() == block.getZ()) {
                        c2 = otherBlock;
                    } else if (otherBlock.getX() == block.getX() && otherBlock.getY() == block.getY() && otherBlock.getZ() == block.getZ() + 1) {
                        c3 = otherBlock;
                    } else if (otherBlock.getX() == block.getX() - 1 && otherBlock.getY() == block.getY() && otherBlock.getZ() == block.getZ()) {
                        c4 = otherBlock;
                    } else if (otherBlock.getX() == block.getX() && otherBlock.getY() == block.getY() - 1 && otherBlock.getZ() == block.getZ()) {
                        c5 = otherBlock;
                    } else if (otherBlock.getX() == block.getX() && otherBlock.getY() == block.getY() && otherBlock.getZ() == block.getZ() - 1) {
                        c6 = otherBlock;
                    }
                }

                //If c1 is null, positive X face should be rendered
                if (c1 == null) {
                    float[] origin = {block.getX(), block.getY(), block.getZ()};
                    TextureCoordinateStore right = block.getBlockType().getRightCoords();
                    float[] posXFace = {
                            //Position                                  Color                       Texture
                            origin[0], origin[1] - 1, origin[2] - 1,    0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0], origin[1], origin[2] - 1,        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                            origin[0], origin[1] - 1, origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0], origin[1], origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                    };
                    int[] posXElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, posXFace);
                    elements = ArrayUtils.addAll(elements, posXElements);
                }

                /*
                //If c3 is null, positive Z face should be rendered
                if (c3 == null) {
                    float[] origin = {block.getX(), block.getY(), block.getZ() + 1};
                    TextureCoordinateStore front = block.getBlockType().getFrontCoords();
                    float[] posZFace = {
                            //Position                                  Color                       Texture
                            origin[0] - 1, origin[1] - 1, origin[2],    1.0f, 0.0f, 0.0f, 0.0f,     front.getBottomLeft()[0], front.getBottomLeft()[1],
                            origin[0], origin[1] - 1, origin[2],        1.0f, 0.0f, 0.0f, 0.0f,     front.getBottomRight()[0], front.getBottomRight()[1],
                            origin[0] - 1, origin[1], origin[2],        1.0f, 0.0f, 0.0f, 0.0f,     front.getTopLeft()[0], front.getTopLeft()[1],
                            origin[0], origin[1], origin[2],            1.0f, 0.0f, 0.0f, 0.0f,     front.getTopRight()[0], front.getTopRight()[1],
                    };
                    int[] posZElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, posZFace);
                    elements = ArrayUtils.addAll(elements, posZElements);
                }

                //If c4 is null, negative X face should be rendered
                if (c4 == null) {
                    float[] origin = {block.getX() - 1, block.getY(), block.getZ()};
                    TextureCoordinateStore right = block.getBlockType().getRightCoords();
                    float[] negXFace = {
                            //Position                                      Color                       Texture
                            origin[0] - 1, origin[1] - 1, origin[2] - 1,    1.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0] - 1, origin[1], origin[2] - 1,        1.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                            origin[0] - 1, origin[1] - 1, origin[2],        1.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0] - 1, origin[1], origin[2],            1.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                    };
                    int[] negXElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, negXFace);
                    elements = ArrayUtils.addAll(elements, negXElements);
                }

                //If c6 is null, negative Z face should be rendered
                if (c6 == null) {
                    float[] origin = {block.getX(), block.getY(), block.getZ() - 1};
                    TextureCoordinateStore back = block.getBlockType().getBackCoords();
                    float[] negZFace = {
                            //Position                                      Color                       Texture
                            origin[0] - 1, origin[1] - 1, origin[2] - 1,    1.0f, 0.0f, 0.0f, 0.0f,     back.getBottomLeft()[0], back.getBottomLeft()[1],
                            origin[0], origin[1] - 1, origin[2] - 1,        1.0f, 0.0f, 0.0f, 0.0f,     back.getBottomRight()[0], back.getBottomRight()[1],
                            origin[0] - 1, origin[1], origin[2] - 1,        1.0f, 0.0f, 0.0f, 0.0f,     back.getTopLeft()[0], back.getTopLeft()[1],
                            origin[0], origin[1], origin[2] - 1,            1.0f, 0.0f, 0.0f, 0.0f,     back.getTopRight()[0], back.getTopRight()[1],
                    };
                    int[] negZElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, negZFace);
                    elements = ArrayUtils.addAll(elements, negZElements);
                }
                 */
            }
            elementCache = elements;
            vertexCache = vertices;
        }

        return new RenderTask(vertexCache, elementCache, vbo, ebo, this);
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

    /**
     * If any of the Chunk vertices have been modified, this will mark the chunk
     * for re-rendering which will cause the RenderTask to be regenerated and included
     * in a draw call.
     *
     * @return True if this chunk should be re-rendered due to change, false if this chunk
     * has not been modified in any way.
     */
    public boolean shouldRerender() {
        return rerender;
    }

    /**
     * Set weather this Chunk should be marked for re-render or not
     * @param b True if this Chunk should be re-rendered, false otherwise
     */
    public void setRerender(boolean b) {
        rerender = b;
    }

    /**
     * Element Buffer Object specific to this chunk used in the
     * Chunks RenderTask and for drawing the chunks data
     * @param i EBO ID. Ideally using glGenBuffers() as the parameter
     */
    public void setEbo(int i) {
        ebo = i;
    }

    /**
     * Vertex Buffer Object specific to this chunk used in the
     * Chunks RenderTask and for drawing the chunks data
     * @param i VBO ID. Ideally using glGenBuffers() as the parameter
     */
    public void setVbo(int i) {
        vbo = i;
    }

    /**
     * Each Chunk is identified by its location in three-dimensional space,
     * this value is unique to all chunks and is therefore used to compare
     * Chunk objects to each other in conjunction with the chunks heightmap.
     * Two chunks will be equal if their location and heightmap are the same.
     *
     * @param obj The object to compare
     * @return True if the chunks are equal, false if not
     */
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