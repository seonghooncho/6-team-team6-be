package ktb.billage.websocket.config;

import ktb.billage.websocket.application.port.WebSocketBrokerRelayPort;
import ktb.billage.websocket.exception.StompErrorHandler;
import ktb.billage.websocket.interceptor.ChatroomSubscriptionInterceptor;
import ktb.billage.websocket.interceptor.StompAuthChannelInterceptor;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import tools.jackson.databind.ObjectMapper;

import static ktb.billage.websocket.config.WebSocketDestinations.APP_PREFIX;
import static ktb.billage.websocket.config.WebSocketDestinations.QUEUE_PREFIX;
import static ktb.billage.websocket.config.WebSocketDestinations.TOPIC_PREFIX;
import static ktb.billage.websocket.config.WebSocketDestinations.USER_PREFIX;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${cors.allowed.origin}")
    private String[] allowedOrigins;

    private final ObjectMapper objectMapper;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final ChatroomSubscriptionInterceptor chatroomSubscriptionInterceptor;
    private final ObjectProvider<WebSocketBrokerRelayPort> webSocketBrokerRelayConfigPortProvider;
    @Value("${websocket.broker.relay.enabled:true}")
    private boolean relayEnabled;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) { // 웹소켓 연결을 위한 엔드포인트 billages.com/ws
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        WebSocketBrokerRelayPort relayPort = webSocketBrokerRelayConfigPortProvider.getIfAvailable();
        if (relayEnabled && relayPort != null) {
            registry.enableStompBrokerRelay(TOPIC_PREFIX, QUEUE_PREFIX)
                .setRelayHost(relayPort.host())
                .setRelayPort(relayPort.port())
                .setVirtualHost(relayPort.virtualHost())
                .setClientLogin(relayPort.username())
                .setClientPasscode(relayPort.password())
                .setSystemLogin(relayPort.username())
                .setSystemPasscode(relayPort.password());
        } else {
            registry.enableSimpleBroker(TOPIC_PREFIX, QUEUE_PREFIX);
            log.info("[WS][BROKER] simple broker enabled (relayEnabled={}, relayBeanPresent={})",
                    relayEnabled, relayPort != null);
        }

        registry.setApplicationDestinationPrefixes(APP_PREFIX); // 클라이언트에서 웹소켓 요청을 위한 프리픽스
        registry.setUserDestinationPrefix(USER_PREFIX); // 1:1 user destination 프리픽스
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor, chatroomSubscriptionInterceptor);
    }

    @Bean(name = "stompSubProtocolErrorHandler")
    public StompSubProtocolErrorHandler stompSubProtocolErrorHandler() {
        return new StompErrorHandler(objectMapper);
    }
}
