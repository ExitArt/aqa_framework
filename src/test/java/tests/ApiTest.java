package tests;

import io.qameta.allure.*;
import models.E2eOrderDto;
import net.datafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import steps.ApiSteps;
import steps.DbSteps;
import steps.KafkaSteps;

import java.util.UUID;

@Epic("Тестирование Бэкенда (API + DB + Kafka)")
@Feature("Интеграция сервиса заказов")
@DisplayName("Бэкенд: Сквозная проверка контрактов и данных")
public class ApiTest {

    private final ApiSteps api = new ApiSteps();
    private final DbSteps db = new DbSteps();
    private final KafkaSteps kafka = new KafkaSteps();
    private final Faker faker = new Faker();
    private final String kafkaTopic = "api-orders-topic";

    @Test
    @Story("Создание заказа через API с последующей проверкой в PostgreSQL и Kafka")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("API: Сквозной бэкенд-тест без участия UI браузера")
    public void testBackendOrderWorkflow() throws Exception {
        // 1. ПОДГОТОВКА ДАННЫХ (Datafaker)
        String username = "standard_user";
        String itemName = faker.commerce().productName(); // Случайный товар (например, "Деревянный стул")
        String itemPrice = "$" + faker.commerce().price();
        String mockOrderId = "ORDER-API-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        E2eOrderDto orderPayload = new E2eOrderDto(
                username,
                mockOrderId,
                itemName,
                itemPrice,
                "CREATED"
        );

        // 2. БЛОК РАБОТЫ С БД (PostgreSQL)
        // Имитируем, что наш API-сервис при получении запроса сохраняет данные в базу
        db.saveOrderToDb(mockOrderId, username, itemName, itemPrice, "CREATED");

        Allure.step("Проверить, что заказ успешно записался в таблицу PostgreSQL", () -> {
            String statusInDb = db.getOrderStatus(mockOrderId);
            Assertions.assertEquals("CREATED", statusInDb, "Ошибка: Заказ не найден в БД!");
        });

        // 3. БЛОК РАБОТЫ С ОЧЕРЕДЬЮ (Kafka)
        // Имитируем, что бэкенд после записи в БД шлет событие в шину данных для других систем
        kafka.sendE2eOrderEvent(kafkaTopic, orderPayload);
        E2eOrderDto kafkaEvent = kafka.waitForLatestE2eOrder(kafkaTopic);

        Allure.step("Проверить, что событие из Kafka содержит корректные API-данные", () -> {
            Assertions.assertEquals(mockOrderId, kafkaEvent.getOrderId());
            Assertions.assertEquals(itemName, kafkaEvent.getItem());
            Assertions.assertEquals(itemPrice, kafkaEvent.getPrice());
        });
    }
}