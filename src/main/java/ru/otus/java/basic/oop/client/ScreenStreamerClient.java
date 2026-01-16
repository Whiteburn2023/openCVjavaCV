package ru.otus.java.basic.oop.client;

import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenStreamerClient {
    private static final int DEFAULT_PORT = 5555;
    private static final String DEFAULT_IP = "127.0.0.1";

    private FFmpegFrameGrabber grabber;
    private CanvasFrame canvas;
    private Thread playbackThread;
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private String currentStreamUrl;

    // GUI компоненты
    private JFrame controlFrame;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusLabel;
    private JLabel statsLabel;

    /**
     * Запуск клиента с GUI
     */
    public void startWithGUI() {
        createGUI();
        controlFrame.setVisible(true);
    }

    /**
     * Создание графического интерфейса
     */
    private void createGUI() {
        // Основное окно управления
        controlFrame = new JFrame("Клиент трансляции экрана");
        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlFrame.setSize(500, 300);
        controlFrame.setLayout(new BorderLayout());

        // Панель подключения
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // IP адрес
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("IP сервера:"), gbc);

        gbc.gridx = 1;
        ipField = new JTextField(DEFAULT_IP, 15);
        connectionPanel.add(ipField, gbc);

        // Порт
        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(new JLabel("Порт:"), gbc);

        gbc.gridx = 1;
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 15);
        connectionPanel.add(portField, gbc);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());

        connectButton = new JButton("Подключиться");
        connectButton.addActionListener(e -> connectToStream());
        buttonPanel.add(connectButton);

        disconnectButton = new JButton("Отключиться");
        disconnectButton.addActionListener(e -> disconnect());
        disconnectButton.setEnabled(false);
        buttonPanel.add(disconnectButton);

        // Статус
        statusLabel = new JLabel("Готов к подключению");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Статистика
        statsLabel = new JLabel(" ");
        statsLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Сборка интерфейса
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statsLabel, BorderLayout.NORTH);

        controlFrame.add(mainPanel, BorderLayout.CENTER);
        controlFrame.add(southPanel, BorderLayout.SOUTH);

        // Обработка закрытия окна
        controlFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    /**
     * Подключение к потоку
     */
    private void connectToStream() {
        if (isPlaying.get()) {
            JOptionPane.showMessageDialog(controlFrame,
                    "Уже подключен к потоку!", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (ip.isEmpty() || portText.isEmpty()) {
            JOptionPane.showMessageDialog(controlFrame,
                    "Введите IP и порт сервера!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            currentStreamUrl = String.format("udp://%s:%d?buffer_size=65535", ip, port);

            updateStatus("Подключение к " + ip + ":" + port + "...", Color.ORANGE);
            connectButton.setEnabled(false);

            // Запуск потока воспроизведения
            playbackThread = new Thread(() -> playStream(currentStreamUrl));
            playbackThread.setName("StreamPlayback-Thread");
            playbackThread.start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(controlFrame,
                    "Порт должен быть числом!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Воспроизведение потока
     */
    private void playStream(String streamUrl) {
        try {
            isPlaying.set(true);

            System.out.println("=".repeat(60));
            System.out.println("🎬 ЗАПУСК КЛИЕНТА ВОСПРОИЗВЕДЕНИЯ");
            System.out.println("Время: " + new Date());
            System.out.println("Подключение к: " + streamUrl);
            System.out.println("=".repeat(60));

            // Настройка граббера для приема потока
            grabber = new FFmpegFrameGrabber(streamUrl);

            // Настройки для низкой задержки
            grabber.setOption("rtbufsize", "10M"); // Буфер 10 МБ
            grabber.setOption("max_delay", "500000"); // Макс задержка 0.5 сек
            grabber.setOption("fflags", "nobuffer");
            grabber.setOption("flags", "low_delay");
            grabber.setOption("analyzeduration", "100000"); // Анализ 100 мс

            // Запуск приема потока
            grabber.start();

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            double framerate = grabber.getFrameRate();

            SwingUtilities.invokeLater(() -> {
                updateStatus("Подключено! " + width + "x" + height + " @" + framerate + "fps",
                        new Color(0, 150, 0));
                disconnectButton.setEnabled(true);
            });

            System.out.println("✅ Поток получен:");
            System.out.println("   Разрешение: " + width + "x" + height);
            System.out.println("   FPS: " + framerate);
            System.out.println("   Кодек: " + grabber.getVideoCodecName());

            // Создание окна для отображения видео
            SwingUtilities.invokeLater(() -> {
                canvas = new CanvasFrame("Трансляция экрана [" + width + "x" + height + "]",
                        CanvasFrame.getDefaultGamma() / 2.2);
                canvas.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                canvas.setCanvasSize(Math.min(width, 1280), Math.min(height, 720));

                // Обработка закрытия окна видео
                canvas.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        disconnect();
                    }
                });
            });

            // Даем время окну создатьcя
            Thread.sleep(500);

            // Цикл воспроизведения
            Frame frame;
            long frameCount = 0;
            long startTime = System.currentTimeMillis();
            long lastStatTime = startTime;
            long lastFPSTime = startTime;
            int fpsCounter = 0;

            while (isPlaying.get() && canvas != null && canvas.isVisible()) {
                try {
                    // Получение кадра
                    frame = grabber.grab();

                    if (frame != null) {
                        frameCount++;
                        fpsCounter++;

                        // Отображение кадра
                        if (canvas != null && canvas.isVisible()) {
                            canvas.showImage(frame);
                        }

                        // Обновление статистики каждую секунду
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastStatTime >= 1000) {
                            final int fps = fpsCounter;
                            final long totalFrames = frameCount;
                            final long elapsed = (currentTime - startTime) / 1000;

                            SwingUtilities.invokeLater(() -> {
                                statsLabel.setText(String.format(
                                        "Кадров: %d | FPS: %d | Время: %d сек",
                                        totalFrames, fps, elapsed));
                            });

                            fpsCounter = 0;
                            lastStatTime = currentTime;
                        }

                        // Обновление FPS в заголовке окна каждые 5 секунд
                        if (currentTime - lastFPSTime >= 5000) {
                            double actualFps = frameCount / ((currentTime - startTime) / 1000.0);
                            if (canvas != null) {
                                canvas.setTitle(String.format(
                                        "Трансляция экрана [%dx%d] | FPS: %.1f",
                                        width, height, actualFps));
                            }
                            lastFPSTime = currentTime;
                        }
                    }

                    // Небольшая пауза для CPU
                    Thread.sleep(1);

                } catch (Exception e) {
                    if (isPlaying.get()) {
                        System.err.println("⚠️  Ошибка при обработке кадра: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("\n❌ ОШИБКА ПОДКЛЮЧЕНИЯ:");
            e.printStackTrace();

            SwingUtilities.invokeLater(() -> {
                updateStatus("Ошибка подключения: " + e.getMessage(), Color.RED);
                JOptionPane.showMessageDialog(controlFrame,
                        "Не удалось подключиться к потоку:\n" + e.getMessage(),
                        "Ошибка подключения", JOptionPane.ERROR_MESSAGE);
                resetConnectionUI();
            });

        } finally {
            disconnect();
        }
    }

    /**
     * Отключение от потока
     */
    public void disconnect() {
        if (!isPlaying.get()) {
            return;
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("🛑 ОТКЛЮЧЕНИЕ ОТ ПОТОКА...");

        isPlaying.set(false);

        try {
            // Закрываем окно видео
            if (canvas != null) {
                SwingUtilities.invokeLater(() -> canvas.dispose());
                canvas = null;
            }

            // Останавливаем граббер
            if (grabber != null) {
                grabber.stop();
                grabber.release();
                grabber = null;
            }

            // Ждем завершения потока
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.join(1000);
            }

            System.out.println("✅ Отключение завершено");

        } catch (Exception e) {
            System.err.println("Ошибка при отключении: " + e.getMessage());
        }

        SwingUtilities.invokeLater(this::resetConnectionUI);
    }

    /**
     * Сброс UI после отключения
     */
    private void resetConnectionUI() {
        updateStatus("Готов к подключению", new Color(0, 0, 150));
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        statsLabel.setText(" ");
    }

    /**
     * Обновление статуса в UI
     */
    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    /**
     * Точка входа (запуск клиента с GUI)
     */
    public static void main(String[] args) {
        // Устанавливаем Look and Feel для красивого интерфейса
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Запускаем GUI в Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            ScreenStreamerClient client = new ScreenStreamerClient();
            client.startWithGUI();
        });
    }

    /**
     * Консольная версия клиента (без GUI)
     */
    public static void consoleMain(String[] args) {
        if (args.length < 1) {
            System.out.println("Использование: java ScreenStreamerClient <server_ip> [port]");
            System.out.println("Пример: java ScreenStreamerClient 127.0.0.1 5555");
            return;
        }

        String ip = args[0];
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        ScreenStreamerClient client = new ScreenStreamerClient();

        // Обработка Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️  Завершение работы...");
            client.disconnect();
        }));

        // Подключение к потоку
        String streamUrl = String.format("udp://%s:%d?buffer_size=65535", ip, port);
        client.playStream(streamUrl);
    }
}
