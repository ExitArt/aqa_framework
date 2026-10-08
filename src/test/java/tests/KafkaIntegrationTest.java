package tests;

import models.OrderDto;
import org.junit.jupiter.api.Test;
import steps.KafkaSteps;

import static org.junit.jupiter.api.Assertions.*;

public class KafkaIntegrationTest {

    private final KafkaSteps kafka = new KafkaSteps();
    private final String topic = "orders-topic";

    @Test
    public void testSendAndReceiveMessage() throws Exception {
        // 1. Готовим тестовые данные через DTO
        OrderDto expectedOrder = new OrderDto("standard_user", "SUCCESS", "12345");

        // 2. Имитируем работу бэкенда: отправляем объект
        kafka.sendOrder(topic, expectedOrder);

        // 3. Имитируем работу нашего теста: стабильно ждем и забираем объект
        OrderDto actualOrder = kafka.waitForLatestOrder(topic);

        // 4. Делаем строгие проверки (Assertions) объект в объект
        assertNotNull(actualOrder, "Ошибка: Заказ не был прочитан из Kafka!");
        assertEquals(expectedOrder.getUser(), actualOrder.getUser(), "Ошибка: Неверный user!");
        assertEquals(expectedOrder.getOrderId(), actualOrder.getOrderId(), "Ошибка: Неверный orderId!");
        assertEquals(expectedOrder.getStatus(), actualOrder.getStatus(), "Ошибка: Неверный status!");

        System.out.println("🔥 ИНТЕГРАЦИЯ С KAFKA ПОЛНОСТЬЮ СТАБИЛЬНА И РАБОТАЕТ!");
    }
}