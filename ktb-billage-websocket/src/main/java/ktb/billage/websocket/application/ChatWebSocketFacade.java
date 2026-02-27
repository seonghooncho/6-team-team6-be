package ktb.billage.websocket.application;

import ktb.billage.domain.chat.dto.ChatResponse;
import ktb.billage.domain.chat.service.ChatMessageCommandService;
import ktb.billage.domain.chat.service.ChatMessageQueryService;
import ktb.billage.domain.chat.service.ChatroomCommandService;
import ktb.billage.domain.chat.service.ChatroomQueryService;
import ktb.billage.domain.group.dto.GroupResponse;
import ktb.billage.domain.group.service.GroupService;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.websocket.application.event.ChatInboxSendEvent;
import ktb.billage.websocket.dto.ChatSendAckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatWebSocketFacade {
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final ChatroomQueryService chatroomQueryService;
    private final ChatroomCommandService chatroomCommandService;
    private final ChatMessageQueryService chatMessageQueryService;
    private final ChatMessageCommandService chatMessageCommandService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatResponse.ChatroomMembershipDto joinChatroom(Long chatroomId, Long userId) {
        chatroomQueryService.validateChatroomExists(chatroomId);

        List<Long> membershipIds = membershipService.findMembershipIds(userId);
        ChatResponse.ChatroomMembershipDto participation = chatroomQueryService.findParticipation(chatroomId, membershipIds);

        chatroomCommandService.readAllMessageBy(participation.chatroomId(), participation.isSeller());
        return participation;
    }

    public void validateParticipating(Long chatroomId, Long userId) {
        List<Long> membershipIds = membershipService.findMembershipIds(userId);
        chatroomQueryService.validateParticipating(chatroomId, membershipIds);
    }

    @Transactional
    public ChatSendAckResponse sendMessage(Long chatroomId, Long sendUserId, Long sendMembershipId, String message) {
        membershipService.validateMembershipOwner(sendUserId, sendMembershipId);
        chatroomQueryService.validateParticipating(chatroomId, sendMembershipId);

        Long receiveMembershipId = chatroomQueryService.findPartnerProfile(chatroomId, sendMembershipId).partnerId();
        Long receiveUserId = membershipService.findUserIdByMembershipId(receiveMembershipId);

        GroupResponse.GroupProfile groupProfile = groupService.findGroupProfileByMembershipId(sendMembershipId);

        Instant now = Instant.now();
        Long messageId = chatMessageCommandService.sendMessage(chatroomId, sendMembershipId, message, now);

        ChatSendAckResponse ack = new ChatSendAckResponse(chatroomId, sendMembershipId, String.valueOf(messageId), message, now, groupProfile.groupName());

        eventPublisher.publishEvent(new ChatInboxSendEvent(receiveUserId, ack));

        return ack;
    }

    public void readMessage(Long chatroomId, Long userId, Long membershipId, String readMessageId) {
        membershipService.validateMembershipOwner(userId, membershipId);
        chatroomQueryService.validateParticipating(chatroomId, membershipId);

        chatMessageQueryService.validateExistingMessage(chatroomId, readMessageId);

        Instant now = Instant.now();
        chatroomCommandService.readMessage(chatroomId, membershipId, readMessageId, now);
    }
}
