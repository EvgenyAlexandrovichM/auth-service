package effectivemobile;

import effectivemobile.entity.User;
import effectivemobile.entity.VerificationCode;
import effectivemobile.exception.ExpiredVerificationCodeException;
import effectivemobile.exception.InvalidVerificationCodeException;
import effectivemobile.exception.UserNotFoundException;
import effectivemobile.kafka.KafkaProducerService;
import effectivemobile.repository.UserRepository;
import effectivemobile.repository.VerificationCodeRepository;
import effectivemobile.security.JwtService;
import effectivemobile.util.VerificationCodeFactory;
import effectivemobile.util.VerificationRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeFactory verificationCodeFactory;
    private final VerificationRateLimiter rateLimiter;
    private final JwtService jwtService;
    private final KafkaProducerService kafkaProducerService;

    public User register(String email) {
        rateLimiter.checkRateLimit(email);

        log.info("Starting registration for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("User not found, creating new user for email={}", email);
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setVerified(false);
                    return userRepository.save(newUser);
                });

        VerificationCode verificationCode = verificationCodeFactory.create(email);
        verificationCodeRepository.save(verificationCode);
        log.info("Verification code generated and saved for email={}", email);

        kafkaProducerService.sendVerificationCode(email, verificationCode.getCode());

        return user;
    }

    public String verify(String email, String code) {
        log.info("Verifying code for email={}", email);

        VerificationCode verificationCode = verificationCodeRepository.findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> {
                    log.warn("Invalid verification attempt for email={}, code={}", email, code);
                    return new InvalidVerificationCodeException("Invalid code");
                });

        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Expired code for email={}, code={}", email, code);
            throw new ExpiredVerificationCodeException("Code expired");
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found during verification for email={}", email);
                    return new UserNotFoundException("User not found");
                });

        user.setVerified(true);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        log.info("User email={} successfully verified, token issued", email);

        return token;
    }
}
