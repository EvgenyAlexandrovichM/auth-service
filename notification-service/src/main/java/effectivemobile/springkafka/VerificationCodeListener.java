package effectivemobile.springkafka;

import effectivemobile.notification.VerificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VerificationCodeListener {

    @KafkaListener(topics = "${kafka.topics.verification}")
    public void listen(VerificationMessage message) {
        log.info("Received verification code: email={}, code={}", message.email(), message.code());
    }
}
