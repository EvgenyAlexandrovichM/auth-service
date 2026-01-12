package effectivemobile.kafka;

import effectivemobile.dto.VerificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, VerificationMessage> kafkaTemplate;
    @Value("${kafka.topics.verification}")
    private String topic;

    public void sendVerificationCode(String email, String code) {
        VerificationMessage message = new VerificationMessage(email, code);

        log.info("Sending verification code to Kafka={}", message);

        kafkaTemplate.send(topic, email, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send verification message to Kafka", ex);
                    } else {
                        log.info("Verification message sent to Kafka, offset={}", result.getRecordMetadata().offset());
                    }
                });
    }
}
