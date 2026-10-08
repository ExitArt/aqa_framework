package utils;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaUtils {

    private final KafkaConsumer<String, String> consumer;

    public KafkaUtils(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // Отключаем автокоммит, он тормозит закрытие

        // УБИРАЕМ ПОДВИСАНИЕ: Отключаем внутреннюю телеметрию и сбор метрик клиента
        props.put("enable.metrics.push", "false");
        props.put("metrics.recording.level", "INFO");

        this.consumer = new KafkaConsumer<>(props);
    }

    public String getLastMessageFromTopic(String topicName) {
        TopicPartition partition = new TopicPartition(topicName, 0);
        consumer.assign(Collections.singletonList(partition));

        consumer.seekToEnd(Collections.singletonList(partition));
        long currentEndOffset = consumer.position(partition);

        if (currentEndOffset > 0) {
            consumer.seek(partition, currentEndOffset - 1);
        } else {
            consumer.seekToBeginning(Collections.singletonList(partition));
        }

        String lastValue = null;

        // Срезаем таймаут опроса до минимума — 50мс локально хватит за глаза
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(150));

        if (records != null && !records.isEmpty()) {
            for (ConsumerRecord<String, String> record : records) {
                lastValue = record.value();
            }
        }

        return lastValue;
    }

    public void close() {
        if (consumer != null) {
            // УБИРАЕМ ПОДВИСАНИЕ: Закрываем консьюмер жестко и мгновенно, не дожидаясь таймаутов сети
            consumer.close(Duration.ZERO);
        }
    }
}