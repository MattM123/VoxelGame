package com.marcuzzo;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;


public class ChunkCache {
    private static int renderDistance;
    public static int bounds = RegionManager.CHUNK_BOUNDS;
    private static Chunk playerChunk;
    private static final List<Region> regions = new GlueList<>();

    /**
     *
     * Updates, stores, and returns a list of in-memory chunks that should be rendered around a player at
     * a certain point in time.
     * @param bounds The length and width of the square chunk.
     * @param playerChunk The chunk a player inhabits.
     */
    public ChunkCache(int renderDistance, int bounds, Chunk playerChunk) {
        ChunkCache.renderDistance = renderDistance;
        ChunkCache.bounds = bounds;
        ChunkCache.playerChunk = playerChunk;
    }

    public static void setBounds(int bounds) {
        ChunkCache.bounds = bounds;
    }
    public static void setRenderDistance(int renderDistance) {
        ChunkCache.renderDistance = renderDistance;
    }
    public static void setPlayerChunk(Chunk c) {
        playerChunk = c;
    }
    /**
     * Gets the chunks diagonally oriented from the chunk the player is in.
     * This includes each 4 quadrants surrounding the player. This does not include the chunks
     * aligned straight out from the player.
     *
     * @return A list of chunks that should be rendered diagonally from the chunk the
     * player is in.
     */
    private static List<Chunk> getQuadrantChunks() {
        List<Chunk> chunks = new ArrayList<>();
        Region playerRegion = Player.getRegionWithPlayer();

        //Top left quadrant
        Vector3f TLstart = new Vector3f(playerChunk.getLocation().x - bounds, 0, playerChunk.getLocation().z + bounds);
        for (int x = (int) TLstart.x; x > TLstart.x - (renderDistance * bounds); x -= bounds) {
            for (int z = (int) TLstart.z; z < TLstart.z + (renderDistance * bounds); z += bounds) {
                Chunk c = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));
                if (c != null) {
                    chunks.add(c);
                    if (!regions.contains(c.getRegion()))
                        regions.add(c.getRegion());
                } else {
                    //Attempts to get chunk from region
                    Chunk get = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));

