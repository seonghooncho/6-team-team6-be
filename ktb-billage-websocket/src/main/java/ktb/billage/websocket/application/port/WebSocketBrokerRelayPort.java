package ktb.billage.websocket.application.port;

public interface WebSocketBrokerRelayPort {
    String host();

    int port();

    String virtualHost();

    String username();

    String password();
}
