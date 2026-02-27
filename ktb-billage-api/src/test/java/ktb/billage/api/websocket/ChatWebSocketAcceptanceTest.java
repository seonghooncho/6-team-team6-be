package ktb.billage.api.websocket;

import ktb.billage.domain.chat.Chatroom;
import ktb.billage.domain.group.Group;
import ktb.billage.domain.membership.Membership;
import ktb.billage.domain.post.Post;
import ktb.billage.fixture.Fixtures;
import ktb.billage.domain.user.User;
import ktb.billage.support.AcceptanceTest;
import ktb.billage.support.AcceptanceTestSupport;
import ktb.billage.websocket.config.WebSocketDestinations;
import ktb.billage.websocket.dto.ChatSendAckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AcceptanceTest
class ChatWebSocketAcceptanceTest extends AcceptanceTestSupport {
    @Autowired
    private Fixtures fixtures;

    @Autowired
    private ObjectMapper objectMapper;

    private User me;
    private String myToken;
    private User another;
    private String anotherToken;

    private Group group;
    private Membership myMembership;
    private Membership anotherMembership;

    private Post myPost;
    private Chatroom chatroom;

    @BeforeEach
    void setUp() {
        me = fixtures.유저_생성();
        myToken = fixtures.토큰_생성(me);
        another = fixtures.또_다른_유저_생성();
        anotherToken = fixtures.토큰_생성(another);

        group = fixtures.그룹_생성("test");
        myMembership = fixtures.그룹_가입(group, me);
        anotherMembership = fixtures.그룹_가입(group, another);

        myPost = fixtures.게시글_생성(myMembership);
        chatroom = fixtures.채팅방_생성(myPost, anotherMembership);
    }

    @Test
    @DisplayName("JWT가 유효하면 CONNECT 성공")
    void connect_success_with_valid_jwt() throws Exception {
        User user = fixtures.유저_생성("wsConnectSuccess-" + System.nanoTime());
        String token = fixtures.토큰_생성(user);

        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        try {
            StompSession session = connect(client, wsUrl, token);
            assertThat(session.isConnected()).isTrue();
        } finally {
            client.stop();
        }
    }

    @Test
    @DisplayName("JWT가 없으면 CONNECT 실패")
    void connect_fail_without_jwt() {
        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        try {
            assertThatThrownBy(() -> connect(client, wsUrl, null))
                    .isInstanceOf(Exception.class);
        } finally {
            client.stop();
        }
    }

    @Test
    @DisplayName("CONNECT 이후 chatroom topic 구독 시 상대 메시지를 수신한다")
    void subscribe_chatroom_topic_after_connect() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        BlockingQueue<ChatSendAckResponse> chatroomMessages = new LinkedBlockingQueue<>();

        try {
            StompSession mySession = connect(client, wsUrl, myToken);
            subscribe(mySession, myToken,
                    WebSocketDestinations.CHATROOM_TOPIC_PREFIX + chatroom.getId(), chatroomMessages);
            Thread.sleep(200);

            StompSession anotherSession = connect(client, wsUrl, anotherToken);
            sendChatMessage(anotherSession, anotherToken,
                    chatroom.getId(), anotherMembership.getId(), "chatroom 구독 테스트");

            ChatSendAckResponse received = chatroomMessages.poll(5, TimeUnit.SECONDS);
            assertThat(received).isNotNull();
            assertThat(received.chatroomId()).isEqualTo(chatroom.getId());
            assertThat(received.membershipId()).isEqualTo(anotherMembership.getId());
            assertThat(received.messageContent()).isEqualTo("chatroom 구독 테스트");
        } finally {
            client.stop();
        }
    }

    @Test
    @DisplayName("CONNECT 이후 user inbox 구독 시 상대 메시지를 수신한다")
    void subscribe_user_inbox_after_connect() throws Exception {
        String wsUrl = "ws://localhost:" + port + "/ws";
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        BlockingQueue<ChatSendAckResponse> inboxMessages = new LinkedBlockingQueue<>();

        try {
            StompSession mySession = connect(client, wsUrl, myToken);
            subscribe(mySession, myToken, WebSocketDestinations.USER_INBOX_SUBSCRIBE, inboxMessages);
            Thread.sleep(200);

            StompSession anotherSession = connect(client, wsUrl, anotherToken);
            sendChatMessage(anotherSession, anotherToken,
                    chatroom.getId(), anotherMembership.getId(), "inbox 구독 테스트");

            ChatSendAckResponse received = inboxMessages.poll(5, TimeUnit.SECONDS);
            assertThat(received).isNotNull();
            assertThat(received.chatroomId()).isEqualTo(chatroom.getId());
            assertThat(received.membershipId()).isEqualTo(anotherMembership.getId());
            assertThat(received.messageContent()).isEqualTo("inbox 구독 테스트");
        } finally {
            client.stop();
        }
    }

    private StompSession connect(WebSocketStompClient stompClient, String wsUrl, String accessToken) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (accessToken != null) {
            connectHeaders.add(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken);
        }
        return stompClient.connectAsync(
                        wsUrl,
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, String accessToken, String destination, BlockingQueue<ChatSendAckResponse> sink) {
        StompHeaders subscribeHeaders = new StompHeaders();
        subscribeHeaders.setDestination(destination);
        subscribeHeaders.add(AUTHORIZATION_HEADER, BEARER_PREFIX + accessToken);
        session.subscribe(subscribeHeaders, new TestClientFrameHandler(sink));
    }

    private void sendChatMessage(StompSession session, String senderToken, Long chatroomId, Long senderMembershipId, String message) {
        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/app/chat/send");
        sendHeaders.add(AUTHORIZATION_HEADER, BEARER_PREFIX + senderToken);
        sendHeaders.setContentType(org.springframework.util.MimeTypeUtils.APPLICATION_JSON);
        String payload = """
                {
                  "chatroomId": %d,
                  "membershipId": %d,
                  "message": "%s"
                }
                """.formatted(chatroomId, senderMembershipId, message);
        session.send(sendHeaders, payload.getBytes(StandardCharsets.UTF_8));
    }

    private record ChatFixture(
            Long chatroomId,
            Long buyerMembershipId,
            String sellerToken,
            String buyerToken
    ) {
    }

    private class TestClientFrameHandler implements StompFrameHandler {
        private final BlockingQueue<ChatSendAckResponse> messageQueue;

        private TestClientFrameHandler(BlockingQueue<ChatSendAckResponse> messageQueue) {
            this.messageQueue = messageQueue;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            try {
                messageQueue.offer(objectMapper.readValue((byte[]) payload, ChatSendAckResponse.class));
            } catch (Exception ignored) {
            }
        }
    }
}
