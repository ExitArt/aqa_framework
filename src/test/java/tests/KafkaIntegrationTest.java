package tests;

import io.qameta.allure.*;
import models.OrderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import steps.AssertSteps;
import steps.KafkaSteps;

import static org.junit.jupiter.api.Assertions.*;

@Epic("Интеграция с бэкенд-сервисами")
@Feature("Очереди сообщений (Kafka)")
@DisplayName("Интеграционные тесты для топика обработки заказов")
public class KafkaIntegrationTest {

    private final KafkaSteps kafka = new KafkaSteps();
    private final AssertSteps verify = new AssertSteps(); // Подключаем шаги проверок
    private final String topic = "orders-topic";

    @Test
    @Story("Успешный цикл отправки и десериализации одиночного заказа")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Тест проверяет, что объект заказа корректно сериализуется бэкендом, улетает в брокер и без искажений вычитывается нашим консьюмером")
    @DisplayName("Проверка отправки заказа в Kafka и его валидация")
    public void testSendAndReceiveMessage() throws Exception {
        // 1. Готовим тестовые данные через DTO
        OrderDto expectedOrder = new OrderDto("standard_user", "SUCCESS", "12345");

        // 2. Имитируем работу бэкенда: отправляем объект
        kafka.sendOrder(topic, expectedOrder);

        // 3. Имитируем работу нашего теста: стабильно ждем и забираем объект
        OrderDto actualOrder = kafka.waitForLatestOrder(topic);

        // 4. Делаем строгие проверки (Assertions) через класс шагов
        verify.assertOrderDetails(expectedOrder, actualOrder);

        System.out.println("🔥 ИНТЕГРАЦИЯ С KAFKA ПОЛНОСТЬЮ СТАБИЛЬНА И РАБОТАЕТ!");
    }
}