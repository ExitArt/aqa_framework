package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.OrderDto;
import utils.KafkaProducerUtils;
import utils.KafkaUtils;

import java.util.Objects;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class KafkaSteps {

    private final String bootstrapServers = "localhost:9092";
    private final ObjectMapper mapper = new ObjectMapper();

    // 1. Шаг отправки (из Java-объекта делает JSON и шлет в Kafka)
    public void sendOrder(String topic, OrderDto order) throws Exception {
        KafkaProducerUtils producer = new KafkaProducerUtils(bootstrapServers);
        String json = mapper.writeValueAsString(order);
        producer.sendMessage(topic, "order_key", json);
        producer.close();
    }

    // 2. Шаг получения (использует твой метод с Awaitility и парсит результат в DTO)
    public OrderDto waitForLatestOrder(String topic) {
        String rawJson = getLatestMessageWithAwait(topic);
        try {
            // Превращаем полученный чистый JSON обратно в Java-объект
            return mapper.readValue(rawJson, OrderDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось распарсить JSON из Kafka в объект OrderDto!", e);
        }
    }

    // 3. Твой метод умного ожидания (вычитывает сырую строку)
    private String getLatestMessageWithAwait(String topic) {
        KafkaUtils consumer = new KafkaUtils(bootstrapServers, "aqa_static_group");

        try {
            // Ждем максимум 5 секунд, опрашивая Kafka каждые 200 мс.
            // Как только метод вернет НЕ null — возвращаем строку мгновенно!
            return await()
                    .atMost(5, SECONDS)
                    .pollInterval(200, MILLISECONDS)
                    .until(
                            () -> consumer.getLastMessageFromTopic(topic),
                            Objects::nonNull
                    );
        } finally {
            // Твой кастомный быстрый close() отработает в любом случае
            consumer.close();
        }
    }
}