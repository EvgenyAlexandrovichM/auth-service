package effectivemobile.util;

import effectivemobile.config.VerificationProperties;
import effectivemobile.exception.TooManyRequestsException;
import effectivemobile.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class VerificationRateLimiter {

    private final VerificationCodeRepository repository;
    private final VerificationProperties properties;

    public void checkRateLimit(String email) {
        repository.findTopByEmailOrderByCreatedAtDesc(email)
                .ifPresent(last -> {
                    Instant next = last.getCreatedAt()
                            .plus(properties.getRateLimitSeconds(), ChronoUnit.SECONDS);

                    if (Instant.now().isBefore(next)) {
                        long left = Duration.between(Instant.now(), next).toSeconds();
                        throw new TooManyRequestsException("Wait " + left + " seconds");
                    }
                });
    }
}
