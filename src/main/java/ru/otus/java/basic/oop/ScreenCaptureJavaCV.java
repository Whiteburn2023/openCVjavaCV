package ru.otus.java.basic.oop;

import org.bytedeco.javacv.*;

public class ScreenCaptureJavaCV {
    public static void main(String[] args) {
        // Создаем захватчик экрана
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber("desktop");

        try {
            // Настройки для Windows
            grabber.setFormat("gdigrab");  // Для Windows
            grabber.setOption("offset_x", "0");
            grabber.setOption("offset_y", "0");
            grabber.setImageWidth(1920);   // Ширина экрана
            grabber.setImageHeight(1080);  // Высота экрана
            grabber.setFrameRate(1);       // Частота кадров

            // Для Linux/Mac используйте:
            // grabber.setFormat("x11grab");  // Linux
            // grabber.setFormat("avfoundation");  // Mac

            grabber.start();

            // Захватываем кадр
            Frame frame = grabber.grab();

            // Сохраняем скриншот
            if (frame != null) {
                Java2DFrameConverter converter = new Java2DFrameConverter();
                java.awt.image.BufferedImage image = converter.convert(frame);

                javax.imageio.ImageIO.write(
                        image,
                        "png",
                        new java.io.File("screenshot_javacv.png")
                );

                System.out.println("Скриншот сохранен как screenshot_javacv.png");
            }

            grabber.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
