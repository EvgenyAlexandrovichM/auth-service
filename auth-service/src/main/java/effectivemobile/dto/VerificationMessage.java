package effectivemobile.dto;

public record VerificationMessage(
        String email,
        String code
) {
}
