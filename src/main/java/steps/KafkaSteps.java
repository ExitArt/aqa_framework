package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import models.E2eOrderDto;
import models.OrderDto;
import utils.KafkaProducerUtils;
import utils.KafkaUtils;

import java.util.Objects;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class KafkaSteps {

    private final String bootstrapServers = "localhost:9092";
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Step("Отправить заказ с ID {order.orderId} в топик Kafka: {topic}")
    public void sendOrder(String topic, OrderDto order) throws Exception {
        KafkaProducerUtils producer = new KafkaProducerUtils(bootstrapServers);
        String json = mapper.writeValueAsString(order);

        attachJsonToAllure("Отправляемый JSON-запрос заказа", json);

        producer.sendMessage(topic, "order_key", json);
        producer.close();
    }

    @Step("Дождаться появления и прочитать новое сообщение из топика: {topic}")
    public OrderDto waitForLatestOrder(String topic) {
        // Передаем уникальную группу именно для этого шага
        String rawJson = getLatestMessageWithAwait(topic, "aqa_orders_group");

        if (rawJson == null) {
            rawJson = "{\n  \"error\": \"Сообщение пустое (null)\"\n}";
        }

        try {
            attachJsonToAllure("Полученный JSON-ответ из Kafka", rawJson);
            return mapper.readValue(rawJson, OrderDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось распарсить JSON из Kafka в объект OrderDto!", e);
        }
    }

    @Step("Отправить E2E событие заказа {order.orderId} в Kafka")
    public void sendE2eOrderEvent(String topic, E2eOrderDto order) throws Exception {
        KafkaProducerUtils producer = new KafkaProducerUtils(bootstrapServers);
        String json = mapper.writeValueAsString(order);

        attachJsonToAllure("Отправляемое E2E событие в Kafka", json);

        producer.sendMessage(topic, "e2e_order_key", json);
        producer.close();
    }

    @Step("Дождаться E2E события из топика Kafka: {topic}")
    public E2eOrderDto waitForLatestE2eOrder(String topic) {
        // Передаем уникальную группу именно для E2E шага
        String rawJson = getLatestMessageWithAwait(topic, "aqa_e2e_group");

        if (rawJson == null) {
            rawJson = "{\n  \"error\": \"Сообщение пустое (null)\"\n}";
        }

        try {
            attachJsonToAllure("Вычитанное E2E событие из Kafka", rawJson);
            return mapper.readValue(rawJson, E2eOrderDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось распарсить E2E JSON!", e);
        }
    }

    // Добавил параметр String groupId, чтобы разделить потоки чтения
    private String getLatestMessageWithAwait(String topic, String groupId) {
        KafkaUtils consumer = new KafkaUtils(bootstrapServers, groupId);

        try {
            return await()
                    .atMost(5, SECONDS)
                    .pollInterval(200, MILLISECONDS)
                    .until(
                            () -> consumer.getLastMessageFromTopic(topic),
                            Objects::nonNull
                    );
        } finally {
            consumer.close();
        }
    }

    @Attachment(value = "{attachmentName}", type = "application/json")
    private String attachJsonToAllure(String attachmentName, String json) {
        return json;
    }
}