                    //If chunk already exists in region, add it
                    if (get != null) {
                        chunks.add(get);
                        if (!regions.contains(get.getRegion()))
                            regions.add(get.getRegion());

                        //If chunk does not already exist in region, create and add it
                    } else {
                        Chunk w = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, new Vector3f(x, 0, z));
                        if (w != null) {
                            chunks.add(w);

                            if (!regions.contains(w.getRegion()))
                                regions.add(w.getRegion());
                        }
                    }
                }
            }
        }


            //Top right quadrant
            Vector3f TRStart = new Vector3f(playerChunk.getLocation().x + bounds, 0,  playerChunk.getLocation().z + bounds);
            for (int x = (int) TRStart.x; x < TRStart.x + (renderDistance * bounds); x += bounds) {
                for (int z = (int) TRStart.z; z < TRStart.z + (renderDistance * bounds); z += bounds) {
                    Chunk c = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));
                    if (c != null) {
                        chunks.add(c);
                        if (!regions.contains(c.getRegion()))
                            regions.add(c.getRegion());
                    } else {
                        //Attempts to get chunk from region
                        Chunk get = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));

                        //If chunk already exists in region, add it
                        if (get != null) {
                            chunks.add(get);
                            if (!regions.contains(get.getRegion()))
                                regions.add(get.getRegion());


                            //If chunk does not already exist in region, create and add it
                        } else {
                            Chunk w = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, new Vector3f(x, 0, z));
                            if (w != null) {
                                chunks.add(w);

                                if (!regions.contains(w.getRegion()))
                                    regions.add(w.getRegion());
                            }
                        }
                    }
                }
            }

            //Bottom right quadrant
            Vector3f BRStart = new Vector3f(playerChunk.getLocation().x - bounds, 0, playerChunk.getLocation().z - bounds);
            for (int x = (int) BRStart.x; x > BRStart.x - (renderDistance * bounds); x -= bounds) {
                for (int z = (int) BRStart.z; z > BRStart.z - (renderDistance * bounds); z -= bounds) {
                    Chunk c = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));
                    if (c != null) {
                        chunks.add(c);
                        if (!regions.contains(c.getRegion()))
                            regions.add(c.getRegion());
                    } else {
                        //Attempts to get chunk from region
                        Chunk get = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));

                        //If chunk already exists in region, add it
                        if (get != null) {
                            chunks.add(get);
                            if (!regions.contains(get.getRegion()))
                                regions.add(get.getRegion());

                            //If chunk does not already exist in region, create and add it
                        } else {
                            Chunk w = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, new Vector3f(x, 0, z));
                            if (w != null) {
                                chunks.add(w);

                                if (!regions.contains(w.getRegion()))
                                    regions.add(w.getRegion());
                            }
                        }
                    }
                }
            }

            //Bottom left quadrant
            Vector3f BLStart = new Vector3f(playerChunk.getLocation().x + bounds, 0, playerChunk.getLocation().z - bounds);
            for (int x = (int) BLStart.x; x < BLStart.x + (renderDistance * bounds); x += bounds) {
                for (int z = (int) BLStart.z; z > BLStart.z - (renderDistance * bounds); z -= bounds) {
                    Chunk c = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));
                    if (c != null) {
                        chunks.add(c);
                        if (!regions.contains(c.getRegion()))
                            regions.add(c.getRegion());
                    } else {
                        //Attempts to get chunk from region
                        Chunk get = playerRegion.getChunkWithLocation(new Vector3f(x, 0, z));

                        //If chunk already exists in region, add it
                        if (get != null) {
                            chunks.add(get);
                            if (!regions.contains(get.getRegion()))
                                regions.add(get.getRegion());


                        //If chunk does not already exist in region, create and add it
                        } else {
                            Chunk w = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, new Vector3f(x, 0, z));
                            if (w != null) {
                                chunks.add(w);

                                if (!regions.contains(w.getRegion()))
                                    regions.add(w.getRegion());
                            }
                        }
                    }
                }
            }

        return chunks;
    }
    /**
     * Gets the chunks that should be rendered along the X And Y axis. E.x a renderer distance
     * of 2 would return 8 chunks, 2 on every side of the player in each cardinal direction
     *
     * @return A list of chunks that should be rendered in x, y, -x, and -y directions
     */
    private static ArrayList<Chunk> getCardinalChunks() {
        ArrayList<Chunk> chunks = new ArrayList<>();
        Region playerRegion = Player.getRegionWithPlayer();

        //Positive X
        for (int i = 1; i <= renderDistance; i++) {
            Vector3f p = new Vector3f(playerChunk.getLocation().x + (i * bounds), 0,  playerChunk.getLocation().z);
            Chunk c =  playerRegion.getChunkWithLocation(p);
            if (c != null) {
                chunks.add(c);
                if (!regions.contains(c.getRegion()))
                    regions.add(c.getRegion());
            } else {
                //Search for existing chunk in region
                Chunk get = playerRegion.binarySearchChunkWithLocation(0, playerRegion.size() - 1, p);

                //If chunk does not exist, create and add it
                if (get == null) {
                    Chunk d = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, p);
                    if (d != null) {
                        chunks.add(d);

                        if (!regions.contains(d.getRegion()))
                            regions.add(d.getRegion());
                    }
                //If region already has chunk, get chunk do not add
                } else {
                    chunks.add(get);
                    if (!regions.contains(get.getRegion()))
                        regions.add(get.getRegion());
                }
            }
        }

        //Negative X
        for (int i = 1; i <= renderDistance; i++) {
            Vector3f p = new Vector3f(playerChunk.getLocation().x - (i * bounds), 0, playerChunk.getLocation().z);
            Chunk c =  playerRegion.getChunkWithLocation(p);
            if (c != null) {
                chunks.add(c);
                if (!regions.contains(c.getRegion()))
                    regions.add(c.getRegion());
            }
            else {
                //Search for existing chunk in region
                Chunk get = playerRegion.binarySearchChunkWithLocation(0, playerRegion.size() - 1, p);

                //If chunk does not exist, create and add it
                if (get == null) {
                    Chunk d = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, p);
                    if (d != null) {
                        chunks.add(d);

                        if (!regions.contains(d.getRegion()))
                            regions.add(d.getRegion());
                    }
                    //If region already has chunk, get chunk do not add
                } else {
                    chunks.add(get);
                    if (!regions.contains(get.getRegion()))
                        regions.add(get.getRegion());
                }
            }
        }

        //Positive Y
        for (int i = 1; i <= renderDistance; i++) {
            Vector3f p = new Vector3f(playerChunk.getLocation().x, 0, playerChunk.getLocation().z + (i * bounds));
            Chunk c = playerRegion.getChunkWithLocation(p);
            if (c != null) {
                chunks.add(c);
                if (!regions.contains(c.getRegion()))
                    regions.add(c.getRegion());
            }
            else {
                //Search for existing chunk in region
                Chunk get = playerRegion.binarySearchChunkWithLocation(0, playerRegion.size() - 1, p);

                //If chunk does not exist, create and add it
                if (get == null) {
                    Chunk d = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, p);
                    if (d != null) {
                        chunks.add(d);

                        if (!regions.contains(d.getRegion()))
                            regions.add(d.getRegion());
                    }
                    //If region already has chunk, get chunk do not add
                } else {
                    chunks.add(get);
                    if (!regions.contains(get.getRegion()))
                        regions.add(get.getRegion());
                }
            }
        }
        //Negative Y
        for (int i = 1; i <= renderDistance; i++) {
            Vector3f p = new Vector3f(playerChunk.getLocation().x, 0, playerChunk.getLocation().z - (i * bounds));
            Chunk c = playerRegion.getChunkWithLocation(p);
            if (c != null) {
                chunks.add(c);
                if (!regions.contains(c.getRegion()))
                    regions.add(c.getRegion());
            }
            else {
                //Search for existing chunk in region
                Chunk get = playerRegion.binarySearchChunkWithLocation(0, playerRegion.size() - 1, p);

                //If chunk does not exist, create and add it
                if (get == null) {
                    Chunk d = playerRegion.binaryInsertChunkWithLocation(0, playerRegion.size() - 1, p);
                    if (d != null) {
                        chunks.add(d);

                        if (!regions.contains(d.getRegion()))
                            regions.add(d.getRegion());
                    }
                    //If region already has chunk, get chunk do not add
                } else {
                    chunks.add(get);
                    if (!regions.contains(get.getRegion()))
                        regions.add(get.getRegion());
                }
            }
        }
        return chunks;
    }

    /**
     * Returns a list of chunks that should be rendered around a player based on a render distance value
     * and updates chunks that surround a player in a global scope.
     * @return The list of chunks that should be rendered.
     */
    public static GlueList<Chunk> getChunksToRender() {
        regions.clear();
        GlueList<Chunk> chunks = new GlueList<>();
        chunks.addAll(getQuadrantChunks());
        chunks.addAll(getCardinalChunks());
        chunks.add(playerChunk);


        return chunks;
    }

    public static List<Region> getRegions() {
        return regions;
    }

}
