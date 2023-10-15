package com.marcuzzo;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

public class WindowCloseCallback implements GLFWWindowCloseCallbackI {
    @Override
    public void invoke(long window) {
        Callbacks.glfwFreeCallbacks(Window.getWindowPtr());
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
        glfwDestroyWindow(Window.getWindowPtr());

        Window.destroy();

        GLFW.glfwSetWindowShouldClose(window, true);
        glfwTerminate();

        System.out.println("Program terminated");
        System.exit(0);
    }
}
