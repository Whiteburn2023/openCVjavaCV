package ru.otus.java.basic.oop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class WindowScreenshot {
    public static void main(String[] args) {
        try {
            Robot robot = new Robot();

            // Получаем список всех окон
            Frame[] frames = Frame.getFrames();

            // Или скриншот активного окна
            Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

            if (activeWindow != null) {
                Rectangle windowRect = activeWindow.getBounds();
                BufferedImage windowImage = robot.createScreenCapture(windowRect);
                ImageIO.write(windowImage, "png", new File("window_screenshot.png"));
                System.out.println("Скриншот окна сохранен");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
