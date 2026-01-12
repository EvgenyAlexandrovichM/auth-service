package effectivemobile;

import effectivemobile.entity.User;
import effectivemobile.entity.VerificationCode;
import effectivemobile.exception.ExpiredVerificationCodeException;
import effectivemobile.exception.InvalidVerificationCodeException;
import effectivemobile.exception.TooManyRequestsException;
import effectivemobile.exception.UserNotFoundException;
import effectivemobile.kafka.KafkaProducerService;
import effectivemobile.repository.UserRepository;
import effectivemobile.repository.VerificationCodeRepository;
import effectivemobile.security.JwtService;
import effectivemobile.util.VerificationCodeFactory;
import effectivemobile.util.VerificationRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private VerificationCodeFactory verificationCodeFactory;

    @Mock
    private VerificationRateLimiter verificationRateLimiter;

    @Mock
    private JwtService jwtService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private AuthService authService;

    private final String email = "test@yandex.ru";
    private final String code = "123456";
    private User user;
    private VerificationCode verificationCode;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setVerified(false);

        verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setUsed(false);
        verificationCode.setExpiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void register_createNewUser_whenUserDoesNotExists() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(verificationCodeFactory.create(email)).thenReturn(verificationCode);

        User result = authService.register(email);

        assertEquals(email, result.getEmail());
        assertFalse(result.isVerified());

        verify(verificationRateLimiter).checkRateLimit(email);
        verify(userRepository).save(any(User.class));
        verify(verificationCodeRepository).save(verificationCode);
        verify(kafkaProducerService).sendVerificationCode(email, code);
    }

    @Test
    void register_reusesExistingUser_whenUserExists() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(verificationCodeFactory.create(email)).thenReturn(verificationCode);

        User result = authService.register(email);

        assertSame(user, result);

        verify(userRepository, never()).save(any());
        verify(verificationCodeRepository).save(verificationCode);
        verify(kafkaProducerService).sendVerificationCode(email, code);
    }

    @Test
    void register_throwsTooManyRequests_whenRateLimited() {
        doThrow(new TooManyRequestsException("Wait!!"))
                .when(verificationRateLimiter).checkRateLimit(email);

        assertThrows(TooManyRequestsException.class,
                () -> authService.register(email));

        verify(userRepository, never()).findByEmail(any());
        verify(verificationCodeRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendVerificationCode(any(), any());
    }

    @Test
    void verify_returnsToken_onSuccess() {
        when(verificationCodeRepository.findByEmailAndCodeAndUsedFalse(email, code))
                .thenReturn(Optional.of(verificationCode));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getId(), email)).thenReturn("TOKEN");

        String token = authService.verify(email, code);

        assertEquals("TOKEN", token);
        assertTrue(verificationCode.isUsed());
        assertTrue(user.isVerified());

        verify(verificationCodeRepository).save(verificationCode);
        verify(userRepository).save(user);
    }

    @Test
    void verify_throwsInvalidCode_whenCodeNotFound() {
        when(verificationCodeRepository.findByEmailAndCodeAndUsedFalse(email, code))
                .thenReturn(Optional.empty());

        assertThrows(InvalidVerificationCodeException.class,
                () -> authService.verify(email, code));
    }

    @Test
    void verify_throwsExpiredCode_whenCodeExpired() {
        verificationCode.setExpiresAt(Instant.now().minusSeconds(10));
        when(verificationCodeRepository.findByEmailAndCodeAndUsedFalse(email, code))
                .thenReturn(Optional.of(verificationCode));

        assertThrows(ExpiredVerificationCodeException.class,
                () -> authService.verify(email, code));
    }

    @Test
    void verify_throwsUserNotFound_whenUserMissing() {
        when(verificationCodeRepository.findByEmailAndCodeAndUsedFalse(email, code))
                .thenReturn(Optional.of(verificationCode));
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> authService.verify(email, code));
    }
}
