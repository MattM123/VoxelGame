package com.marcuzzo;

import org.burningwave.core.assembler.StaticComponentContainer;
import org.lwjgl.Version;
import org.nustaq.serialization.FSTConfiguration;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final ShaderProgram shaderProgram = new ShaderProgram();
    public static Window window;
    public static String root = System.getenv("APPDATA") + "/.voxelGame/";
    public static ExecutorService executor = Executors.newFixedThreadPool(6, Thread::new);

    public static FSTConfiguration getInstance() {
        StaticComponentContainer.Modules.exportAllToAll();
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerClass(Block.class, Chunk.class, Region.class);
        return conf;
    }

    public static void main(String[] args) {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        //Creating game directory if it does not exist
        try {
            Files.createDirectories(Paths.get(root + "worlds//"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        window = new Window(new ImGuiLayer());
        window.init();
        shaderProgram.generateProgramID();
        window.run();
        Window.destroy();

    }
}