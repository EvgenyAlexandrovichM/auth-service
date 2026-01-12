package effectivemobile.controller;

import effectivemobile.dto.JwtResponse;
import effectivemobile.dto.RegisterRequest;
import effectivemobile.dto.UserDto;
import effectivemobile.dto.VerifyRequest;
import effectivemobile.dto.mapper.UserMapper;
import effectivemobile.entity.User;
import effectivemobile.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email());

        return ResponseEntity.ok(mapper.toDto(user));
    }

    @PostMapping("/verify")
    public ResponseEntity<JwtResponse> verify(@Valid @RequestBody VerifyRequest request) {
        String token = authService.verify(request.email(), request.code());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(mapper.toDto(user));
    }
}
