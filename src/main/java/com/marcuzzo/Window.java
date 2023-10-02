package com.marcuzzo;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.app.Application;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private static long windowPtr;
    private final ImGuiLayer layer;
    private float angle = 0.0f;
    private final float FOV = (float) Math.toRadians(60.0f);
    private static Matrix4f projectionMatrix;
    private static Matrix4f modelViewMatrix;
    private static ShaderProgram shaderProgram;
    //X pos of window
    private int windowWidth = 0;
    //Y pos of window
    private int windowHeight = 0;
    private static Player player;
    public static Chunk globalPlayerChunk;
    public static Region globalPlayerRegion;
    private static RegionManager loadedWorld;
    public final float PLAYER_STEP_SPEED = 1.0f;
    public final float MOUSE_SENSITIVITY = 0.04f;
    private final MouseInput mouseInput = new MouseInput();
    private Vector3f playerInc = new Vector3f(0f, 0f, 0f);
    public Window(ImGuiLayer layer, ShaderProgram shaderProgram) {
        if (player == null)
            player = new Player();
        this.layer = layer;
        Window.shaderProgram = shaderProgram;
    }
    public void init() {
        initWindow();
        glfwMakeContextCurrent(windowPtr);
        GL.createCapabilities();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init();
        TextureLoader.initTextureAtlas();
    }
    public void destroy() {
        shaderProgram.cleanup();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        ImGui.destroyPlatformWindows();
        Callbacks.glfwFreeCallbacks(windowPtr);
        glfwDestroyWindow(windowPtr);
        glfwTerminate();
        System.out.println("Program terminated");
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


        String glslVersion = "#version 130";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        windowPtr = glfwCreateWindow(effectiveScreenArea.width, effectiveScreenArea.height, "Main Window", NULL, NULL);

        if (windowPtr == NULL) {
            System.out.println("Unable to create window");
            System.exit(-1);
        }

        glfwMakeContextCurrent(windowPtr);
        glfwSwapInterval(1);
        glfwShowWindow(windowPtr);
        GL.createCapabilities();

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
       // initPlayer();
        modelViewMatrix = Window.getPlayer().getModelViewMatrix();
        modelViewMatrix.translate(new Vector3f(0.0f, 0.0f, -2.0f));
        glfwSetKeyCallback(windowPtr, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_W) {
                playerInc.z = 1.0f;
            } else if (key == GLFW_KEY_S) {
                playerInc.z = -1.0f;
            }
            if (key == GLFW_KEY_A) {
                playerInc.x = 1.0f;
            } else if (key == GLFW_KEY_D) {
                playerInc.x = -1.0f;
            }
        });

        //Setup cursor callbacks
        mouseInput.init();

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
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
        String vertexShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/VertexShader.glsl");
        shaderProgram.createVertexShader(vertexShaderSource);

        // Load the fragment shader from file
        String fragmentShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/FragShader.glsl");
        shaderProgram.createFragmentShader(fragmentShaderSource);

        //Load Textures
        shaderProgram.uploadTexture("texture_Sampler", 0);

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

        while (!glfwWindowShouldClose(windowPtr)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer
            glFlush();

            imGuiGlfw.newFrame();
            ImGui.newFrame();
            layer.imgui();

            // Update player position
            mouseInput.input();
            player.movePosition(playerInc.x * PLAYER_STEP_SPEED,
                    playerInc.y * PLAYER_STEP_SPEED,
                    playerInc.z * PLAYER_STEP_SPEED);
            playerInc = new Vector3f(0f, 0f, 0f);

            //Update player camera rotation
            Vector2f rotVec = mouseInput.getDisplVec();
            getPlayer().moveRotation(rotVec.x * MOUSE_SENSITIVITY, rotVec.y * MOUSE_SENSITIVITY, 0);


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

                projectionMatrix = new Matrix4f().perspective(FOV, (float) this.getCurrentWindowWidth() / this.getCurrentWindowHeight(), NEAR, FAR);
                projectionMatrix.get(pMatrix);
            }

            // Render the menu/world
            if (loadedWorld == null) {
                angle += 0.01f;
                renderMenu();
            }
            else {
                glfwSetInputMode(windowPtr, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
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

    /**
     * |\--------|\
     * | \       | \
     * |__\======|__\
     * \  |      \  |
     *  \ |       \ |
     *   \|________\|
     */
    private void renderMenu() {
        // Define the vertices and colors of the cube

        glEnable(GL_PRIMITIVE_RESTART);
        glPrimitiveRestartIndex(80000);

        //TODO: Figure out how tf to render a cube with GL_TRIANGLE_STRIP preferably
        float[] vertices = {
                //Position (X, Y, Z)    Color (R, G, B, A)          Texture (U, V)

                // Front face
                -0.5f, -0.5f, 0.5f,     1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,     0.0f, 1.0f, 0.0f, 1.0f,     1.0f, 0.0f,
                -0.5f, 0.5f,  0.5f,     0.0f, 0.0f, 1.0f, 1.0f,     0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,     1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f,


                // Back face
                -0.5f, -0.5f, -0.5f,    1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,     0.0f, 1.0f, 0.0f,  1.0f,    1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,    0.0f, 0.0f, 1.0f,  1.0f,    0.0f, 1.0f,
                0.5f,  0.5f, -0.5f,     1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f,

                // Left face
                -0.5f, -0.5f, -0.5f,    1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,    0.0f, 1.0f, 0.0f, 1.0f,     1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,    0.0f, 0.0f, 1.0f, 1.0f,     0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,    1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f,

                // Right face
                0.5f, -0.5f, -0.5f,     1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                0.5f,  0.5f, -0.5f,     0.0f, 1.0f, 0.0f, 1.0f,     1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,     0.0f, 0.0f, 1.0f, 1.0f,     0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,     1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f,

                // Top face
                -0.5f, 0.5f, -0.5f,     1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                0.5f,  0.5f, -0.5f,     0.0f, 1.0f, 0.0f, 1.0f,     1.0f, 0.0f,
                -0.5f, 0.5f,  0.5f,     0.0f, 0.0f, 1.0f, 1.0f,     0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,     1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f,

                // Bottom face
                -0.5f, -0.5f, -0.5f,    1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f,
                0.5f,  -0.5f, -0.5f,    0.0f, 1.0f, 0.0f, 1.0f,     1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,    0.0f, 0.0f, 1.0f, 1.0f,     0.0f, 1.0f,
                0.5f,  -0.5f,  0.5f,    1.0f, 1.0f, 1.0f, 1.0f,     1.0f, 1.0f
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

        //Loading texture image
       // String path = "src/main/resources/textures/grass_side.png";
       // IntBuffer width = BufferUtils.createIntBuffer(1);
       // IntBuffer height = BufferUtils.createIntBuffer(1);
      //  IntBuffer channels = BufferUtils.createIntBuffer(1);
      //  ByteBuffer image = stbi_load(path, width, height, channels, 0);

        //Binding texture ID
        /*
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glGenerateMipmap(GL_TEXTURE_2D);


        if (image != null) {
            //Loads image to GPU
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                    0, GL_RGBA, GL_UNSIGNED_BYTE, image);

            //Frees memory containing image
            stbi_image_free(image);
        } else {
            logger.warning("Texture could not be loaded from " + path);
        }

         */



      TextureLoader.loadTexture(Texture.getByteBufferTexture(Texture.GRASS_SIDE));

       // glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
        //        0, GL_RGBA, GL_UNSIGNED_BYTE, image);


        /*==================================
        Buffer binding and loading
        ====================================*/

        // Create VAO
        int vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Create a float buffer of vertices
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        // Create VBO upload the vertex buffer
        int vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Create the indices and upload
        IntBuffer elementBuffer = BufferUtils.createIntBuffer(elementArray.length);
        elementBuffer.put(elementArray).flip();

        // Create EBO upload the element buffer
        int eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL_STATIC_DRAW);

        // Bind the VAO that we're using
        glBindVertexArray(vaoID);

        /*=====================================
        Vertex attribute definitions for shader
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
        modelViewMatrix.translate(new Vector3f(0.0f, 0.0f, -2.0f));
        modelViewMatrix.rotate(angle, new Vector3f(0.2f, 1.0f, 0.2f));


        // Set up the model-view-projection matrix
        Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);

        // Pass the model-view-projection matrix to the shader as a uniform
        int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "modelViewProjectionMatrix");
        glUniformMatrix4fv(mvpMatrixLocation, false, modelViewProjectionMatrix.get(new float[16]));


        // Enable the vertex attribute pointers
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);


        /*==================================
        Drawing
        ====================================*/
        glDrawElements(GL_TRIANGLE_STRIP, elementArray.length, GL_UNSIGNED_INT, 0);

        //Unbind everything
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
        TextureLoader.unbind();
    }

    private void renderWorld() {

        //TODO: Fix Camera rotation
        //TODO: Regions either not displaying chunk number correctly or adding unnecessary chunks to them
        //Buffer binding
        int uboId = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, uboId);

        int vboId = glGenVertexArrays();


        if (vboId > 0)
            glBindBuffer(GL_ARRAY_BUFFER, vboId);

        //playerChunk will be null when world first loads
        if (globalPlayerChunk == null)
            globalPlayerChunk = Player.getRegion().getChunkWithPlayer();

        //playerRegion will be null when world first loads
        if (globalPlayerRegion == null) {
            globalPlayerRegion = Player.getRegion();
            RegionManager.enterRegion(globalPlayerRegion);
            // RegionManager.updateVisibleRegions();
        }

        //Updates the regions when player moves into different region
        if (!Player.getRegion().getLocation().equals(globalPlayerRegion.getLocation())) {
            globalPlayerRegion = Player.getRegion();
        }

        //Updates the chunks to render only when the player has moved into a new chunk
        GlueList<Chunk> chunksToRender = new GlueList<>(ChunkRenderer.getChunksToRender());
        if (!Player.getRegion().getChunkWithPlayer().getLocation().equals(globalPlayerChunk.getLocation())) {
            globalPlayerChunk = Player.getRegion().getChunkWithPlayer();
            ChunkRenderer.setPlayerChunk(globalPlayerChunk);
            chunksToRender = new GlueList<>(ChunkRenderer.getChunksToRender());
        }


        int posSize = 3;
        int colorSize = 4;
        int uvSize = 2;
        int floatSizeBytes = 4;
        int vertexSizeBytes = (posSize + colorSize) * floatSizeBytes;

        //Position
        glVertexAttribPointer(0, posSize, GL_FLOAT, false, vertexSizeBytes, 0);
        glEnableVertexAttribArray(0);

        //Color
        glVertexAttribPointer(1, colorSize, GL_FLOAT, false, vertexSizeBytes, posSize * Float.BYTES);
        glEnableVertexAttribArray(1);

        //Texture
        glVertexAttribPointer(2, uvSize, GL_FLOAT, false, vertexSizeBytes, (posSize + colorSize) * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Set up the model-view-projection matrix
        Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(player.getModelViewMatrix());


        // Pass the model-view-projection matrix to the shader as a uniform
        int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "modelViewProjectionMatrix");
        glUniformMatrix4fv(mvpMatrixLocation, false, modelViewProjectionMatrix.get(new float[16]));


        //TODO: create faces by iterating through heightmap??
        //TODO: Region not generating when moving into new region?




        /*==================================
        Drawing
        ====================================*/
        glDrawElements(GL_TRIANGLE_STRIP, 3, GL_UNSIGNED_INT, 0);


        // Unbind everything
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
/*
        for (Chunk chunk : chunksToRender) {
            if (chunk.orderedPoints == null)
                continue;
            else
                chunk.updateMesh();


            //Populating mesh arrays
            int[] firsts = new int[chunk.orderedPoints.length / 6];
            int[] counts = new int[chunk.orderedPoints.length / 6];
            Arrays.fill(counts, 2);
            for (int i = 0; i < chunk.orderedPoints.length / 6; i++) {
                firsts[i] = i * 6;
            }

            //Drawing primitives
            glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(chunk.orderedPoints.length).put(chunk.orderedPoints).flip(), GL_DYNAMIC_DRAW);
           // glDrawElements(GL_TRIANGLES, chunk.getZFaces());


            glMultiDrawArrays(GL_TRIANGLES, firsts, counts);

          //glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(chunk.getVertexArray().length).put(chunk.getVertexArray()).flip(), GL_STATIC_DRAW);
          //  glDrawElements(GL_TRIANGLE_STRIP, chunk.getFaces());
        }

 */




    }

    public static Player getPlayer() {
        return Window.player;
    }
    public static void setPlayer(Player p) {
        player = p;
    }
    /**
     * Loads vertex and fragment shaders
     * @param filePath filepath of glsl file
     * @return String containing glsl sourcecode
     */
    private static String loadShaderFromFile(String filePath) {
        try {
            byte[] encodedBytes = Files.readAllBytes(Paths.get(filePath));
            return new String(encodedBytes, StandardCharsets.UTF_8);
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
    /*
    public static RegionManager getLoadedWorld() {
        return loadedWorld;
    }

     */
    public static long getWindowPtr() {
        return windowPtr;
    }

    /*
    public static void setModelViewMatrix(Matrix4f modelViewMatrix) {
        Window.modelViewMatrix = modelViewMatrix;
    }

    public static Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    public static ShaderProgram getShaders() {
        return shaderProgram;
    }

     */
}