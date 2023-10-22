package com.marcuzzo;

import javafx.geometry.Point2D;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.awt.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class Region extends ChunkManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    //TODO: use Area and Shape class instead
    private Shape regionBounds;
    private static final Logger logger = Logger.getLogger("Logger");
    private boolean didChange = false;

    /**
     * An object representing a 32x32 chunk area, 1024 chunks in total managed by a chunk manager.
     * @param x coordinate of the corner of this region
     * @param z coordinate of the corner of this region
     */
    public Region(int x, int z) {
        super();
        regionBounds = new Rectangle(x, z, RegionManager.REGION_BOUNDS, RegionManager.REGION_BOUNDS);

    }
    /**
     * Returns true if at least one chunk in a region has changed. If true,
     * the region as a whol is also marked as having changed therefore
     * should be re-written to file.
     *
     * @return True if at least one chunk has changed, false if not
     */
    public boolean didChange() {
        for (Chunk c : this) {
            if (c.didChange()) {
                didChange = true;
                break;
            }
        }
        return didChange;
    }

    public Shape getBounds() {
        return regionBounds;
    }
    /**
     * Writes to file only if the didChange flag of the region is true. This
     * flag would only be true if at least one chunks didChange flag was also true.
     */
    @Serial
    private void writeObject(ObjectOutputStream o) {
        if (didChange()) {
            writeRegion(o, this);
            didChange = false;
        }
    }

    @Serial
    private void readObject(ObjectInputStream o) {
        this.replaceAll(c -> readRegion(o).get(indexOf(c)));
    }

    private void writeRegion(OutputStream stream, Region r) {
        System.out.println("Writing " + r);

        Main.executor.execute(() -> {
            try {
                FSTObjectOutput out = Main.getInstance().getObjectOutput(stream);
                out.writeObject(r, Region.class);
                r.clear();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
    }

    private Region readRegion(InputStream stream) {

        AtomicReference<Region> r = new AtomicReference<>();
        Main.executor.execute(() -> {
            FSTObjectInput in = Main.getInstance().getObjectInput(stream);

            try {
                r.set((Region) in.readObject(Region.class));
                System.out.println("Reading Region: " + r);
                stream.close();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }

            try { in.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
        return r.get();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Region) {
            return this.regionBounds.getBounds2D().getX() == ((Region) o).regionBounds.getBounds2D().getX()
                    && this.regionBounds.getBounds2D().getY() == ((Region) o).regionBounds.getBounds2D().getY();
        }
        return false;
    }
    @Override
    public String toString() {

        if (super.size() > 0 && super.getChunks() != null)
            return "(" + this.size() + " Chunks) Region: (" + regionBounds.getBounds2D().getX()
                    + ", " + regionBounds.getBounds2D().getY() + ")";
        else
            return "(Empty) Region: (" + regionBounds.getBounds2D().getX() + ", " + regionBounds.getBounds2D().getY() + ")";



    }

}
