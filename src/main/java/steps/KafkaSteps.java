package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import models.OrderDto;
import utils.KafkaProducerUtils;
import utils.KafkaUtils;

import java.util.Objects;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class KafkaSteps {

    private final String bootstrapServers = "localhost:9092";
    // Включаем красивое форматирование INDENT_OUTPUT (чтобы JSON в отчете был с отступами и переносами)
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    // Аннотация подставит тему топика и ID заказа прямо в название шага Allure
    @Step("Отправить заказ с ID {order.orderId} в топик Kafka: {topic}")
    public void sendOrder(String topic, OrderDto order) throws Exception {
        KafkaProducerUtils producer = new KafkaProducerUtils(bootstrapServers);
        String json = mapper.writeValueAsString(order);

        // Автоматически прикрепляем JSON-запрос к отчету Allure
        attachJsonToAllure("Отправляемый JSON-запрос заказа", json);

        producer.sendMessage(topic, "order_key", json);
        producer.close();
    }

    @Step("Дождаться появления и прочитать новое сообщение из топика: {topic}")
    public OrderDto waitForLatestOrder(String topic) {
        String rawJson = getLatestMessageWithAwait(topic);
        try {
            // Автоматически прикрепляем JSON-ответ к отчету Allure
            attachJsonToAllure("Полученный JSON-ответ из Kafka", rawJson);

            return mapper.readValue(rawJson, OrderDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Не удалось распарсить JSON из Kafka в объект OrderDto!", e);
        }
    }

    // Оставляем метод без аннотации @Step, чтобы не спамить техническими деталями ожидания в отчете
    private String getLatestMessageWithAwait(String topic) {
        KafkaUtils consumer = new KafkaUtils(bootstrapServers, "aqa_static_group");

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

    // Создает файл-вложение внутри шага в Allure
    @Attachment(value = "{attachmentName}", type = "application/json")
    private String attachJsonToAllure(String attachmentName, String json) {
        return json != null ? json : "{\n  \"error\": \"Сообщение пустое (null)\"\n}";
    }
}