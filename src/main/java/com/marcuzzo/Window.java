package com.marcuzzo;

import com.marcuzzo.Texturing.BlockType;
import com.marcuzzo.Texturing.TextureCoordinateStore;
import com.marcuzzo.Texturing.TextureLoader;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.app.Application;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import static com.marcuzzo.Main.shaderProgram;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Low-level abstraction, which creates application window and starts the main loop.
 * It's recommended to use {@link Application}, but this class could be extended directly as well.
 * When extended, life-cycle methods should be called manually.
 */

public class Window {
    private static final Logger logger = Logger.getLogger("Logger");
    public static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    public static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private static long windowPtr;
    private final ImGuiLayer layer;
    private float angle = 0.0f;
    private final float FOV = (float) Math.toRadians(60.0f);
    private static Matrix4f projectionMatrix;
    private static Matrix4f modelViewMatrix;
    //X pos of window
    private static Player player;
    private int windowWidth = 0;
    //Y pos of window
    private int windowHeight = 0;
    public static Chunk globalPlayerChunk;
    public static Region globalPlayerRegion;
    private static RegionManager loadedWorld;
    public final float PLAYER_STEP_SPEED = 1.0f;
    public final float MOUSE_SENSITIVITY = 0.04f;
    private final MouseInput mouseInput = new MouseInput();
    private int vaoID;
    private int vboID;
    private int eboID;
    private static boolean isMenuRendered = false;

    public Window(ImGuiLayer layer) {
        if (player == null)
            player = new Player();

        this.layer = layer;
    }
    public void init() {
        initWindow();
        glfwMakeContextCurrent(windowPtr);
        GL.createCapabilities();

        mouseInput.init();
        initImGui();
        imGuiGlfw.init(windowPtr, false);
        imGuiGl3.init();
    }
    public static void destroy() {
        glfwMakeContextCurrent(NULL);
        shaderProgram.cleanup();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        TextureLoader.unbind();
    }
    private void initWindow() {
        //Getting screen size
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle bounds = ge.getMaximumWindowBounds();

        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(ge.getDefaultScreenDevice().getDefaultConfiguration());
        Rectangle effectiveScreenArea = new Rectangle();
        effectiveScreenArea.x = bounds.x + screenInsets.left;
        effectiveScreenArea.y = bounds.y + screenInsets.top;
        effectiveScreenArea.height = bounds.height - screenInsets.bottom;
        effectiveScreenArea.width = bounds.width - screenInsets.left - screenInsets.right;

        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() ) {
            System.out.println("Unable to initialize GLFW");
            System.exit(-1);
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        windowPtr = glfwCreateWindow(effectiveScreenArea.width, effectiveScreenArea.height, "Main Window", NULL, NULL);

        // Set the window close callback
        GLFW.glfwSetWindowCloseCallback(windowPtr, new WindowCloseCallback());

        if (windowPtr == NULL) {
            System.out.println("Unable to create window");
            System.exit(-1);
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        GL.createCapabilities();

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        modelViewMatrix = player.getViewMatrix();
        modelViewMatrix.translate(new Vector3f(0.0f, 0.0f, -2.0f));
        glfwSetKeyCallback(windowPtr, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_W)
                player.moveForward(1);
            else if (key == GLFW_KEY_S)
                player.moveBackwards(1);

            if (key == GLFW_KEY_A)
                player.moveLeft(1);
            else if (key == GLFW_KEY_D)
                player.moveRight(1);

            if (key == GLFW_KEY_SPACE)
                player.moveUp(1);
            else if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT)
                player.moveDown(1);
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowPtr, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            assert vidmode != null;
            glfwSetWindowPos(
                    windowPtr,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically
    }
    private void initImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
    }

    /**
     * Main program loop
     */
    public void run() {

        //========================
        //Shader Compilation
        //========================

        //Load the vertex shader from file
        String vertexShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/Texturing/VertexShader.glsl");
        shaderProgram.createVertexShader(vertexShaderSource);

        // Load the fragment shader from file
        String fragmentShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/Texturing/FragShader.glsl");
        shaderProgram.createFragmentShader(fragmentShaderSource);

        //Load Textures
        shaderProgram.uploadTexture("texture_sampler", 0);

        // Link the shader program
        shaderProgram.link();

        // Use the shader program for rendering
        shaderProgram.bind();

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);

