package effectivemobile;

import com.fasterxml.jackson.databind.ObjectMapper;
import effectivemobile.config.SecurityConfig;
import effectivemobile.controller.AuthController;
import effectivemobile.dto.RegisterRequest;
import effectivemobile.dto.UserDto;
import effectivemobile.dto.VerifyRequest;
import effectivemobile.dto.mapper.UserMapper;
import effectivemobile.entity.User;
import effectivemobile.exception.ExpiredVerificationCodeException;
import effectivemobile.exception.InvalidVerificationCodeException;
import effectivemobile.exception.TooManyRequestsException;
import effectivemobile.exception.UserNotFoundException;
import effectivemobile.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        })

@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private String email;
    private String code;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setup() {
        email = "test@yandex.ru";
        code = "123456";
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setVerified(true);

        userDto = new UserDto(user.getId(), user.getEmail(), true);
    }

    @Test
    void register_returnsUserDto_onSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(email);

        when(authService.register(email)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void register_returnsBadRequest_onInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("invalid email");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returnsTooManyRequest_whenRateLimited() throws Exception {
        RegisterRequest request = new RegisterRequest(email);

        when(authService.register(email)).thenThrow(new TooManyRequestsException("Wait!"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void verify_returnsToken_onSuccess() throws Exception {
        VerifyRequest request = new VerifyRequest(email, code);

        when(authService.verify(email, code)).thenReturn("TOKEN");

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"));
    }

    @Test
    void verify_returnsBadRequest_onInvalidCode() throws Exception {
        VerifyRequest request = new VerifyRequest(email, code);

        when(authService.verify(anyString(), anyString()))
                .thenThrow(new InvalidVerificationCodeException("Invalid"));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_returnsBadRequest_onExpiredCode() throws Exception {
        VerifyRequest request = new VerifyRequest(email, code);

        when(authService.verify(anyString(), anyString()))
                .thenThrow(new ExpiredVerificationCodeException("Expired"));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void verify_returnsNotFound_whenUserMissing() throws Exception {
        VerifyRequest request = new VerifyRequest(email, code);

        when(authService.verify(anyString(), anyString()))
                .thenThrow(new UserNotFoundException("Not found"));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_returnsUserDto_onSuccess() throws Exception {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.verified").value(true));
    }
}

