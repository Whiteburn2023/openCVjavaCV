package ru.otus.java.basic.oop;
import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.ffmpeg;

import java.awt.*;
import java.io.*;
import java.util.*;

public class SystemCheck {
    public static void main(String[] args) {
        System.out.println("=== ПРОВЕРКА СИСТЕМЫ ДЛЯ ТРАНСЛЯЦИИ ЭКРАНА ===");
        System.out.println("Дата проверки: " + new Date());
        System.out.println();

        // 1. Проверка Java и ОС
        checkJavaAndOS();

        // 2. Проверка FFmpeg через командную строку
        checkFFmpegViaCmd();

        // 3. Проверка JavaCV библиотек
        checkJavaCVDependencies();

        // 4. Проверка GPU
        checkGPUInfo();

        // 5. Проверка доступных устройств захвата
        checkCaptureDevices();

        System.out.println("\n=== РЕКОМЕНДАЦИИ ===");
        printRecommendations();
    }

    private static void checkJavaAndOS() {
        System.out.println("1. ИНФОРМАЦИЯ О СИСТЕМЕ:");
        System.out.println("   Java версия: " + System.getProperty("java.version"));
        System.out.println("   Java Home: " + System.getProperty("java.home"));
        System.out.println("   ОС: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("   Архитектура: " + System.getProperty("os.arch"));
        System.out.println("   Пользователь: " + System.getProperty("user.name"));
        System.out.println("   Рабочий каталог: " + new File(".").getAbsolutePath());
    }

    private static void checkFFmpegViaCmd() {
        System.out.println("\n2. ПРОВЕРКА FFMPEG:");

        String[] commands = {
                "ffmpeg -version",
                "where ffmpeg",  // Windows
                "which ffmpeg",  // Linux/Mac
                "ffmpeg -codecs | findstr h264"  // Windows с поиском кодеков
        };

        boolean ffmpegFound = false;

        for (String cmd : commands) {
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!ffmpegFound && line.contains("ffmpeg version")) {
                        System.out.println("   ✅ FFmpeg найден в системе!");
                        ffmpegFound = true;
                    }
                    if (line.contains("h264") || line.contains("H.264")) {
                        System.out.println("   🔍 " + line.trim());
                    }
                }

                process.waitFor();

            } catch (Exception e) {
                // Игнорируем ошибки команд
            }
        }

        if (!ffmpegFound) {
            System.out.println("   ❌ FFmpeg не найден в PATH");
            System.out.println("   💡 Скачайте FFmpeg с: https://ffmpeg.org/download.html");
            System.out.println("   💡 Добавьте в PATH: C:\\ffmpeg\\bin");
        }
    }

    private static void checkJavaCVDependencies() {
        System.out.println("\n3. ПРОВЕРКА JAVACV БИБЛИОТЕК:");

        try {
            // Попытка загрузки классов JavaCV
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
            System.out.println("   ✅ FFmpegFrameGrabber загружен");

            Class<?> recorderClass = Class.forName("org.bytedeco.javacv.FFmpegFrameRecorder");
            System.out.println("   ✅ FFmpegFrameRecorder загружен");

            // Проверка нативных библиотек
            System.out.println("   📍 Путь к нативным библиотекам:");
            String javaLibraryPath = System.getProperty("java.library.path");
            String[] paths = javaLibraryPath.split(File.pathSeparator);
            for (String path : paths) {
                System.out.println("      - " + path);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("   ❌ JavaCV библиотеки не найдены!");
            System.out.println("   💡 Проверьте pom.xml или добавьте зависимости:");
            System.out.println("   💡 groupId: org.bytedeco, artifactId: javacv-platform");
        }
    }

    private static void checkGPUInfo() {
        System.out.println("\n4. ИНФОРМАЦИЯ О GPU:");

        String os = System.getProperty("os.name").toLowerCase();

        try {
            Process process;
            if (os.contains("win")) {
                // Windows
                process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "path", "win32_VideoController", "get", "name,DriverVersion"}
                );
            } else if (os.contains("mac")) {
                // Mac
                process = Runtime.getRuntime().exec(
                        new String[]{"system_profiler", "SPDisplaysDataType"}
                );
            } else {
                // Linux
                process = Runtime.getRuntime().exec(
                        new String[]{"lspci", "|", "grep", "-i", "vga"}
                );
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0 &&
                        (line.contains("NVIDIA") || line.contains("AMD") ||
                                line.contains("Intel") || line.contains("Graphics"))) {
                    System.out.println("   💻 " + line.trim());
                }
            }

            process.waitFor();

        } catch (Exception e) {
            System.out.println("   ⚠️  Не удалось получить информацию о GPU");
        }

        // Проверка аппаратного ускорения
        System.out.println("\n5. ПРОВЕРКА АППАРАТНОГО УСКОРЕНИЯ:");

        String[] hwAccels = {"cuda", "qsv", "dxva2", "amf", "videotoolbox", "vaapi"};
        for (String accel : hwAccels) {
            try {
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-hwaccels");
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(accel)) {
                        System.out.println("   ✅ " + accel.toUpperCase() + " доступен");
                        break;
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                // Игнорируем
            }
        }
    }

    private static void checkCaptureDevices() {
        System.out.println("\n6. ПРОВЕРКА УСТРОЙСТВ ЗАХВАТА:");

        try {
            // Для Windows: проверка dshow устройств
            Process process = Runtime.getRuntime().exec("ffmpeg -list_devices true -f dshow -i dummy");
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            boolean inVideoDevices = false;
            while ((line = errorReader.readLine()) != null) {
                if (line.contains("DirectShow video devices")) {
                    inVideoDevices = true;
                    continue;
                }
                if (inVideoDevices && line.contains("]  \"")) {
                    System.out.println("   🎥 " + line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
                }
                if (line.contains("DirectShow audio devices")) {
                    inVideoDevices = false;
                }
            }

            process.waitFor();

        } catch (Exception e) {
            System.out.println("   ⚠️  Не удалось получить список устройств");
        }

        // Проверка захвата экрана
        System.out.println("\n7. ТЕСТ ЗАХВАТА ЭКРАНА:");

        try {
            // Простой тест через Java Robot
            Class<?> robotClass = Class.forName("java.awt.Robot");
            System.out.println("   ✅ java.awt.Robot доступен");

            // Тест создания скриншота
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println("   ❌ Графическая среда недоступна (headless режим)");
            } else {
                System.out.println("   ✅ Графическая среда доступна");

                // Разрешение экрана
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                System.out.println("   📐 Разрешение экрана: " +
                        screenSize.width + "x" + screenSize.height);
            }

        } catch (Exception e) {
            System.out.println("   ❌ Ошибка при проверке графической среды: " + e.getMessage());
        }
    }

    private static void printRecommendations() {
        System.out.println("\n📋 ЧТО ДЕЛАТЬ ДАЛЬШЕ:");

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            System.out.println("1. Установите FFmpeg для Windows:");
            System.out.println("   https://www.gyan.dev/ffmpeg/builds/");
            System.out.println("   Скачайте 'ffmpeg-release-essentials.zip'");
            System.out.println("   Распакуйте в C:\\ffmpeg");
            System.out.println("   Добавьте C:\\ffmpeg\\bin в PATH");

            System.out.println("\n2. Для аппаратного ускорения NVIDIA:");
            System.out.println("   - Установите драйверы NVIDIA GeForce");
            System.out.println("   - Установите CUDA Toolkit: https://developer.nvidia.com/cuda-downloads");

            System.out.println("\n3. Запустите трансляцию:");
            System.out.println("   java -cp \"target\\*;target\\dependency\\*\" SimpleScreenStreamer");

        } else if (os.contains("linux")) {
            System.out.println("1. Установите FFmpeg:");
            System.out.println("   Ubuntu/Debian: sudo apt install ffmpeg");
            System.out.println("   Fedora: sudo dnf install ffmpeg");

            System.out.println("\n2. Разрешите захват экрана:");
            System.out.println("   Для X11: установите x11grab");
            System.out.println("   Для Wayland: используйте --enable pipewire");

        } else if (os.contains("mac")) {
            System.out.println("1. Установите FFmpeg через Homebrew:");
            System.out.println("   brew install ffmpeg");

            System.out.println("\n2. Разрешите запись экрана:");
            System.out.println("   Системные настройки → Защита и безопасность → Конфиденциальность");
            System.out.println("   Добавьте приложение в список записи экрана");
        }

        System.out.println("\n🛠 ДЛЯ ТЕСТИРОВАНИЯ ПОПРОБУЙТЕ:");
        System.out.println("1. Запустить SimpleScreenStreamer.java");
        System.out.println("2. Если ошибка, уменьшите разрешение в коде до 640x480");
        System.out.println("3. Попробуйте сначала сохранить в файл, а не в сеть");
        System.out.println("4. Проверьте права доступа (запуск от администратора)");
    }
}
