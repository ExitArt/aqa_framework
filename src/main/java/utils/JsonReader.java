package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.TestDataUser;
import java.io.InputStream;
import java.util.List;

public class JsonReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<TestDataUser> getUsersData() {
        try (InputStream is = JsonReader.class.getClassLoader().getResourceAsStream("users.json")) {
            if (is == null) {
                throw new IllegalArgumentException("Файл users.json не найден в папке resources!");
            }
            // Превращаем JSON-массив в типизированный List<TestDataUser>
            return mapper.readValue(is, new TypeReference<List<TestDataUser>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге тестовых данных JSON: " + e.getMessage(), e);
        }
    }
}