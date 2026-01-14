package ru.otus.java.basic.oop.client;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenStreamerClient {
    private static final int CLIENT_PORT = 5555;
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB буфер
    private FFmpegFrameGrabber grabber;
    private CanvasFrame canvas;
    private AtomicBoolean isPlaying = new AtomicBoolean(true);

    public void startPlayback(String serverIp) {
        try {
            System.out.println("🎬 Запуск клиента воспроизведения...");

            // 1. Получение потока по UDP
            String streamUrl = String.format("udp://%s:%d?buffer_size=%d&fifo_size=5000000",
                    serverIp, CLIENT_PORT, BUFFER_SIZE);

            grabber = new FFmpegFrameGrabber(streamUrl);
            grabber.setOption("rtbufsize", "100M"); // Буфер для сетевых скачков
            grabber.setOption("max_delay", "500000"); // Макс задержка 0.5с

            // 2. Настройки декодера
            grabber.setVideoCodecName("h264");
            grabber.setFormat("mpegts");
            grabber.setFrameRate(30);

            // 3. Создание окна для отображения
            canvas = new CanvasFrame("Трансляция экрана", CanvasFrame.getDefaultGamma() / 2.2);
            canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            canvas.setCanvasSize(1280, 720); // Масштабирование

            // 4. Запуск воспроизведения
            grabber.start();

            System.out.println("✅ Подключено к серверу: " + serverIp);

            // 5. Главный цикл воспроизведения
            Frame frame;
            long frameCount = 0;
            long lastTime = System.currentTimeMillis();
            long startTime = System.currentTimeMillis();

            while (isPlaying.get() && canvas.isVisible()) {
                frame = grabber.grab();

                if (frame != null) {
                    frameCount++;

                    // Отображение кадра
                    canvas.showImage(frame);

                    // Статистика
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime >= 1000) {
                        double fps = frameCount / ((currentTime - startTime) / 1000.0);
                        canvas.setTitle(String.format("Трансляция экрана [%.1f FPS]", fps));
                        lastTime = currentTime;
                    }
                }

                // Небольшая пауза для CPU
                Thread.sleep(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopPlayback();
        }
    }

    public void stopPlayback() {
        isPlaying.set(false);
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
            if (canvas != null) {
                canvas.dispose();
            }
            System.out.println("🛑 Воспроизведение остановлено");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: java ScreenStreamerClient <server_ip>");
            System.out.println("Пример: java ScreenStreamerClient 192.168.1.100");
            return;
        }

        ScreenStreamerClient client = new ScreenStreamerClient();

        // Завершение по Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️  Завершение клиента...");
            client.stopPlayback();
        }));

        client.startPlayback(args[0]);
    }
}
