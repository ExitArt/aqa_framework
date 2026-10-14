package tests;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import models.E2eOrderDto;
import net.datafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import steps.ApiSteps;
import steps.DbSteps;
import steps.KafkaSteps;
import steps.WireMockSteps; // Добавили импорт шагов WireMock

import java.util.Map;
import java.util.UUID;

@Epic("Тестирование Бэкенда (API + DB + Kafka + WireMock)")
@Feature("Интеграция сервиса заказов")
@DisplayName("Бэкенд: Сквозная проверка контрактов и данных")
public class ApiTest extends BaseTest {

    private final ApiSteps api = new ApiSteps();
    //private final DbSteps db = new DbSteps();
    private final KafkaSteps kafka = new KafkaSteps();
    private final WireMockSteps wm = new WireMockSteps(); // Инициализация шагов WireMock
    private final Faker faker = new Faker();
    private final String kafkaTopic = "api-orders-topic";

    @BeforeEach
    public void cleanUpWireMock() {
        // Очищаем контейнер WireMock перед каждым тестом, чтобы правила не накапливались
        wm.resetWireMock();
        // Поскольку мы теперь наследуемся от BaseTest, JUnit 5 автоматически
        // вызовет и этот метод, и метод setUp() из BaseTest (который очистит таблицы БД и запустит браузер).
    }

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

        // БЛОК РАБОТЫ С БД (PostgreSQL)
        // Имитируем, что наш API-сервис при получении запроса сохраняет данные в базу
        db.saveOrderToDb(mockOrderId, username, itemName, itemPrice, "CREATED");

        Allure.step("Проверить, что заказ успешно записался в таблицу PostgreSQL", () -> {
            String statusInDb = db.getOrderStatus(mockOrderId);
            Assertions.assertEquals("CREATED", statusInDb, "Ошибка: Заказ не найден в БД!");
        });

        // БЛОК РАБОТЫ С ОЧЕРЕДЬЮ (Kafka)
        // Имитируем, что бэкенд после записи в БД шлет событие в шину данных для других систем
        kafka.sendE2eOrderEvent(kafkaTopic, orderPayload);
        E2eOrderDto kafkaEvent = kafka.waitForLatestE2eOrder(kafkaTopic);

        Allure.step("Проверить, что событие из Kafka содержит корректные API-данные", () -> {
            Assertions.assertEquals(mockOrderId, kafkaEvent.getOrderId());
            Assertions.assertEquals(itemName, kafkaEvent.getItem());
            Assertions.assertEquals(itemPrice, kafkaEvent.getPrice());
        });
    }

    @Test
    @Story("Виртуализация сервисов: Использование Stubs и Mocks для изоляции бэкенда")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("API: Тестирование контрактов заказа с использованием WireMock в Docker")
    public void testWireMockStubAndMock() {
        // ПОДГОТОВКА ДАННЫХ
        String fakeItemName = faker.commerce().productName(); // Случайный товар
        String mockOrderId = "ORD-WM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();

        // НАСТРОЙКА STUB (Заглушки)
        // Шлём команду WireMock-контейнеру: "Если прилетит POST на /api/v1/orders с этим товаром — верни 201"
        wm.stubCreateOrder(mockOrderId, fakeItemName);

        // ОТПРАВКА РЕАЛЬНОГО ЗАПРОСА ЧЕРЕЗ REST ASSURED
        Map<String, Object> requestPayload = Map.of(
                "item", fakeItemName,
                "quantity", 1
        );

        Allure.step("REST Assured: Отправить реальный POST запрос на застабленный сервер WireMock", () -> {
            var responseBody = RestAssured.given()
                    .contentType("application/json")
                    .body(requestPayload)
                    .log().all() // Выведет отправляемый запрос в консоль
                    .when()
                    .post("http://localhost:8080/api/v1/orders") // Стучимся прямо в докер-порт WireMock
                    .then()
                    .log().all() // Выведет полученный ответ от заглушки в консоль
                    .statusCode(201) // Проверяем успешный статус заглушки 201 Created
                    .extract()
                    .jsonPath();

            // Проверяем данные, которые мы сами заложили в контракт заглушки
            Assertions.assertEquals(mockOrderId, responseBody.getString("orderId"),
                    "Ошибка: Заглушка вернула некорректный orderId!");
            Assertions.assertEquals("CREATED", responseBody.getString("status"),
                    "Ошибка: Заглушка вернула некорректный статус заказа!");
        });

        // MOCK-ВЕРИФИКАЦИЯ (Проверка поведения бэкенда)
        // Проверяем у контейнера: действительно ли к нему прилетал ровно 1 корректный запрос
        wm.verifyOrderWasSent(fakeItemName);
    }
}