package tests;

import org.junit.jupiter.api.Test;
import utils.KafkaProducerUtils;
import utils.KafkaUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaIntegrationTest {

    @Test
    public void testSendAndReceiveMessage() {
        String bootstrapServers = "localhost:9092";
        String topic = "orders-topic";

        // 1. Имитируем работу бэкенда: отправляем сообщение в Kafka
        KafkaProducerUtils producer = new KafkaProducerUtils(bootstrapServers);
        String testJson = "{\"user\": \"standard_user\", \"status\": \"SUCCESS\", \"orderId\": \"12345\"}";

        producer.sendMessage(topic, "order_key", testJson);
        producer.close();

        // 2. Имитируем работу нашего теста: читаем это сообщение из Kafka
        // Используем фиксированную группу для моментального отклика сети
        KafkaUtils consumer = new KafkaUtils(bootstrapServers, "aqa_static_group");

        try {
            String receivedMessage = consumer.getLastMessageFromTopic(topic);
            System.out.println("<<< Тест прочитал из Kafka: " + receivedMessage);

            // 3. Делаем проверки (Assertions)
            assertNotNull(receivedMessage, "Ошибка: Сообщение не было прочитано из Kafka (вернулся null)!");
            assertTrue(receivedMessage.contains("standard_user"), "Ошибка: В сообщении нет имени пользователя!");
            assertTrue(receivedMessage.contains("12345"), "Ошибка: В сообщении нет правильного orderId!");

            System.out.println("🔥 ИНТЕГРАЦИЯ С KAFKA НА ЛОКАЛКЕ ПОЛНОСТЬСТЬЮ РАБОТАЕТ!");
        } finally {
            consumer.close();
        }
    }
}