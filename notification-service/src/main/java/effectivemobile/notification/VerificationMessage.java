package effectivemobile.notification;

public record VerificationMessage(
        String email,
        String code
) {
}
