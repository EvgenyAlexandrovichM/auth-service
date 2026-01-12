package effectivemobile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email
) {
}