        // Set the clear color (background)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        //Enable primitive restart
        glEnable(GL_PRIMITIVE_RESTART);
        glPrimitiveRestartIndex(80000);

        while (!glfwWindowShouldClose(windowPtr)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer
            glFlush();

            imGuiGlfw.newFrame();
            ImGui.newFrame();
            layer.imgui();

            // Update player position
            mouseInput.input();

            Vector2f rotVec = MouseInput.getDisplVec();
            player.setLookDir((float) Math.toRadians(-rotVec.x * MOUSE_SENSITIVITY),
                    (float) Math.toRadians(-rotVec.y * MOUSE_SENSITIVITY));


            //Resets viewport on window resize. Prevents graphics getting cut off by
            //remapping them to a new viewport transformation using the new window size
            if (windowWidth != this.getCurrentWindowWidth() || windowHeight != this.getCurrentWindowHeight()) {
                glViewport(0, 0, this.getCurrentWindowWidth(), this.getCurrentWindowHeight());
                windowWidth = this.getCurrentWindowWidth();
                windowHeight = this.getCurrentWindowHeight();
            }

            //Passes parent window position and size to UI layer to use for positioning
            try (var stack = MemoryStack.stackPush()) {
                var posXBuffer = stack.mallocInt(1);
                var posYBuffer = stack.mallocInt(1);
                glfwGetWindowPos(windowPtr, posXBuffer, posYBuffer);
                layer.setParentX(posXBuffer.get());
                layer.setParentY(posYBuffer.get());

                var widthBuffer = stack.mallocInt(1);
                var heightBuffer = stack.mallocInt(1);
                glfwGetWindowSize(windowPtr, widthBuffer, heightBuffer);
                layer.setParentWidth(widthBuffer.get());
                layer.setParentHeight(heightBuffer.get());
            }

            // Set up the projection matrix
            try (MemoryStack stack = stackPush()) {
                FloatBuffer pMatrix = stack.mallocFloat(16);
                float FAR = 100.0f;
                float NEAR = 0.1f;

                projectionMatrix = new Matrix4f().setPerspective(FOV, (float) this.getCurrentWindowWidth() / this.getCurrentWindowHeight(), NEAR, FAR);
                projectionMatrix.get(pMatrix);
            }

            // Render the menu/world
            if (loadedWorld == null) {
                angle += 0.007f;
                isMenuRendered = true;
                renderMenu();
            }
            else {
                glfwSetInputMode(windowPtr, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                isMenuRendered = false;
                renderWorld();
            }

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backWindowPtr);
            }

