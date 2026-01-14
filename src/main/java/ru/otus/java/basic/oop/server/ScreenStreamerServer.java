package ru.otus.java.basic.oop.server;

import org.bytedeco.javacv.*;

public class ScreenStreamerServer {
    public static void main(String[] args) {
        FFmpegFrameGrabber grabber = null;
        FFmpegFrameRecorder recorder = null;

        try {
            System.out.println("🚀 Запуск простой трансляции экрана...");

            // 1. ЗАХВАТ ЭКРАНА
            grabber = new FFmpegFrameGrabber("desktop");
            grabber.setFormat("gdigrab");
            grabber.setFrameRate(20); // Меньше FPS = стабильнее
            grabber.setImageWidth(1280);
            grabber.setImageHeight(720);
            grabber.setOption("draw_mouse", "1");

            grabber.start();
            System.out.println("✅ Захват запущен: " +
                    grabber.getImageWidth() + "x" + grabber.getImageHeight());

            // 2. ТРАНСЛЯЦИЯ В ФАЙЛ ДЛЯ ТЕСТА
            String outputFile = "test_stream.ts"; // Тест в файл
            // Или для сети: "udp://127.0.0.1:5555"

            recorder = new FFmpegFrameRecorder(outputFile,
                    grabber.getImageWidth(),
                    grabber.getImageHeight());

            // МИНИМАЛЬНЫЕ НАСТРОЙКИ ДЛЯ МАКСИМАЛЬНОЙ СОВМЕСТИМОСТИ
            recorder.setFormat("mpegts");
            recorder.setFrameRate(20);
            recorder.setVideoBitrate(1000000); // 1 Mbps
            recorder.setVideoCodecName("libx264"); // Программный кодек
            recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);

            // КРИТИЧЕСКИЕ ОПЦИИ ДЛЯ РАБОТЫ
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");
            recorder.setVideoOption("crf", "28"); // Качество (23-28 нормально)
            recorder.setVideoOption("x264opts", "no-scenecut");

            // Запуск
            recorder.start();
            System.out.println("✅ Трансляция запущена в файл: " + outputFile);

            // 3. ЦИКЛ ТРАНСЛЯЦИИ
            int frameCount = 0;
            long startTime = System.currentTimeMillis();

            while (frameCount < 300) { // 300 кадров ~ 15 секунд
                Frame frame = grabber.grabImage();

                if (frame != null) {
                    recorder.record(frame);
                    frameCount++;

                    if (frameCount % 50 == 0) {
                        System.out.println("📊 Отправлено кадров: " + frameCount);
                    }
                }

                Thread.sleep(10); // Небольшая пауза
            }

            long elapsed = System.currentTimeMillis() - startTime;
            double fps = frameCount / (elapsed / 1000.0);
            System.out.printf("✅ Готово! Итог: %d кадров за %.1f сек (%.1f FPS)\n",
                    frameCount, elapsed / 1000.0, fps);

        } catch (Exception e) {
            System.err.println("❌ Ошибка:");
            e.printStackTrace();

            // Полезная информация для отладки
            System.out.println("\n=== ПОДСКАЗКИ ДЛЯ РЕШЕНИЯ ===");
            System.out.println("1. Установите полную версию FFmpeg: https://ffmpeg.org/download.html");
            System.out.println("2. Проверьте зависимости Maven в pom.xml");
            System.out.println("3. Попробуйте уменьшить разрешение до 640x480");
            System.out.println("4. Запустите как администратор (Windows)");

        } finally {
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}