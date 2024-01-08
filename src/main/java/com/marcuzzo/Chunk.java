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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static com.marcuzzo.RegionManager.CHUNK_BOUNDS;
import static com.marcuzzo.RegionManager.CHUNK_HEIGHT;

public class Chunk extends PolygonMeshView implements Serializable {

//public class Chunk implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Vector3f location;
    private boolean rerender = false;
    private final List<Block> blocks = new GlueList<>();
    private final int[][] heightMap = new int[CHUNK_BOUNDS][CHUNK_BOUNDS];
    private List<Block> chunkBlocks = new GlueList<>();
    private boolean isInitialized = false;
    private static final Logger logger = Logger.getLogger("Logger");
    private int vbo = 0;
    private int ebo = 0;

    public Chunk() {

        //Chunk events executed by player
        //determine when a chunk should be re-rendered

        //Option 1: Use polygonmesh view with built-in listeners (might not even work
        //since object is the only reference to javafx in the whole project)
        //Option 2: Don't extend Chunk and implement custom listeners

        /*
        setOnMouseClicked(mouseEvent -> {
            rerender = true;
            mouseEvent.getPickResult().

            if (Player.getblock players looking at is in heightmap) {
              remove from heightmap
            else {
              add to heightmap
            });

        */
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
            for (int z1 = (int) z; z1 < z + CHUNK_BOUNDS; z1++) {

                //Converts the raw noise value in the range of -1 to 1, to the range of 0 to 320 to match Z coordinate.
                int elevation = getGlobalHeightMapValue(x1, z1);

                heightMap[xCount][zCount] = elevation;
                //Block c = new Block(x1, elevation, z1, BlockType.GRASS);
              //  chunkBlocks.add(c);

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

                    Block c = new Block(x1, y1, z1, BlockType.DIRT);
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
        populateChunk();
        sortBlocks();
        isInitialized = true;
        rerender = true;
       // updateMesh();

        return this;
    }

    /**
     * Regenerates this chunks heightmap if the chunk is marked for
     * re-rendering.

     * This might not even be used
     */
    public void updateMesh() {

        if (blocks.size() > 0 && rerender) {

            //Every vertex contained inside the chunk mesh in no particular order
            //   float[] points = new float[0];

            //Check if interpolations are already present in heightmap
           // List<Block> cList = getInterpolatedBlocks();
           // for (Block c : cList) {
           //     if (c != null) {
            //        if (!chunkBlocks.contains(c))
            //            chunkBlocks.add(c);
           //     }
           // }

            //Populating array that holds the surface points of the chunk.
        //    for (Block block : chunkBlocks) {
        //        float[] coordArr = {block.getLocation().x, block.getLocation().y, block.getLocation().z};
       //         points = ArrayUtils.addAll(points, coordArr);
      //      }

            //Updates data caches when the chunk mesh has changed
            //Future<RenderTask> temp;
            Main.executor.submit(this::getRenderTask);
          //  try {
              //  vertexCache = temp.get().getVertexData();
             //   elementCache = temp.get().getElementData();
         //   } catch (Exception e) {
          //      logger.warning(e.getMessage());
          //  }
        }
      //  for (int[] ints : heightMap) {
      //      System.out.println(Arrays.toString(ints));
     //   }

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
     * Given a 2D chunk heightmap, interpolates between
     * height-mapped blocks to fill in vertical gaps in terrain generation.
     * Populates this chunk with blocks by making comparisons between the
     * 4 cardinal blocks of each block.
     *       |-----|              |-----|
     *       |  d  |              |  a  |
     * |-----|-----|-----|        |-----|
     * |  c  |base |  a  |        |  ?  | <- unknown
     * |-----|-----|-----|  |-----|-----|
     *       |  b  |        | base|
     *       |-----|        |-----|
     */
    private void populateChunk() {
        //TODO: Determine BlockType based on noise
        //loop through heightmap
        chunkBlocks.clear();
        for (int row = 0; row < heightMap.length; row++) {
            for (int col = 0; col < heightMap[row].length; col++) {
                int base = heightMap[row][col];

                //horizontal comparisons
                int comparison1; //-1
                int comparison2; //+1

                //vertical comparisons
                int comparison3; //-1
                int comparison4; //+1

                //-1 horizontal comparison
                if (col > 0)
                    comparison1 = heightMap[row][col - 1];
                else
                    comparison1 = getGlobalHeightMapValue((int) (col + getLocation().x - 1), (int) (row + getLocation().z));

                //+1 horizontal comparison
                if (col < CHUNK_BOUNDS - 1)
                    comparison2 = heightMap[row][col + 1];
                else
                    comparison2 = getGlobalHeightMapValue((int) (col + getLocation().x + 1), (int) (row + getLocation().z));

                //-1 2d vertical comparison
                if (row > 0)
                    comparison3 = heightMap[row - 1][col];
                else
                    comparison3 = getGlobalHeightMapValue((int) (col + getLocation().x), (int) (row + getLocation().z - 1));

                //+1 2d vertical comparison
                if (row < CHUNK_BOUNDS - 1)
                    comparison4 = heightMap[row + 1][col];
                else
                    comparison4 = getGlobalHeightMapValue((int) (col + getLocation().x), (int) (row + getLocation().z + 1));

                //Adds base by default since that will always be visible and rendered
                if (!chunkBlocks.contains(new Block(col + getLocation().x, base, row + getLocation().z, BlockType.DIRT))) {
                    chunkBlocks.add(new Block(col + getLocation().x, base, row + getLocation().z, BlockType.DIRT));
                }


                //Populates chunk vertex list. Base needs to be larger than at least one
                //comparison for any vertical blocks to be added
                if (base > comparison1) {
                    if (base - comparison1 > 1) {
                        int numOfBlocks = base - comparison1;

                        for (int i = 0; i < numOfBlocks; i++) {
                            if (!chunkBlocks.contains(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT)))
                                chunkBlocks.add(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT));

                        }
                    }
                }

                if (base > comparison2) {
                    if (base - comparison2 > 1) {
                        int numOfBlocks = base - comparison2;

                        for (int i = 0; i < numOfBlocks; i++) {
                            if (!chunkBlocks.contains(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT)))
                                chunkBlocks.add(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT));

                        }
                    }
                }

                if (base > comparison3) {
                    if (base - comparison3 > 1) {
                        int numOfBlocks = base - comparison3;

                        for (int i = 0; i < numOfBlocks; i++) {
                            if (!chunkBlocks.contains(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT)))
                                chunkBlocks.add(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT));

                        }
                    }
                }

                if (base > comparison4) {
                    if (base - comparison4 > 1) {
                        int numOfBlocks = base - comparison4;

                        for (int i = 0; i < numOfBlocks; i++) {
                            if (!chunkBlocks.contains(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT)))
                                chunkBlocks.add(new Block(col + getLocation().x, base - i, row + getLocation().z, BlockType.DIRT));

                        }
                    }
                }
            }
        }
    }
    
    /**
     * Since the location of each chunk is unique this is used as an
     * identifier by the ChunkManager to retrieve, insert, and
     * effectively sort chunks.
     * @return The corner vertex of this chunk.
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
     * Sorts the chunks blocks to make accessing them more efficient.
     */
    private void sortBlocks() {
        chunkBlocks.sort(new BlockComparator());
    }

    /**
     * Generates or regenerates this Chunks RenderTask. The RenderTask is
     * used to graphically render the Chunk. Calling this method will
     * automatically update (if necessary) this chunks vertex and element data and
     * return a new RenderTask that can be passed to the GPU when drawing.
     *
     * @return A RenderTask whose regularly updated contents can be
     * used in GL draw calls to render this Chunk graphically
     */
    public RenderTask getRenderTask() {

        //TODO: in progress
        float[] vertices = new float[0];
        int[] elements = new int[0];
        int elementCounter = 0;

        if (rerender) {// || elementCache.length == 0 || vertexCache.length == 0) {

            //Calculate faces to render given block origin
            for (int i = 0; i < chunkBlocks.size(); i++) {
                Block block = chunkBlocks.get(i);

                Block c1 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x + 1, block.getLocation().y, block.getLocation().z));
                Block c2 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x, block.getLocation().y + 1, block.getLocation().z));
                Block c3 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x, block.getLocation().y, block.getLocation().z + 1));
                Block c4 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x - 1, block.getLocation().y, block.getLocation().z));
                Block c5 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x, block.getLocation().y - 1, block.getLocation().z));
                Block c6 = binarySearchBlockWithLocation(0, chunkBlocks.size() - 1, new Block(block.getLocation().x, block.getLocation().y, block.getLocation().z - 1));



                //If c1 is null, positive X face should be rendered
                if (c1 == null) {
                    float[] origin = {block.getLocation().x, block.getLocation().y, block.getLocation().z};
                    TextureCoordinateStore right = block.getBlockType().getRightCoords();
                    float[] posXFace = {
                            //Position (X, Y, Z)                        Color (R, G, B, A)          Texture (U, V)
                            origin[0] + 1, origin[1], origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0] + 1, origin[1] + 1, origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0] + 1, origin[1] + 1, origin[2] + 1,    0.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                            origin[0] + 1, origin[1], origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                    };
                    int[] posXElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, elementCounter, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, posXFace);
                    elements = ArrayUtils.addAll(elements, posXElements);
                }




                //If c2 is null, positive Y face should be rendered
                if (c2 == null) {
                    float[] origin = {block.getLocation().x, block.getLocation().y, block.getLocation().z};
                    TextureCoordinateStore right = block.getBlockType().getRightCoords();
                    float[] posYFace = {
                            //Position (X, Y, Z)                            Color (R, G, B, A)          Texture (U, V)
                            origin[0], origin[1] + 1, origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0], origin[1] + 1, origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0] + 1, origin[1] + 1, origin[2] + 1,    0.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                            origin[0] + 1, origin[1] + 1, origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                    };
                    int[] posYElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, elementCounter, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, posYFace);
                    elements = ArrayUtils.addAll(elements, posYElements);
                }
