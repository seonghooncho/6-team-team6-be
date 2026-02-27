package ktb.billage.websocket.application;

import ktb.billage.domain.chat.dto.PartnerProfile;
import ktb.billage.domain.chat.service.ChatMessageCommandService;
import ktb.billage.domain.chat.service.ChatroomQueryService;
import ktb.billage.domain.group.dto.GroupResponse;
import ktb.billage.domain.group.service.GroupService;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.websocket.application.event.ChatInboxSendEvent;
import ktb.billage.websocket.dto.ChatSendAckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketFacadeTest {
    @Mock
    private MembershipService membershipService;

    @Mock
    private ChatroomQueryService chatroomQueryService;

    @Mock
    private GroupService groupService;

    @Mock
    private ChatMessageCommandService chatMessageCommandService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatWebSocketFacade chatWebSocketFacade;

    @Test
    void sendMessage_ShouldResolveReceiverUserIdAndReturnChatSendResult() {
        Long chatroomId = 1L;
        Long sendUserId = 10L;
        Long sendMembershipId = 100L;
        Long receiveMembershipId = 200L;
        Long receiveUserId = 20L;
        String message = "test message";

        when(chatroomQueryService.findPartnerProfile(chatroomId, sendMembershipId))
                .thenReturn(new PartnerProfile(receiveMembershipId, "partner", false));
        when(membershipService.findUserIdByMembershipId(receiveMembershipId))
                .thenReturn(receiveUserId);
        when(chatMessageCommandService.sendMessage(eq(chatroomId), eq(sendMembershipId), eq(message), any(Instant.class)))
                .thenReturn(999L);
        when(groupService.findGroupProfileByMembershipId(sendMembershipId)).thenReturn(new GroupResponse.GroupProfile(888L, "test group", "group-cover.url"));

        ChatSendAckResponse ack = chatWebSocketFacade.sendMessage(chatroomId, sendUserId, sendMembershipId, message);

        assertThat(ack.chatroomId()).isEqualTo(chatroomId);
        assertThat(ack.membershipId()).isEqualTo(sendMembershipId);
        assertThat(ack.messageId()).isEqualTo("999");
        assertThat(ack.messageContent()).isEqualTo(message);
        assertThat(ack.createdAt()).isNotNull();

        verify(membershipService).validateMembershipOwner(sendUserId, sendMembershipId);
        verify(chatroomQueryService).validateParticipating(chatroomId, sendMembershipId);
        verify(chatroomQueryService).findPartnerProfile(chatroomId, sendMembershipId);
        verify(membershipService).findUserIdByMembershipId(receiveMembershipId);
        verify(chatMessageCommandService).sendMessage(chatroomId, sendMembershipId, message, ack.createdAt());
        verify(eventPublisher).publishEvent(new ChatInboxSendEvent(receiveUserId, ack));
    }
}
