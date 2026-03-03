package ktb.billage.infra.rabbitmq.config;

import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConditionalOnProperty(prefix = "websocket.broker.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {
    private static final String DEFAULT_VIRTUAL_HOST = "/";

    private final String host;
    private final int port;
    private final String virtualHost;
    private final String username;
    private final String password;

    public RabbitMqConfig(
            @Value("${rabbitmq.host}") String host,
            @Value("${rabbitmq.port}") String port,
            @Value("${rabbitmq.username}") String username,
            @Value("${rabbitmq.password}") String password
    ) {
        this.host = host;
        this.port = Integer.parseInt(port);
        this.virtualHost = DEFAULT_VIRTUAL_HOST;
        this.username = username;
        this.password = password;
    }
}
