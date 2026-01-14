package ru.otus.java.basic.oop;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScreenshotExample {
    public static void main(String[] args) {
        try {
            // Создаем объект Robot
            Robot robot = new Robot();

            // Получаем размер экрана
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            // Делаем скриншот
            BufferedImage screenImage = robot.createScreenCapture(screenRect);

            // Сохраняем в файл
            ImageIO.write(screenImage, "png", new File("screenshot.png"));

            System.out.println("Скриншот сохранен как screenshot.png");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
