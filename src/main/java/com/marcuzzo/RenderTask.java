package com.marcuzzo;


import java.util.Map;

public class RenderTask {
    private final int vbo;
    private final int ebo;
    private final Map<float[], int[]> chunkData;
    private final Chunk origin;

    public RenderTask(Map<float[], int[]> chunkData, int vbo, int ebo, Chunk origin) {
        this.chunkData = chunkData;
        this.origin = origin;
        this.vbo = vbo;
        this.ebo = ebo;
    }

    public Map.Entry<float[], int[]> getChunkData() throws RuntimeException {

        return chunkData.entrySet().iterator().next();
    }
    public int getVbo() {
        return vbo;
    }
    public int getEbo() {
        return ebo;
    }
    public Chunk getChunk() {
        return origin;
    }
}
