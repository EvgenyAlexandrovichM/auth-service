package effectivemobile.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "verification")
@Getter
@Setter
public class VerificationProperties {

    private int ttlMinutes;
    private int rateLimitSeconds;
}
