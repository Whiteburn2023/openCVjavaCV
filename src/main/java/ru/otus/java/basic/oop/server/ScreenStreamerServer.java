package ru.otus.java.basic.oop.server;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.Frame;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Date;

public class ScreenStreamerServer {
    private static final int DEFAULT_PORT = 5555;
    private static final int DEFAULT_FPS = 20;
    private static final int DEFAULT_BITRATE = 1_500_000; // 1.5 Mbps
    private static final String DEFAULT_IP = "127.0.0.1";

    private FFmpegFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;
    private AtomicBoolean isStreaming = new AtomicBoolean(false);
    private Thread streamingThread;

    // Конструктор
    public ScreenStreamerServer() {
        // Инициализация
    }

    /**
     * Запуск сервера трансляции
     * @param ip IP адрес для трансляции
     * @param port Порт для трансляции
     * @param width Ширина видео
     * @param height Высота видео
     * @param fps Частота кадров
     */
    public void start(String ip, int port, int width, int height, int fps, int bitrate) {
        if (isStreaming.get()) {
            System.out.println("⚠️  Трансляция уже запущена!");
            return;
        }

        streamingThread = new Thread(() -> {
            try {
                isStreaming.set(true);
                System.out.println("=".repeat(60));
                System.out.println("🚀 ЗАПУСК СЕРВЕРА ТРАНСЛЯЦИИ ЭКРАНА");
                System.out.println("Время: " + new Date());
                System.out.println("=".repeat(60));

                // 1. ПОЛУЧАЕМ РАЗРЕШЕНИЕ ЭКРАНА
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int screenWidth = screenSize.width;
                int screenHeight = screenSize.height;

                // Если параметры не заданы, используем половину экрана


                System.out.println("🖥️  Информация о системе:");
                System.out.println("   Разрешение экрана: " + screenWidth + "x" + screenHeight);
                System.out.println("   Трансляция в разрешении: " + width + "x" + height);
                System.out.println("   FPS: " + fps);
                System.out.println("   Битрейт: " + (bitrate / 1000) + " Кбит/с");
                System.out.println("   Адрес: udp://" + ip + ":" + port);

                // 2. ЗАХВАТ ЭКРАНА
                System.out.println("\n🎥 Инициализация захвата экрана...");

                grabber = new FFmpegFrameGrabber("desktop");
                grabber.setFormat("gdigrab");
                grabber.setFrameRate(fps);
                grabber.setImageWidth(width);
                grabber.setImageHeight(height);
                grabber.setOption("draw_mouse", "1"); // Захватывать курсор мыши
                grabber.setOption("fflags", "nobuffer");
                grabber.setOption("flags", "low_delay");
                grabber.setOption("probesize", "10M");

                grabber.start();
                System.out.println("✅ Захват экрана запущен");

                // 3. НАСТРОЙКА ТРАНСЛЯЦИИ
                System.out.println("\n⚙️  Настройка трансляции...");

                String outputUrl = String.format("udp://%s:%d?pkt_size=1316&buffer_size=65535", ip, port);

                recorder = new FFmpegFrameRecorder(outputUrl, width, height);

                // Основные настройки
                recorder.setFormat("mpegts"); // Формат для сетевой передачи
                recorder.setFrameRate(fps);
                recorder.setVideoBitrate(bitrate);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setGopSize(fps * 2); // Ключевой кадр каждые 2 секунды

                // Критические опции для низкой задержки
                recorder.setVideoOption("preset", "ultrafast");
                recorder.setVideoOption("tune", "zerolatency");
                recorder.setVideoOption("crf", "23");
                recorder.setVideoOption("x264-params", "keyint=" + (fps*2) + ":min-keyint=" + fps);

                // Отключаем аудио (только видео)
                recorder.setAudioChannels(0);

                // Запуск кодировщика
                recorder.start();
                System.out.println("✅ Трансляция запущена");

                // 4. ИНФОРМАЦИЯ О ТРАНСЛЯЦИИ
                System.out.println("\n" + "=".repeat(60));
                System.out.println("📡 ТРАНСЛЯЦИЯ АКТИВНА");
                System.out.println("URL для подключения: udp://" + ip + ":" + port);
                System.out.println("Статус: Ожидание подключения клиентов...");
                System.out.println("=".repeat(60));
                System.out.println("\nДля остановки нажмите Ctrl+C\n");

                // 5. ЦИКЛ ТРАНСЛЯЦИИ
                Frame frame;
                long frameCount = 0;
                long startTime = System.currentTimeMillis();
                long lastStatTime = startTime;

                while (isStreaming.get()) {
                    try {
                        // Захват кадра
                        frame = grabber.grabImage();

                        if (frame != null) {
                            // Отправка кадра
                            recorder.record(frame);
                            frameCount++;

                            // Статистика каждые 5 секунд
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastStatTime >= 5000) {
                                double elapsedSeconds = (currentTime - startTime) / 1000.0;
                                double actualFps = frameCount / elapsedSeconds;

                                System.out.printf("📊 Статистика: %d кадров | %.1f FPS\n",
                                        frameCount, actualFps);
                                lastStatTime = currentTime;
                            }
                        }

                        // Пауза для поддержания FPS
                        Thread.sleep(Math.max(1, 1000 / fps - 10));

                    } catch (Exception e) {
                        if (isStreaming.get()) {
                            System.err.println("⚠️  Ошибка при обработке кадра: " + e.getMessage());
                            Thread.sleep(100); // Небольшая пауза при ошибке
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("\n❌ КРИТИЧЕСКАЯ ОШИБКА:");
                e.printStackTrace();
                stop();
            }
        });

        streamingThread.setName("ScreenStreamer-Thread");
        streamingThread.start();
    }

    /**
     * Запуск с параметрами по умолчанию
     */
    public void start() {
        start(DEFAULT_IP, DEFAULT_PORT, 1280, 720, DEFAULT_FPS, DEFAULT_BITRATE);
    }

    /**
     * Остановка сервера
     */
    public void stop() {
        if (!isStreaming.get()) {
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("🛑 ОСТАНОВКА СЕРВЕРА...");

        isStreaming.set(false);

        try {
            // Ждем завершения потока
            if (streamingThread != null && streamingThread.isAlive()) {
                streamingThread.join(2000);
            }

            // Останавливаем рекордер
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                System.out.println("✅ Трансляция остановлена");
            }

            // Останавливаем захват
            if (grabber != null) {
                grabber.stop();
                grabber.release();
                System.out.println("✅ Захват экрана остановлен");
            }

        } catch (Exception e) {
            System.err.println("Ошибка при остановке: " + e.getMessage());
        }

        System.out.println("✅ Сервер остановлен");
        System.out.println("=".repeat(60));
    }

    /**
     * Проверка, активна ли трансляция
     */
    public boolean isStreaming() {
        return isStreaming.get();
    }

    /**
     * Точка входа (запуск сервера)
     */
    public static void main(String[] args) {
        ScreenStreamerServer server = new ScreenStreamerServer();

        // Парсинг аргументов командной строки
        String ip = DEFAULT_IP;
        int port = DEFAULT_PORT;
        int width = 1280;
        int height = 720;
        int fps = DEFAULT_FPS;
        int bitrate = DEFAULT_BITRATE;

        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-ip":
                        ip = args[++i];
                        break;
                    case "-port":
                        port = Integer.parseInt(args[++i]);
                        break;
                    case "-width":
                        width = Integer.parseInt(args[++i]);
                        break;
                    case "-height":
                        height = Integer.parseInt(args[++i]);
                        break;
                    case "-fps":
                        fps = Integer.parseInt(args[++i]);
                        break;
                    case "-bitrate":
                        bitrate = Integer.parseInt(args[++i]);
                        break;
                    case "-help":
                        printHelp();
                        return;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга аргументов: " + e.getMessage());
            printHelp();
            return;
        }

        // Добавляем обработчик Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\n⚠️  Получен сигнал завершения...");
            server.stop();
        }));

        // Запуск сервера
        server.start(ip, port, width, height, fps, bitrate);

        // Ожидание завершения (для консольного приложения)
        try {
            if (server.isStreaming()) {
                server.streamingThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printHelp() {
        System.out.println("ScreenStreamerServer - Сервер трансляции экрана");
        System.out.println("\nИспользование:");
        System.out.println("  java ScreenStreamerServer [опции]");
        System.out.println("\nОпции:");
        System.out.println("  -ip <адрес>      IP адрес для трансляции (по умолчанию: 127.0.0.1)");
        System.out.println("  -port <порт>     Порт для трансляции (по умолчанию: 5555)");
        System.out.println("  -width <ширина>  Ширина видео (по умолчанию: 1280)");
        System.out.println("  -height <высота> Высота видео (по умолчанию: 720)");
        System.out.println("  -fps <fps>       Частота кадров (по умолчанию: 20)");
        System.out.println("  -bitrate <bps>   Битрейт в битах/с (по умолчанию: 1500000)");
        System.out.println("  -help            Показать эту справку");
        System.out.println("\nПримеры:");
        System.out.println("  java ScreenStreamerServer");
        System.out.println("  java ScreenStreamerServer -ip 192.168.1.100 -port 5555 -width 1920 -height 1080");
        System.out.println("  java ScreenStreamerServer -fps 30 -bitrate 4000000");
    }
}