            glfwSwapBuffers(windowPtr);
            glfwPollEvents();

        }
        destroy();

    }

    private void cleanupBuffers() {
        // Delete VAO, VBO, and EBO
        glDeleteVertexArrays(vaoID);
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
    }

    private void renderMenu() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

            TextureCoordinateStore grassFront = BlockType.GRASS.getFrontCoords();
            TextureCoordinateStore grassBack = BlockType.GRASS.getBackCoords();
            TextureCoordinateStore grassLeft = BlockType.GRASS.getLeftCoords();
            TextureCoordinateStore grassRight = BlockType.GRASS.getRightCoords();
            TextureCoordinateStore grassTop = BlockType.GRASS.getTopCoords();
            TextureCoordinateStore grassBott = BlockType.GRASS.getBottomCoords();


            float[] vertices = {
                    //Position (X, Y, Z)    Color (R, G, B, A)          Texture (U, V)

                    // Front face
                    -0.5f, -0.5f, 0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassFront.getBottomLeft()[0], grassFront.getBottomLeft()[1],
                    0.5f, -0.5f,  0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassFront.getBottomRight()[0], grassFront.getBottomRight()[1],
                    -0.5f, 0.5f,  0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassFront.getTopLeft()[0], grassFront.getTopLeft()[1],
                    0.5f,  0.5f,  0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassFront.getTopRight()[0], grassFront.getTopRight()[1],

                    // Back face
                    -0.5f, -0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBack.getBottomLeft()[0], grassBack.getBottomLeft()[1],
                    0.5f, -0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassBack.getBottomRight()[0], grassBack.getBottomRight()[1],
                    -0.5f,  0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBack.getTopLeft()[0], grassBack.getTopLeft()[1],
                    0.5f,  0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassBack.getTopRight()[0], grassBack.getTopRight()[1],

                    // Left face
                    -0.5f, -0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassLeft.getBottomLeft()[0], grassLeft.getBottomLeft()[1],
                    -0.5f,  0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassLeft.getTopLeft()[0], grassLeft.getTopLeft()[1],
                    -0.5f, -0.5f, 0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassLeft.getBottomRight()[0], grassLeft.getBottomRight()[1],
                    -0.5f,  0.5f, 0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassLeft.getTopRight()[0], grassLeft.getTopRight()[1],

                    // Right face
                    0.5f, -0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassRight.getBottomRight()[0], grassRight.getBottomRight()[1],
                    0.5f,  0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassRight.getTopRight()[0], grassRight.getTopRight()[1],
                    0.5f, -0.5f, 0.5f,      0.0f, 0.0f, 0.0f, 0.0f,     grassRight.getBottomLeft()[0], grassRight.getBottomLeft()[1],
                    0.5f,  0.5f, 0.5f,      0.0f, 0.0f, 0.0f, 0.0f,     grassRight.getTopLeft()[0], grassRight.getTopLeft()[1],

                    // Top face
                    -0.5f, 0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassTop.getBottomLeft()[0], grassTop.getBottomLeft()[1],
                    0.5f,  0.5f, -0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassTop.getBottomRight()[0], grassTop.getBottomRight()[1],
                    -0.5f, 0.5f,  0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassTop.getTopLeft()[0], grassTop.getTopLeft()[1],
                    0.5f,  0.5f,  0.5f,     0.0f, 0.0f, 0.0f, 0.0f,     grassTop.getTopRight()[0], grassTop.getTopRight()[1],

                    // Bottom face
                    -0.5f, -0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBott.getBottomLeft()[0], grassBott.getBottomLeft()[1],
                    0.5f,  -0.5f, -0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBott.getBottomRight()[0], grassBott.getBottomRight()[1],
                    -0.5f, -0.5f,  0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBott.getTopLeft()[0], grassBott.getTopLeft()[1],
                    0.5f,  -0.5f,  0.5f,    0.0f, 0.0f, 0.0f, 0.0f,     grassBott.getTopRight()[0], grassBott.getTopRight()[1],
            };

            // Declares the Elements Array, where the indices to be drawn are stored
            int[] elementArray = {
                    //Front face
                    0, 1, 2, 3, 80000,
                    //Back face
                    4, 5, 6, 7, 80000,
                    //Left face
                    8, 9, 10, 11, 80000,
                    //Right face
                    12, 13, 14, 15, 80000,
                    //Top face
                    16, 17, 18, 19, 80000,
                    //Bottom face
                    20, 21, 22, 23, 80000
            };


            /*==================================
            Buffer binding and loading
            ====================================*/
            vboID = glGenBuffers();
            eboID = glGenBuffers();
            vaoID = glGenVertexArrays();

            glBindVertexArray(vaoID);

            //Vertices
            FloatBuffer vertexBuffer = stack.mallocFloat(vertices.length);
            vertexBuffer.put(vertices).flip();

            // Create VBO upload the vertex buffer
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

            //Elements
            IntBuffer elementBuffer = stack.mallocInt(elementArray.length);
            elementBuffer.put(elementArray).flip();

            // Create EBO upload the element buffer
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);

            /*=====================================
            Vertex attribute definitions for shaders
            ======================================*/
            int posSize = 3;
            int colorSize = 4;
            int uvSize = 2;
            int floatSizeBytes = 4;
            int vertexSizeBytes = (posSize + colorSize + uvSize) * floatSizeBytes;

            //Position
            glVertexAttribPointer(0, posSize, GL_FLOAT, false, vertexSizeBytes, 0);
            glEnableVertexAttribArray(0);

            //Color
            glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexSizeBytes, posSize * Float.BYTES);
            glEnableVertexAttribArray(1);

            //Texture
            glVertexAttribPointer(2, uvSize, GL_FLOAT, false, vertexSizeBytes, (posSize + colorSize) * Float.BYTES);
            glEnableVertexAttribArray(2);

            /*==================================
            View Matrix setup
            ====================================*/

            // Set up the model-view matrix for rotation
            modelViewMatrix = new Matrix4f();
            modelViewMatrix.translate(new Vector3f(0.0f, 0f, -2.0f));
            modelViewMatrix.rotate(angle, new Vector3f(0.2f, 1.0f, 0.2f));


            // Set up the model-view-projection matrix
            Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);


            // Pass the model-view-projection matrix to the shader as a uniform
            int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "viewProjectionMatrix");
            glUniformMatrix4fv(mvpMatrixLocation, false, modelViewProjectionMatrix.get(new float[16]));

            /*==================================
            Drawing
            ====================================*/
            TextureLoader.loadTexture("src/main/resources/textures/texture_atlas.png");
            glDrawElements(GL_TRIANGLE_STRIP, elementArray.length, GL_UNSIGNED_INT, 0);

        } catch(Exception e) {
            logger.warning(e.getMessage());
        }

        //Unbind and cleanup everything
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        TextureLoader.unbind();
        glBindVertexArray(0);
        cleanupBuffers();
    }


    //Potential optimizations:
    // Bind buffers outside of loop

    private void renderWorld() {

        /*=====================================
         View Matrix setup
        ======================================*/
        Matrix4f viewProjectionMatrix = new Matrix4f(projectionMatrix).mul(player.getViewMatrix());
        // System.out.println(Arrays.toString(viewProjectionMatrix.get(new float[16])));

        // Pass the model-view-projection matrix to the shader as a uniform
        int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "viewProjectionMatrix");
        glUniformMatrix4fv(mvpMatrixLocation, false, viewProjectionMatrix.get(new float[16]));


        /*====================================
         Chunk and Region check
        =====================================*/
        //playerChunk will be null when world first loads
        if (globalPlayerChunk == null)
            globalPlayerChunk = Player.getChunkWithPlayer();

        //playerRegion will be null when world first loads
        if (globalPlayerRegion == null) {
            globalPlayerRegion = Player.getRegionWithPlayer();
            RegionManager.enterRegion(globalPlayerRegion);
        }

        //Updates the chunks to render only when the player has moved into a new chunk
        GlueList<Chunk> chunksToRender = new GlueList<>(ChunkCache.getChunksToRender());
        if (!Player.getChunkWithPlayer().getLocation().equals(globalPlayerChunk.getLocation())) {
            globalPlayerChunk = Player.getChunkWithPlayer();
            ChunkCache.setPlayerChunk(globalPlayerChunk);
            chunksToRender = new GlueList<>(ChunkCache.getChunksToRender());

            //Updates the regions when player moves into different region
            if (!Player.getRegionWithPlayer().equals(globalPlayerRegion)) {
                globalPlayerRegion = Player.getRegionWithPlayer();
                RegionManager.updateVisibleRegions();
            }
        }

        /*=====================================
         Vertex attribute definitions for shaders
         ======================================*/
        int posSize = 3;
        int colorSize = 4;
        int uvSize = 2;
        int floatSizeBytes = 4;
        int vertexSizeBytes = (posSize + colorSize + uvSize) * floatSizeBytes;

        //Position
        glVertexAttribPointer(0, posSize, GL_FLOAT, false, vertexSizeBytes, 0);
        glEnableVertexAttribArray(0);

        //Color
        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexSizeBytes, posSize * Float.BYTES);
        glEnableVertexAttribArray(1);

        //Texture
        glVertexAttribPointer(2, uvSize, GL_FLOAT, false, vertexSizeBytes, (posSize + colorSize) * Float.BYTES);
        glEnableVertexAttribArray(2);

        //Per chunk primitive information calculated in thread pool and later sent to GPU for drawing
        ArrayList<Future<RenderTask>> renderTasks = new ArrayList<>();
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //Gets ChunkCache mesh data in a non-blocking manner
        for (Chunk c : chunksToRender) {
            /*======================================================
             Getting vertex and element information for rendering
            ========================================================*/

            //Will only queue chunks RenderTask if chunk mesh has been modified
            if (c.shouldRerender()) {
                c.setEbo(glGenBuffers());
                c.setVbo(glGenBuffers());
                renderTasks.add(Main.executor.submit(c::getRenderTask));

                //Chunks RenderTask has been queued so chunks render flag can
                // be set to false now until the chunk is modified again
                c.setRerender(false);

            }

        }

        for (Future<RenderTask> chunkRenderTask : renderTasks) {

            float[] vertices = new float[0];
            int[] elements = new int[0];
            RenderTask task = null;
            //Gets chunk data from previously submitted Future
            try {
                task = chunkRenderTask.get();
                vertices = ArrayUtils.addAll(vertices, task.getVertexData());
                elements = ArrayUtils.addAll(elements, task.getElementData());
            } catch (InterruptedException | ExecutionException e) {
                logger.warning(e.getMessage());
            }

            //Sends chunk data to GPU for drawing
            if (vertices.length > 0 && elements.length > 0) {
                /*==================================
                Buffer binding and loading
                ====================================*/

                //Vertices
                FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
                vertexBuffer.put(vertices).flip();

                // Create VBO upload the vertex buffer
                glBindBuffer(GL_ARRAY_BUFFER, task.getVbo());
                glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

               // float[] test = new float[vertices.length];
               // glGetBufferSubData(GL_ARRAY_BUFFER, 0, test);
               // System.out.println("VBO:      " + Arrays.toString(test));
               // System.out.println("Vertices: " + Arrays.toString(vertices).substring(0, 1000));

                //Elements
                IntBuffer elementBuffer = MemoryUtil.memAllocInt(elements.length);
                elementBuffer.put(elements).flip();

                // Create EBO upload the element buffer
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, task.getEbo());
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);

              //  int[] test1 = new int[elements.length];
              //  glGetBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, test1);
              //  System.out.println("EBO:      " + Arrays.toString(test1));
              //  System.out.println("Elements: " + Arrays.toString(elements).substring(0, 1000));

                /*==================================
                Drawing
                ====================================*/
                glDrawElements(GL_TRIANGLE_STRIP, elements.length, GL_UNSIGNED_INT, 0);


                System.out.println("Drawing " + vertices.length + " vertices and " + elements.length + " elements");

                MemoryUtil.memFree(elementBuffer);
                MemoryUtil.memFree(vertexBuffer);
            } else {
                logger.warning("Chunk has no data or inconsistent data");
            }
        }

        //Unbind and cleanup everything
        cleanupBuffers();
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindVertexArray(0);
    }


    /**
     * Loads vertex and fragment shaders
     */

    private String loadShaderFromFile(String filePath) {
        Path path = Paths.get(filePath);

        try {
            return Files.readString(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getCurrentWindowWidth() {
        //Passes parent window position to UI layer to use for positioning
        try (var stack = MemoryStack.stackPush()) {
            var heightBuffer = stack.mallocInt(1);
            var widthBuffer = stack.mallocInt(1);
            glfwGetWindowSize(windowPtr, widthBuffer, heightBuffer);

            return widthBuffer.get();
        }
    }
    private int getCurrentWindowHeight() {
        //Passes parent window position to UI layer to use for positioning
        try (var stack = MemoryStack.stackPush()) {
            var heightBuffer = stack.mallocInt(1);
            var widthBuffer = stack.mallocInt(1);
            glfwGetWindowSize(windowPtr, widthBuffer, heightBuffer);

            return heightBuffer.get();

        }
    }
    public static void setLoadedWorld(RegionManager r) {
        loadedWorld = r;
    }
    public static long getWindowPtr() {
        return windowPtr;
    }

    public static boolean isMenuRendered() {
        return isMenuRendered;
    }

}