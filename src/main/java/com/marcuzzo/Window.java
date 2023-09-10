package com.marcuzzo;

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
import static org.lwjgl.opengl.GL31.GL_PRIMITIVE_RESTART;
import static org.lwjgl.opengl.GL31.glPrimitiveRestartIndex;
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
    private Chunk playerChunk;
    private Region playerRegion;
    private static RegionManager loadedWorld;
    public final float PLAYER_STEP_SPEED = 1.0f;
    public final float MOUSE_SENSITIVITY = 0.04f;
    private final MouseInput mouseInput = new MouseInput();
    private Vector3f playerInc = new Vector3f(0f, 0f, 0f);
    public Window(ImGuiLayer layer, ShaderProgram shaderProgram) {
        RegionManager.setPlayer(new Player());
        player = RegionManager.getPlayer();
        this.layer = layer;
        Window.shaderProgram = shaderProgram;
    }
    public void init() {
        initWindow();
        initImGui();
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init();
    }
    public void destroy() {
        shaderProgram.cleanup();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
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
        modelViewMatrix = RegionManager.getPlayer().getModelViewMatrix();
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

        // Load the vertex shader from file
        String vertexShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/VertexShader.glsl");
        shaderProgram.createVertexShader(vertexShaderSource);

        // Load the fragment shader from file
        String fragmentShaderSource = loadShaderFromFile("src/main/java/com/marcuzzo/FragShader.glsl");
        shaderProgram.createFragmentShader(fragmentShaderSource);

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
        glEnable(GL_PRIMITIVE_RESTART);
        glPrimitiveRestartIndex(65535);

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
                float FAR = 1000.0f;
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

    private void renderMenu() {
        // Define the vertices and colors of the cube

        float[] vertices = {
                // Front face
                -0.5f, -0.5f,  0.5f,  // Bottom-left
                0.5f, -0.5f,  0.5f,  // Bottom-right
                -0.5f,  0.5f,  0.5f,  // Top-left
                0.5f,  0.5f,  0.5f,  // Top-right

                // Right face
                0.5f, -0.5f,  0.5f,  // Front-bottom
                0.5f, -0.5f, -0.5f,  // Back-bottom
                0.5f,  0.5f,  0.5f,  // Front-top
                0.5f,  0.5f, -0.5f,  // Back-top

                // Back face
                0.5f, -0.5f, -0.5f,  // Bottom-right
                -0.5f, -0.5f, -0.5f,  // Bottom-left
                0.5f,  0.5f, -0.5f,  // Top-right
                -0.5f,  0.5f, -0.5f,  // Top-left

                // Left face
                -0.5f, -0.5f, -0.5f,  // Back-bottom
                -0.5f, -0.5f,  0.5f,  // Front-bottom
                -0.5f,  0.5f, -0.5f,  // Back-top
                -0.5f,  0.5f,  0.5f,  // Front-top

                // Top face
                -0.5f,  0.5f,  0.5f,  // Front-left
                0.5f,  0.5f,  0.5f,  // Front-right
                -0.5f,  0.5f, -0.5f,  // Back-left
                0.5f,  0.5f, -0.5f,  // Back-right

                // Bottom face
                -0.5f, -0.5f,  0.5f,  // Front-left
                0.5f, -0.5f,  0.5f,  // Front-right
                -0.5f, -0.5f, -0.5f,  // Back-left
                0.5f, -0.5f, -0.5f   // Back-right
        };


        //Bind vertex array
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Set up the vertex buffer object (VBO)
        int vboId = glGenBuffers();
        if (vboId > 0)
            glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer f = BufferUtils.createFloatBuffer(vertices.length).put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, f, GL_DYNAMIC_DRAW);

        // Set up the vertex attribute pointers
        int positionAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "position");
        glEnableVertexAttribArray(positionAttribute);
        glVertexAttribPointer(positionAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);

        int colorAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "color");
        glEnableVertexAttribArray(colorAttribute);
        glVertexAttribPointer(colorAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);

        int normalAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "normal");
        glEnableVertexAttribArray(normalAttribute);
        glVertexAttribPointer(normalAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);

        // Set up the model-view matrix for rotation
        modelViewMatrix = new Matrix4f();
        modelViewMatrix.translate(new Vector3f(0.0f, 0.0f, -2.0f));
        modelViewMatrix.rotate(angle, new Vector3f(0.0f, 1.0f, 0.0f));


        // Set up the model-view-projection matrix
        Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);


        // Pass the model-view-projection matrix to the shader as a uniform
        int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "modelViewProjectionMatrix");
        glUniformMatrix4fv(mvpMatrixLocation, false, modelViewProjectionMatrix.get(new float[16]));

        // Draw the cube
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 24);

        // Unbind the VAO
        glBindVertexArray(0);
    }

    private void renderWorld() {

        //TODO: Fix Camera rotation
        //Bind vertex array
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Set up the vertex buffer object (VBO)
        int vboId = glGenBuffers();
        if (vboId > 0)
            glBindBuffer(GL_ARRAY_BUFFER, vboId);

      //  final float[][] chunkVerts = {new float[0]};
        float[] chunkVerts = new float[0];
        GlueList<Chunk> chunksToRender = new GlueList<>(ChunkRenderer.getChunksToRender());

        //playerChunk will be null when world first loads
        if (playerChunk == null)
            playerChunk = RegionManager.getRegionWithPlayer().getChunkWithPlayer();

        //playerRegion will be null when world first loads
        if (playerRegion == null) {
            playerRegion = RegionManager.getRegionWithPlayer();
        }

        //Updates the regions when player moves into different region
        if (!RegionManager.getRegionWithPlayer().getLocation().equals(playerRegion.getLocation())) {
            RegionManager.updateVisibleRegions();
            playerRegion = RegionManager.getRegionWithPlayer();
        }

        //Updates the chunks to render only when the player has moved into a new chunk
        if (!RegionManager.getRegionWithPlayer().getChunkWithPlayer().getLocation().equals(playerChunk.getLocation())) {

            playerChunk = RegionManager.getRegionWithPlayer().getChunkWithPlayer();
            ChunkRenderer.setPlayerChunk(playerChunk);
            chunksToRender = new GlueList<>(ChunkRenderer.getChunksToRender());
        }

        for (Chunk c : chunksToRender)
                chunkVerts = ArrayUtils.addAll(c.getZPlanarPoints());


        // Set up the vertex attribute pointers
        int positionAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "position");
        glEnableVertexAttribArray(positionAttribute);
        glVertexAttribPointer(positionAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);

        int colorAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "color");
        glEnableVertexAttribArray(colorAttribute);
        glVertexAttribPointer(colorAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);

        int normalAttribute = glGetAttribLocation(shaderProgram.getProgramId(), "normal");
        glEnableVertexAttribArray(normalAttribute);
        glVertexAttribPointer(normalAttribute, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);

        // Set up the model-view-projection matrix
        Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(player.getModelViewMatrix());


        // Pass the model-view-projection matrix to the shader as a uniform
        int mvpMatrixLocation = glGetUniformLocation(shaderProgram.getProgramId(), "modelViewProjectionMatrix");
        glUniformMatrix4fv(mvpMatrixLocation, false, modelViewProjectionMatrix.get(new float[16]));



        for (Chunk chunk : chunksToRender) {
            glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(chunk.getZPlanarPoints().length).put(chunk.getZPlanarPoints()).flip(), GL_STATIC_DRAW);
            glDrawElements(GL_TRIANGLE_STRIP, RegionManager.getRegionWithPlayer().getChunkWithPlayer().getZFaces());
        }


        // Draw chunk triangles
      //  glDrawArrays(GL_TRIANGLE_STRIP, 0, chunkVerts.length / 3);


     //   glDrawElements(GL_TRIANGLE_STRIP, RegionManager.getRegionWithPlayer().getChunkWithPlayer().getZFaces());



        // Unbind the VAO
        glBindVertexArray(0);


    }

    public static Player getPlayer() {
        return Window.player;
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