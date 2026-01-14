package ru.otus.java.basic.oop;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;

public class Main {
    public static void main(String[] args) {
        // Способ 1: Использование FrameGrabber (универсальный)
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0); // 0 = первая камера

        try {
            grabber.start(); // Запуск захвата

            // Захват одного кадра
            Frame frame = grabber.grab();

            // Конвертация Frame в Mat (матрицу OpenCV)
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
            Mat mat = converter.convert(frame);

            // Сохранение изображения
            org.bytedeco.opencv.global.opencv_imgcodecs.imwrite("capture.jpg", mat);

            grabber.stop();

        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
}