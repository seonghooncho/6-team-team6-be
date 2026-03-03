package ktb.billage.websocket.application.adapter;

import ktb.billage.websocket.application.port.ChatWebSocketNotifier;
import ktb.billage.websocket.config.WebSocketDestinations;
import ktb.billage.websocket.dto.ChatSendAckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketChatInboxNotifier implements ChatWebSocketNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUserInbox(Long receiveUserId, ChatSendAckResponse ack) {
        messagingTemplate.convertAndSendToUser(receiveUserId.toString(), WebSocketDestinations.USER_INBOX_QUEUE, ack);
    }
}
