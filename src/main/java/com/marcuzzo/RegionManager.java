package com.marcuzzo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class RegionManager extends GlueList<Region> {
    public static BufferedImage textureAtlas;
    private static Player player;
    public static List<Region> visibleRegions = new GlueList<>();
    public static Path worldDir;
   // public static GlueList<Texture> textures = null;
    public static TextureAtlas textures1 = null;
    public static ChunkRenderer renderer;
    public static final int RENDER_DISTANCE = 4;
    public static final int CHUNK_BOUNDS = 16;
    public static final long WORLD_SEED = 1234567890;
    private static final Logger logger = Logger.getLogger("Logger");

    /**
     * The highest level object representation of a world
     *
     * @param path The path of this worlds directory
     */
    public RegionManager(Path path) {
        //Loads texture atlas
        try {
            textureAtlas = ImageIO.read(new File("src/main/resources/textures/texture_atlas.png"));
        } catch (IOException e) {
            logger.warning("Unable to load texture atlas: " + e.getMessage());
        }

        if (Window.getPlayer() == null)
            Window.setPlayer(new Player());

        try {
            Files.createDirectories(Paths.get(worldDir + "\\regions\\"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        RegionManager.worldDir = path;

        //load chunks in region player is in
        if (renderer == null) {
            renderer = new ChunkRenderer(RENDER_DISTANCE, Chunk.CHUNK_BOUNDS,
                    Player.getChunk());
        }


       // Map<String, Image> textureMap = new HashMap<>();
        try {
            DirectoryStream<Path> textureStream = Files.newDirectoryStream(Paths.get("src/main/resources/textures"));
            Iterator<Path> textureIterator = textureStream.iterator();
            int dirLen = Objects.requireNonNull(new File("src/main/resources/textures").listFiles()).length;

            if (dirLen > 0) {
                textureIterator.forEachRemaining(c -> {
                    String name = c.getFileName().toString();
              //      String realName = name.substring(0, name.length() - 4);
                    /*
                       try {
                           textures.add(TextureIO.newTexture(TextureIO.newTextureData(GLProfile.getDefault(), new File("src/main/resources/textures/" + name), false, "png")));
                       } catch (IOException e) {
                           throw new RuntimeException(e);
                       }

                     */

                //    textureMap.put(realName, new Image("src/main/resources/textures/" + name));
                });
            //    textures1 = new TextureAtlas(textureMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getEmpty() {
       return visibleRegions.isEmpty();
    }

    /*
    public static Point2D getRegionCoordsWithPlayer() {
        Point2D loc = new Point2D(player.getPosition().x , player.getPosition().y);
        return new Point2D(loc.getX() - Math.floorMod((int) loc.getX(), 512), loc.getY() - Math.floorMod((int) loc.getY(), 512));
    }

     */

 //   /**
 //    * Gets the region that the player currently inhabits.
 //    * @return The region that the player is in
  //   */
    /*
    public static Region getRegionWithPlayer() {
        Region playerRegion;

        Point2D q = new Point2D(player.getPosition().x - Math.floorMod((int) player.getPosition().x, 512),
                player.getPosition().y - Math.floorMod((int) player.getPosition().y, 512));


        for (Region r : RegionManager.visibleRegions) {
            if ((int) r.regionBounds.getX() == (int) q.getX()
                    && (int) r.regionBounds.getY() == (int) q.getY()) {
                return r;
            }
        }

        //Returns new region if one does not exist
        playerRegion = new Region((int) q.getX(), (int) q.getY());

       //Enters region if not found in visible region list
        enterRegion(playerRegion);


        return playerRegion;

    }

     */

    /*
    /**
     * Gets the coordinates of the region the player inhabits.
     * @return The region coordinates
     */
    /*
    public static Region getRegionFromLocation(int x, int y) {
        Point2D p = new Point2D(x - Math.floorMod(x, 512), y - Math.floorMod(y, 512));
        return new Region((int) p.getX(), (int) p.getY());
    }

     */


    /**
     * Removes a region from the visible regions once a player leaves a region and
     * their render distance no longer overlaps it.
     * Also writes region to file in the process.
     *
     * @param r The region to leave
     */
    public static void leaveRegion(Region r) {
        try {
            File[] regionFiles = new File(worldDir + "\\regions\\").listFiles((dir, name) ->
                    name.equals((int) r.regionBounds.getX() + "." + (int) r.regionBounds.getY() + ".dat"));

            //Writes region to file and removes from visibility
            //If region file already exists
            assert regionFiles != null;
            if (!Arrays.stream(regionFiles).toList().isEmpty()) {
                FileOutputStream f = new FileOutputStream(Arrays.stream(regionFiles).toList().get(0));
                ObjectOutputStream o = new ObjectOutputStream(f);
                Main.executor.execute(() -> {
                    try {
                        o.writeObject(r);
                        o.close();
                        f.close();
                    } catch (IOException ignored) {
                        //throw new RuntimeException(e);
                    }
                });
                visibleRegions.remove(r);
                System.out.println("[Exiting Region1] " + r);
            }
            //If region file does not already exist
            else {
                FileOutputStream f = new FileOutputStream(worldDir + "\\regions\\"
                        + (int) r.regionBounds.getX() + "." + (int) r.regionBounds.getY() + ".dat");
                ObjectOutputStream o = new ObjectOutputStream(f);
                Main.executor.execute(() -> {
                    try {
                        o.writeObject(r);
                        o.close();
                        f.close();
                    } catch (IOException ignored) {
                    }
                });
                visibleRegions.remove(r);
                System.out.println("[Exiting Region2] " + r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates or loads an already generated region from filesystem when the players
     * render distance intersects with the regions bounds.
     */
    public static Region enterRegion(Region r) {
        //If region is already visible
        if (visibleRegions.contains(r)) {
            return r;
        }

        //Gets region from files if it's written to file but not visible
        try {
            //Check if region is already in files
            File[] regionFiles = new File(worldDir + "\\regions\\").listFiles((dir, name) ->
                    name.equals((int) r.regionBounds.getX() + "." + (int) r.regionBounds.getY() + ".dat"));

            //Get region from files if it exists
            Region match = visibleRegions.stream().filter(p -> p.regionBounds.getX() == r.regionBounds.getX()
                    && p.regionBounds.getY() == r.regionBounds.getY()).findFirst().orElse(
                    new Region((int) r.regionBounds.getX(), (int) r.regionBounds.getY()));

            assert regionFiles != null;
            if (!visibleRegions.contains(match) && regionFiles.length > 0) {

                FileInputStream f = new FileInputStream(worldDir + "\\regions\\"
                        + (int) r.regionBounds.getX() + "." + (int) r.regionBounds.getY() + ".dat");
                AtomicReference<Region> q = new AtomicReference<>();
                ObjectInputStream o = new ObjectInputStream(f);


                Main.executor.execute(() -> {
                    try {
                        q.set((Region) o.readObject());
                        f.close();
                        o.close();
                    } catch (Exception ignored) {
                    //    System.out.println("Error Message: " +  ignored.getMessage());
                    //    ignored.printStackTrace();
                    }
                });
                if (q.get() != null && !visibleRegions.contains(q.get())) {
                    visibleRegions.add(q.get());
                    System.out.println("[Entering Region1] " + r);
                    return q.get();
                }
            }

            //if region is not visible and not written to files creates new region
            else if (!visibleRegions.contains(match) && regionFiles.length == 0) {
                visibleRegions.add(r);
                System.out.println("[Entering Region2] " + r);
                return r;
            }

        } catch (Exception e) {
            if (!visibleRegions.contains(r)) {
                visibleRegions.add(r);
                System.out.println("[Entering Region3] " + r);
                return r;
            }
        }
        if (!visibleRegions.contains(r)) {
            System.out.println("[Entering Region4] " + r);
            Region q = new Region((int) r.regionBounds.getX(), (int) r.regionBounds.getY());

            //Adds chunks to new empty region
            List<Chunk> chunks = ChunkRenderer.getChunksToRender();
            for (Chunk chunk : chunks) {
                if (chunk.getRegion().regionBounds.x == r.regionBounds.getX()
                        && chunk.getRegion().regionBounds.getY() == r.regionBounds.getY()) {
                    q.add(chunk);
                }
            }

            visibleRegions.add(q);
            return q;
        }
        return r;

    }


    /**
     * Updates the regions surrounding the player and reads them from file if in render distance.
     * Also removes and writes regions to file that are no longer in render distance.
     */
    public static void updateVisibleRegions() {

        //Updates regions within render distance
        ChunkRenderer.getChunksToRender();
        List<Region> updatedRegions = ChunkRenderer.getRegions();

        if (visibleRegions.size() > 0) {
            System.out.println("[Updating Regions...]");

            //Retrieves from file or generates any region that is visible
            for (int i = 0; i < updatedRegions.size(); i++) {
                if (!visibleRegions.contains(updatedRegions.get(i)))
                    enterRegion(updatedRegions.get(i));

            }

            //Write to file and de-render any regions that are no longer visible
            for (int i = 0; i < visibleRegions.size(); i++) {
                if (!updatedRegions.contains(visibleRegions.get(i))) {
                    //System.out.println("UPR: " + updatedRegions.get(i));
                    // if (visibleRegions.get(i).regionBounds.getY() % 512 == 0 && visibleRegions.get(i).regionBounds.getX() % 512 == 0) {
                    //    System.out.println(updatedRegions.get(i).regionBounds.getX()  + "           " + updatedRegions.get(i).regionBounds.getY() );
                    leaveRegion(visibleRegions.get(i));
                }
            }
        }
    }
}
