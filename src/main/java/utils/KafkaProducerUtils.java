package utils;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;

public class KafkaProducerUtils {

    private final KafkaProducer<String, String> producer;

    public KafkaProducerUtils(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // УБИРАЕМ ПОДВИСАНИЕ: Отключаем внутреннюю телеметрию продюсера
        props.put("enable.metrics.push", "false");
        props.put("metrics.recording.level", "INFO");

        this.producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        try {
            producer.send(record).get();
            System.out.println(">>> Успешно отправлено в Kafka (" + topic + "): " + value);
        } catch (Exception e) {
            System.err.println("ОШИБКА отправки в Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        if (producer != null) {
            // УБИРАЕМ ПОДВИСАНИЕ: Обрываем сетевые буферы продюсера мгновенно при закрытии
            producer.close(Duration.ZERO);
        }
    }
}