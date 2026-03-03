package ktb.billage.infra.rabbitmq;

import ktb.billage.infra.rabbitmq.config.RabbitMqConfig;
import ktb.billage.websocket.application.port.WebSocketBrokerRelayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "websocket.broker.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqWebSocketBrokerRelay implements WebSocketBrokerRelayPort {
    private final RabbitMqConfig rabbitMqConfig;

    @Override
    public String host() {
        return rabbitMqConfig.getHost();
    }

    @Override
    public int port() {
        return rabbitMqConfig.getPort();
    }

    @Override
    public String virtualHost() {
        return rabbitMqConfig.getVirtualHost();
    }

    @Override
    public String username() {
        return rabbitMqConfig.getUsername();
    }

    @Override
    public String password() {
        return rabbitMqConfig.getPassword();
    }
}
