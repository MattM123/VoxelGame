package com.marcuzzo;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import org.lwjgl.openvr.Texture;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class RegionManager extends GlueList<Region> {
    private static Player player;
    public static List<Region> visibleRegions = new GlueList<>();
    public static Path worldDir;
   // public static GlueList<Texture> textures = null;
    public static TextureAtlas textures1 = null;
    public static ChunkRenderer renderer;
    public static final int RENDER_DISTANCE = 4;
    public static final int CHUNK_BOUNDS = 16;
    public static final long WORLD_SEED = 1234567890;

    /**
     * The highest level object representation of a world
     *
     * @param path The path of this worlds directory
     */
    public RegionManager(Path path) {
        if (RegionManager.player == null)
            RegionManager.player = new Player();

        try {
            Files.createDirectories(Paths.get(worldDir + "\\regions\\"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        RegionManager.worldDir = path;

        //Add world origin region when creating new world
       // enterRegion(new Region(0, 0));

        //load chunks in region player is in
        if (renderer == null) {
            renderer = new ChunkRenderer(RENDER_DISTANCE, Chunk.CHUNK_BOUNDS,
                    RegionManager.getRegionWithPlayer().getChunkWithPlayer(), RegionManager.getPlayer());
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


    /*
    public static Point2D getRegionCoordsWithPlayer() {
        Point2D loc = new Point2D(player.getPosition().x , player.getPosition().y);
        return new Point2D(loc.getX() - Math.floorMod((int) loc.getX(), 512), loc.getY() - Math.floorMod((int) loc.getY(), 512));
    }

     */

    public static Player getPlayer() {
        return player;
    }
    public static void setPlayer(Player player) {
        RegionManager.player = player;
    }

    /**
     * Gets the region that the player currently inhabits.
     * @return The region that the player is in
     */
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

       // System.out.println("ttt");
        enterRegion(playerRegion);


        return playerRegion;

    }

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
                System.out.println("[Exiting Region] " + r);
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
                System.out.println("[Exiting Region] " + r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates or loads an already generated region from filesystem when the players
     * render distance intersects with the regions bounds.
     */
    public static void enterRegion(Region r) {
        //If region is already visible
        if (visibleRegions.contains(r)) {
           // System.out.println(r + " already entered. Skipping.");
          //  r.updateChunks();
            return;
        }

        //Gets region from files if it's written to file but not visible
        try {
            File[] regionFiles = new File(worldDir + "\\regions\\").listFiles((dir, name) ->
                    name.equals((int) r.regionBounds.getX() + "." + (int) r.regionBounds.getY() + ".dat"));

            Region match = visibleRegions.stream().filter(p -> p.regionBounds.getX() == r.regionBounds.getX()
                    && p.regionBounds.getY() == r.regionBounds.getY()).findFirst().orElse(
                    new Region((int) r.regionBounds.getX(), (int) r.regionBounds.getY()));

            assert regionFiles != null;
            if (!visibleRegions.contains(match) && !Arrays.stream(regionFiles).toList().isEmpty()) {
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
                    }
                });
                if (q.get() != null && !visibleRegions.contains(q.get())) {
                    visibleRegions.add(q.get());
                    System.out.println("[Entering Region] " + r);
               //     q.get().updateChunks();
                    return;
                }
            }

            //if region is not visible and not written to files creates new region
            else if (!visibleRegions.contains(match) || Arrays.stream(regionFiles).toList().isEmpty()) {
                visibleRegions.add(r);
                System.out.println("[Entering Region] " + r);
             //   r.updateChunks();
                return;
            }

        } catch (Exception e) {
            if (!visibleRegions.contains(r)) {
                visibleRegions.add(r);
                System.out.println("[Entering Region] " + r);
            //    r.updateChunks();
                return;
            }
        }
        if (!visibleRegions.contains(r)) {
            System.out.println("[Entering Region] " + r);
            Region q = new Region((int) r.regionBounds.getX(), (int) r.regionBounds.getY());
            visibleRegions.add(q);
        }

    }

    /**
     * Updates the regions surrounding the player and reads them from file if in render distance.
     * Also removes and writes regions to file that are no longer in render distance.
     */
    public static void updateVisibleRegions() {

        //Updates regions within render distance
        ChunkRenderer.getChunksToRender();


        if (visibleRegions.size() > 0) {
            for (int i = 0; i < visibleRegions.size(); i++) {
                leaveRegion(visibleRegions.get(i));
            }
            for (int i = 0; i < ChunkRenderer.getRegions().size(); i++) {
                enterRegion(ChunkRenderer.getRegions().get(i));
            }

            for (int i = 0; i < visibleRegions.size(); i++) {
                visibleRegions.get(i).updateChunks();
            }
        }

       // System.out.println("Visible regions: " + visibleRegions);
    }
}