/*
                //If c3 is null, positive Z face should be rendered
                if (c3 == null) {
                    float[] origin = {block.getLocation().x, block.getLocation().y, block.getLocation().z + 1};
                    TextureCoordinateStore front = block.getBlockType().getFrontCoords();
                    float[] posZFace = {
                            //Position                                  Color                       Texture
                            origin[0], origin[1], origin[2] + 1,            0.0f, 0.0f, 0.0f, 0.0f,     front.getBottomLeft()[0], front.getBottomLeft()[1],
                            origin[0] + 1, origin[1], origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     front.getBottomRight()[0], front.getBottomRight()[1],
                            origin[0] + 1, origin[1] + 1, origin[2] + 1,    0.0f, 0.0f, 0.0f, 0.0f,     front.getTopLeft()[0], front.getTopLeft()[1],
                            origin[0], origin[1] + 1, origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     front.getTopRight()[0], front.getTopRight()[1],
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
                    float[] origin = {block.getLocation().x - 1, block.getLocation().y, block.getLocation().z};
                    TextureCoordinateStore right = block.getBlockType().getRightCoords();
                    float[] negXFace = {
                            //Position                                      Color                       Texture
                            origin[0], origin[1], origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0], origin[1], origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                            origin[0], origin[1] + 1, origin[2] + 1,    0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0], origin[1] + 1, origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                    };
                    int[] negXElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, negXFace);
                    elements = ArrayUtils.addAll(elements, negXElements);
                }



                //If c5 is null, negative Y face should be rendered
                if (c5 == null) {
                    float[] origin = {block.getLocation().x, block.getLocation().y, block.getLocation().z};
                    TextureCoordinateStore right = block.getBlockType().getTopCoords();
                    float[] negYFace = {
                            //Position (X, Y, Z)                            Color (R, G, B, A)          Texture (U, V)
                            origin[0], origin[1], origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomLeft()[0], right.getBottomLeft()[1],
                            origin[0], origin[1], origin[2] + 1,        0.0f, 0.0f, 0.0f, 0.0f,     right.getBottomRight()[0], right.getBottomRight()[1],
                            origin[0] + 1, origin[1], origin[2] + 1,    0.0f, 0.0f, 0.0f, 0.0f,     right.getTopLeft()[0], right.getTopLeft()[1],
                            origin[0] + 1, origin[1], origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     right.getTopRight()[0], right.getTopRight()[1],
                    };
                    int[] negYElements = {
                            elementCounter, elementCounter + 1, elementCounter + 2, elementCounter + 3, elementCounter, 80000
                    };
                    elementCounter += 4;

                    vertices = ArrayUtils.addAll(vertices, negYFace);
                    elements = ArrayUtils.addAll(elements, negYElements);
                }


                //If c6 is null, negative Z face should be rendered
                if (c6 == null) {
                    float[] origin = {block.getLocation().x, block.getLocation().y, block.getLocation().z - 1};
                    TextureCoordinateStore back = block.getBlockType().getBackCoords();
                    float[] negZFace = {
                            //Position                                      Color                       Texture
                            origin[0], origin[1], origin[2],            0.0f, 0.0f, 0.0f, 0.0f,     back.getBottomLeft()[0], back.getBottomLeft()[1],
                            origin[0], origin[1] + 1, origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     back.getBottomRight()[0], back.getBottomRight()[1],
                            origin[0] + 1, origin[1] + 1, origin[2],    0.0f, 0.0f, 0.0f, 0.0f,     back.getTopLeft()[0], back.getTopLeft()[1],
                            origin[0] + 1, origin[1], origin[2],        0.0f, 0.0f, 0.0f, 0.0f,     back.getTopRight()[0], back.getTopRight()[1],
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
        }
        return new RenderTask(vertices, elements, vbo, ebo, this);
    }

    /**
     * Searches for an index to insert a new Block at in O(log n) time complexity.
     * Ensures the list is sorted by the Blocks location as new Blocks are inserted into it.
     *
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param v The coordinate to search for.
     * @return Returns the Block that was just inserted into the list.
     */
    private Block binaryInsertBlockWithLocation(int l, int r, Vector3f v) {
        ChunkComparator comparator = new ChunkComparator();
        Block b = new Block(v.x, v.y, v.z);

        if (chunkBlocks.isEmpty()) {
            chunkBlocks.add(b);
        }
        if (chunkBlocks.size() == 1) {
            //Inserts element as first in list
            if (comparator.compare(v, chunkBlocks.get(0).getLocation()) < 0) {
                chunkBlocks.add(0, b);
                return b;
            }
            //Appends to end of list
            if (comparator.compare(v, chunkBlocks.get(0).getLocation()) > 0) {
                chunkBlocks.add(b);
                return b;
            }
        }

        if (r >= l && chunkBlocks.size() > 1) {
            int mid = l + (r - l) / 2;
            //When an index has been found, right and left will be very close to each other
            //Insertion of the right index will shift the right element
            //and all subsequent ones to the right.
            if (Math.abs(r - l) == 1) {
                chunkBlocks.add(r, b);
                return b;
            }

            //If element is less than first element insert at front of list
            if (comparator.compare(v, chunkBlocks.get(0).getLocation()) < 0) {
                chunkBlocks.add(0, b);
                return b;
            }
            //If element is more than last element insert at end of list
            if (comparator.compare(v, chunkBlocks.get(chunkBlocks.size() - 1).getLocation()) > 0) {
                chunkBlocks.add(b);
                return b;
            }

            //If the index is near the middle
            if (comparator.compare(v, chunkBlocks.get(mid - 1).getLocation()) > 0
                    && comparator.compare(b.getLocation(), chunkBlocks.get(mid).getLocation()) < 0) {
                chunkBlocks.add(mid, b);
                return b;
            }
            if (comparator.compare(v, chunkBlocks.get(mid + 1).getLocation()) < 0
                    && comparator.compare(v, chunkBlocks.get(mid).getLocation()) > 0) {
                chunkBlocks.add(mid + 1, b);
                return b;
            }

            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (comparator.compare(v, chunkBlocks.get(mid).getLocation()) < 0) {
                return binaryInsertBlockWithLocation(l, mid - 1, v);
            }

            // Else the element can only be present
            // in right subarray
            return binaryInsertBlockWithLocation(mid + 1, r, v);

        } else {
            return null;
        }
    }

    /**
     * Searches for a Block in O(log n) time complexity and returns it.
     *
     * @param l The farthest left index of the list
     * @param r The farthest right index of the list
     * @param v The coordinate to search for.
     * @return Returns the Block if found. Else null.
     */
    private Block binarySearchBlockWithLocation(int l, int r, Block v) {
        BlockComparator comparator = new BlockComparator();

        if (r >= l) {
            int mid = l + (r - l) / 2;

            // If the element is present at the middle
            if (comparator.compare(v, chunkBlocks.get(mid)) == 0) {
               // System.out.println("Found Equal: " + v + "   " + chunkBlocks.get(mid));
                return chunkBlocks.get(mid);
            }

            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (comparator.compare(v, chunkBlocks.get(mid)) < 0) {
                return binarySearchBlockWithLocation(l, mid - 1, v);
            }

            // Else the element can only be present
            // in right subarray
            if (comparator.compare(v, chunkBlocks.get(mid)) > 0) {
                return binarySearchBlockWithLocation(mid + 1, r, v);
            }
        }
        return null;

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
        this.chunkBlocks = readChunk(o).chunkBlocks;
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
     * @return True if this chunk should be re-rendered on the next frame, false if this chunk
     * has not been modified in any way and therefore should not be re-rendered.
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
            if (this.chunkBlocks.equals(chunk.chunkBlocks) && this.getLocation() == ((Chunk) obj).getLocation())
                return true;
        } else return false;
        return true;
    }

    @Override
    public String toString() {
        return "Chunk[" + location.x + "," + location.z + "]";
    }
}