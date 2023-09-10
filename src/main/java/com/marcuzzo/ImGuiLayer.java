package com.marcuzzo;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;

public class ImGuiLayer {
    private int parentX = 0;
    private int parentY = 0;
    private int parentWidth = 0;
    private int parentHeight = 0;
    public static String worlds = System.getenv("APPDATA") + "/.voxelGame/worlds/";

    public void imgui() {
        ImGui.begin("Window");
        ImGui.text("World List");
        ImGui.sameLine(ImGui.getWindowSizeX() - 85, -1);

        if (ImGui.button("Reposition")) {
            ImGui.setWindowPos(parentX, parentY);
            ImGui.setWindowSize(parentWidth / 4.0f, parentHeight / 2.0f);
        }


        ImGui.beginChild("World List Pane", 0,0, true, ImGuiWindowFlags.AlwaysVerticalScrollbar);

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
                        System.out.println(RegionManager.visibleRegions);

                        RegionManager.updateVisibleRegions();

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
        ImGui.end();
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
