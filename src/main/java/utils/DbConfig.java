package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DbConfig {
    private static final Map<String, String> envMap = new HashMap<>();

    static {
        // Читаем локальный .env файл, если он есть
        if (Files.exists(Paths.get(".env"))) {
            try {
                Files.readAllLines(Paths.get(".env")).forEach(line -> {
                    // Игнорируем комментарии и пустые строки
                    if (!line.trim().startsWith("#") && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            envMap.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                });
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла .env: " + e.getMessage());
            }
        }
    }

    // Жесткий метод получения секретов
    private static String getRequiredEnv(String key) {
        // Сначала ищем в памяти (из файла .env)
        if (envMap.containsKey(key)) {
            return envMap.get(key);
        }
        // Если в .env нет, ищем в системных переменных окружения (для GitHub Actions)
        String systemEnv = System.getenv(key);
        if (systemEnv != null && !systemEnv.isBlank()) {
            return systemEnv;
        }

        // Если секрета нет нигде — взрываем тест с понятным описанием проблемы
        throw new IllegalStateException("КРИТИЧЕСКАЯ ОШИБКА БЕЗОПАСНОСТИ: Переменная " + key + " не найдена ни в файле .env, ни в системе!");
    }

    public static String getUrl() {
        String dbName = getRequiredEnv("DB_NAME");
        return "jdbc:postgresql://localhost:5432/" + dbName;
    }

    public static String getUser() {
        return getRequiredEnv("DB_USER");
    }

    public static String getPassword() {
        return getRequiredEnv("DB_PASSWORD");
    }
}