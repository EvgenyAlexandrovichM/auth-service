package effectivemobile.util;

import effectivemobile.config.VerificationProperties;
import effectivemobile.entity.VerificationCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class VerificationCodeFactory {

    private final VerificationProperties properties;
    private final SecureRandom random = new SecureRandom();

    public VerificationCode create(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(Instant.now().plus(properties.getTtlMinutes(), ChronoUnit.MINUTES));
        verificationCode.setUsed(false);

        return verificationCode;
    }
}
