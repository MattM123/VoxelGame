package com.marcuzzo;

public class RenderTask {
    private final int vbo;
    private final int ebo;
    private final float[] vertexData;
    private final int[] elementData;
    private final Chunk origin;


    /**
     * RenderTask objects store GL primitive data relative to each chunk.
     * Each chunk has its own RenderTask that it generates or regenerates
     * when that chunk is flagged for re-rendering. The RenderTask contains a
     * VBO, EBO, vertex array, and element array used when performing draw
     * calls with glDrawElements
     *
     * @param vertexData float array constructed using the chunks heightmap
     * @param elementData int array constructed alongside the vertexData
     * @param vbo Chunk specific VBO used for rendering
     * @param ebo Chunk specific EBO used for rendering
     * @param origin The chunk that this RenderTask belongs to
     */
    public RenderTask(float[] vertexData, int[] elementData, int vbo, int ebo, Chunk origin) {
        this.vertexData = vertexData;
        this.elementData = elementData;
        this.origin = origin;
        this.vbo = vbo;
        this.ebo = ebo;
    }

    public float[] getVertexData() {
        return vertexData;
    }
    public int[] getElementData() {
        return elementData;
    }
    public int getVbo() {
        return vbo;
    }
    public int getEbo() {
        return ebo;
    }
  //  public Chunk getChunk() {
  //     return origin;
   // }
}
