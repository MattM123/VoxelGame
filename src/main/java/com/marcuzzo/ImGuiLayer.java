package com.marcuzzo;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Logger;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;


public class ImGuiLayer {
    private int parentX = 0;
    private int parentY = 0;
    private int parentWidth = 0;
    private int parentHeight = 0;
    public static String worlds = System.getenv("APPDATA") + "/.voxelGame/worlds/";
    private static final Logger logger = Logger.getLogger("Logger");

    public void imgui() {
        if (Window.isMenuRendered()) {
            ImGui.begin("World List", new ImBoolean(true));

            ImGui.text("Choose a World");
            ImGui.sameLine(ImGui.getWindowSizeX() - 100, -1);

            if (ImGui.button("Reposition")) {
                ImGui.setWindowPos(parentX, parentY);
                ImGui.setWindowSize(parentWidth / 4.0f, parentHeight / 2.0f);
            }

            ImGui.beginChild("World List Pane", 0, -30, true, ImGuiWindowFlags.AlwaysVerticalScrollbar);

            //Create UI entries for each world within world list
            try {
                DirectoryStream<Path> worldStream = Files.newDirectoryStream(Paths.get(worlds));
                Iterator<Path> worldIterator = worldStream.iterator();
                int dirLen = Objects.requireNonNull(new File(worlds).listFiles()).length;
                if (dirLen > 0) {
                    worldIterator.forEachRemaining(c -> {
                        //World label
                        ImGui.text(c.getFileName().toString());
                        ImGui.sameLine();

                        //Load button
                        ImGui.button("Load World");
                        if (ImGui.isItemClicked()) {
                            RegionManager rm = new RegionManager(Paths.get(worlds + c.getFileName()));
                         //   RegionManager.updateVisibleRegions();
                            Window.setLoadedWorld(rm);
                        }
                        ImGui.sameLine();

                        //Delete button
                        ImGui.button("Delete World");
                        if (ImGui.isItemClicked()) {
                            try {
                                FileUtils.deleteDirectory(Paths.get(worlds + c.getFileName().toString()).toFile());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ImGui.endChild();

            ImGui.pushItemWidth(ImGui.getWindowSizeY() / 3);
            glfwPollEvents();
            ImString txt = new ImString("");
            ImGui.inputText(" ", txt, ImGuiInputTextFlags.CallbackEdit | ImGuiInputTextFlags.CallbackResize);
            ImGui.popItemWidth();

            ImGui.sameLine();

            if (ImGui.button("Create New World")) {
                try {
                    Files.createDirectory(Path.of(worlds + txt));
                } catch (Exception e) {
                    logger.warning(e.getMessage());
                }
            }

            ImGui.end();
        } else {
            /*=====================================
            Debug Display
            =====================================*/
            ImGui.begin("Debug", new ImBoolean(false), ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize);

            ImGui.setWindowPos(parentX, parentY);
            ImGui.setWindowSize(parentWidth / 4.0f, parentHeight / 2.0f);
            ImGui.pushStyleColor(ImGuiCol.WindowBg,1.0f, 1.0f, 0.0f, 1.0f);
            ImGui.text("Player Position: X:" + Player.getPosition().x() + " Y:" + Player.getPosition().y() + " Z:" + Player.getPosition().z());
            ImGui.text("Player Rotation: X:" + Player.getRotation().x() + ", Y:" + Player.getRotation().y());
            ImGui.text("");
            ImGui.text("Region: " + Player.getRegionWithPlayer());
            ImGui.text(Player.getChunkWithPlayer().toString());
            ImGui.text("RenderedChunks Size: " + ChunkCache.getChunksToRender().size);
            ImGui.text("");
            ImGui.text("Cursor Position: " + MouseInput.getCursorPos().x + ", " + MouseInput.getCursorPos().y);
            ImGui.popStyleColor();
            ImGui.text("Memory: " + formatSize(Runtime.getRuntime().totalMemory()) +  "/" + formatSize(Runtime.getRuntime().maxMemory()));
            ImGui.end();

        }
    }

    public static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    public void setParentX(int x) {
        this.parentX = x;
    }
    public void setParentY(int y) {
        this.parentY = y;
    }

    public void setParentWidth(int x) {
        this.parentWidth = x;
    }

    public void setParentHeight(int y) {
        this.parentHeight = y;
    }
}
