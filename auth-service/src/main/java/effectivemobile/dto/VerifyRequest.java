package effectivemobile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRequest(

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,
        @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
        String code
) {
}
