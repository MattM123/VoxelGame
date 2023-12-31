package com.marcuzzo;

import com.marcuzzo.Texturing.TextureLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RegionManager extends GlueList<Region> {
    public static List<Region> visibleRegions = new GlueList<>();
    public static Path worldDir;
    public static final int CHUNK_HEIGHT = 320;
    public static final int RENDER_DISTANCE = 3;
    public static final int REGION_BOUNDS = 512;
    public static final int CHUNK_BOUNDS = 16;
    public static final long WORLD_SEED = 1234567890;

    /**
     * The highest level object representation of a world. The RegionManager
     * contains an in-memory list of regions that are currently within
     * the players render distance. This region list is constantly updated each
     * frame and is used for reading regions from file and writing regions to file.
     *
     * @param path The path of this worlds directory
     */
    public RegionManager(Path path) {

        //Bind texture atlas once when world is loading
        TextureLoader.loadTexture("src/main/resources/textures/texture_atlas.png");

        try {
            RegionManager.worldDir = path;
            Files.createDirectories(Paths.get(worldDir + "\\regions\\"));
        } catch (Exception e) {
            e.printStackTrace();
        }


        ChunkCache.setBounds(CHUNK_BOUNDS);
        ChunkCache.setRenderDistance(RENDER_DISTANCE);
    }




    /**
     * Removes a region from the visible regions once a player leaves a region and
     * their render distance no longer overlaps it. Writes region to file in the process
     * effectively saving the regions state for future use.
     *
     * @param r The region to leave
     */
    public static void leaveRegion(Region r) {
        try {
            File[] regionFiles = new File(worldDir + "\\regions\\").listFiles((dir, name) ->
                    name.equals((int) r.getBounds().getBounds2D().getX() + "." + (int) r.getBounds().getBounds2D().getY() + ".dat"));

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
                        + (int) r.getBounds().getBounds2D().getX() + "." + (int) r.getBounds().getBounds2D().getY() + ".dat");
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
                    name.equals((int) r.getBounds().getBounds2D().getX() + "." + (int) r.getBounds().getBounds2D().getY() + ".dat"));

            //Get region from files if it exists
            Region match = visibleRegions.stream().filter(p -> p.getBounds().getBounds2D().getX() == r.getBounds().getBounds2D().getX()
                    && p.getBounds().getBounds2D().getY()== r.getBounds().getBounds2D().getY()).findFirst().orElse(
                    new Region((int) r.getBounds().getBounds2D().getX(), (int) r.getBounds().getBounds2D().getY()));

            assert regionFiles != null;
            if (!visibleRegions.contains(match) && regionFiles.length > 0) {

                FileInputStream f = new FileInputStream(worldDir + "\\regions\\"
                        + (int) r.getBounds().getBounds2D().getX() + "." + (int) r.getBounds().getBounds2D().getY() + ".dat");
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
                    Region w = q.get();
                    visibleRegions.add(w);
                    System.out.println("[Entering Region1] " + w);

                    return w;
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
            //TODO: Need to get region from file instead of generating a new one
            Region q = new Region((int) r.getBounds().getBounds2D().getX(), (int) r.getBounds().getBounds2D().getY());
            visibleRegions.add(q);
            return q;
        }
        return r;

    }

    /**
     * The ChunkCache will update the regions in memory, storing them as potentially blank objects
     * if the region was not already in memory. This method is responsible for reading region data
     * into these blank region objects when in memory and writing data to the file
     * system for future use when the player no longer inhabits them.
     *
     */
    public static void updateVisibleRegions() {

        //Updates regions within render distance
        ChunkCache.getChunksToRender();
        List<Region> updatedRegions = ChunkCache.getRegions();

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
                    leaveRegion(visibleRegions.get(i));
                }
            }
        }
    }
}
