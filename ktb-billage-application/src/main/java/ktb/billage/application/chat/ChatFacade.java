package ktb.billage.application.chat;

import ktb.billage.common.image.ImageService;
import ktb.billage.domain.chat.dto.ChatResponse;
import ktb.billage.domain.chat.service.ChatMessageQueryService;
import ktb.billage.domain.chat.service.ChatroomCommandService;
import ktb.billage.domain.chat.service.ChatroomQueryService;
import ktb.billage.domain.group.dto.GroupResponse;
import ktb.billage.domain.group.service.GroupService;
import ktb.billage.domain.membership.dto.MembershipProfile;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.domain.post.dto.PostResponse;
import ktb.billage.domain.post.service.PostQueryService;
import ktb.billage.domain.user.dto.UserResponse;
import ktb.billage.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatFacade {
    private final ChatMessageQueryService chatMessageQueryService;
    private final ChatroomQueryService chatroomQueryService;
    private final ChatroomCommandService chatroomCommandService;
    private final PostQueryService postQueryService;
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final UserService userService;
    private final ImageService imageService;

    public ChatResponse.Id createChatroom(Long postId, Long buyerUserId) {
        postQueryService.validatePost(postId);

        Long groupId = postQueryService.findGroupIdByPostId(postId);
        groupService.validateGroup(groupId);

        Long sellerMembershipId = postQueryService.findSellerIdByPostId(postId);
        Long buyerMembershipId = membershipService.findMembershipId(groupId, buyerUserId);

        return chatroomCommandService.create(postId, sellerMembershipId, buyerMembershipId);
    }

    public ChatResponse.Messages getMessagesByCursor(Long postId, Long chatroomId, Long userId, String cursor) {
        postQueryService.validatePost(postId);
        Long sellerMembershipId = postQueryService.findSellerIdByPostId(postId);

        Long groupId = membershipService.findGroupIdByMembershipId(sellerMembershipId);
        groupService.validateGroup(groupId);

        Long requestorMembershipId = membershipService.findMembershipId(groupId, userId);

        chatroomQueryService.validateChatroom(chatroomId);
        chatroomQueryService.validateParticipating(chatroomId, requestorMembershipId);
        return chatMessageQueryService.getMessagesByCursor(chatroomId, requestorMembershipId, cursor);
    }

    @Transactional
    public ChatResponse.ChatroomSummaries getChatroomsByMyPostId(Long postId, Long userId, String cursor) {

        postQueryService.validatePost(postId);
        String postFirstImageUrl = postQueryService.findPostFirstImageUrl(postId);

        Long sellerMembershipId = postQueryService.findSellerIdByPostId(postId);
        membershipService.validateMembershipOwner(userId, sellerMembershipId);

        Long groupId = membershipService.findGroupIdByMembershipId(sellerMembershipId);
        groupService.validateGroup(groupId);

        ChatResponse.ChatroomSummaryCores cores = chatroomQueryService.findChatroomSummariesByMyPostIdAndCursor(postId, cursor);
        List<Long> unreadMessageCounts = chatMessageQueryService.countUnreadPartnerMessagesByChatroomSummariesForSeller(cores.chatroomSummaryCores(), sellerMembershipId);

        GroupResponse.GroupProfile groupProfile = groupService.findGroupProfile(groupId);

        List<UserResponse.UserProfile> userProfiles = userService.findUserProfiles(toUserIds(cores.chatroomSummaryCores()));
        Map<Long, MembershipProfile> membershipProfilesMap = membershipService.findMembershipProfiles(toMembershipIds(cores.chatroomSummaryCores()));

        List<ChatResponse.ChatroomSummary> summaries = new ArrayList<>();
        for (int i = 0; i < cores.chatroomSummaryCores().size(); i++) {
            ChatResponse.ChatroomSummaryCore summaryCore = cores.chatroomSummaryCores().get(i);

            summaries.add(new ChatResponse.ChatroomSummary(
                    summaryCore.chatroomId(),
                    membershipProfilesMap.get(summaryCore.chatPartnerId()).membershipId(),
                    getImagePresignedUrl(userProfiles.get(i).avatarImageUrl()),
                    membershipProfilesMap.get(summaryCore.chatPartnerId()).nickname(),
                    groupProfile.groupId(),
                    groupProfile.groupName(),
                    postId,
                    getImagePresignedUrl(postFirstImageUrl),
                    summaryCore.lastMessageAt(),
                    summaryCore.lastMessage(),
                    unreadMessageCounts.get(i)
            ));
        }

        return new ChatResponse.ChatroomSummaries(
                summaries,
                cores.cursorDto()
        );
    }

    @Transactional(readOnly = true)
    public ChatResponse.ChatroomSummaries getMyParticipatingChatrooms(Long userId, String cursor) {
        List<Long> membershipIds = membershipService.findMembershipIds(userId);

        ChatResponse.ChatroomSummaryCores cores = chatroomQueryService.findChatroomSummariesByMembershipIdsAndCursor(membershipIds, cursor);
        Map<Long, Long> unreadCounts = chatMessageQueryService.countUnreadPartnerMessagesByChatroomSummariesAndMembershipIdForRole(cores, Set.copyOf(membershipIds));

        Map<Long, MembershipProfile> membershipProfilesMap = membershipService.findMembershipProfiles(toMembershipIds(cores.chatroomSummaryCores()));
        Map<Long, GroupResponse.GroupProfile> groupProfilesMap = groupService.findGroupProfiles(
                membershipProfilesMap.values().stream()
                        .map(MembershipProfile::groupId)
                        .toList()
        );
        Map<Long, UserResponse.UserProfile> userProfilesMap = userService.findUserProfilesMap(
                membershipProfilesMap.values().stream()
                        .map(MembershipProfile::userId)
                        .toList()
        );
        Map<Long, String> postFirstImageUrlsMap = postQueryService.findPostFirstImageUrls(
                cores.chatroomSummaryCores().stream()
                        .map(ChatResponse.ChatroomSummaryCore::postId)
                        .toList()
        );

        List<ChatResponse.ChatroomSummary> summaries = new ArrayList<>();
        List<ChatResponse.ChatroomSummaryCore> summaryCores = cores.chatroomSummaryCores();
        for (int i = 0; i < summaryCores.size(); i++) {
            ChatResponse.ChatroomSummaryCore core = summaryCores.get(i);

            MembershipProfile membershipProfile = membershipProfilesMap.get(core.chatPartnerId());
            Long groupId = membershipProfile.groupId();
            GroupResponse.GroupProfile groupProfile = groupProfilesMap.get(groupId);
            UserResponse.UserProfile userProfile = userProfilesMap.get(membershipProfile.userId());
            String postFirstImageUrl = postFirstImageUrlsMap.get(core.postId());

            summaries.add(new ChatResponse.ChatroomSummary(
                    core.chatroomId(),
                    membershipProfilesMap.get(core.chatPartnerId()).membershipId(),
                    getImagePresignedUrl(userProfile.avatarImageUrl()),
                    membershipProfilesMap.get(core.chatPartnerId()).nickname(),
                    groupId,
                    groupProfile.groupName(),
                    core.postId(),
                    getImagePresignedUrl(postFirstImageUrl),
                    core.lastMessageAt(),
                    core.lastMessage(),
                    unreadCounts.get(core.chatroomId())
                    ));
        }

        return new ChatResponse.ChatroomSummaries(summaries, cores.cursorDto());
    }

    public Long countAllUnReadMessagesOnParticipatingChatrooms(Long userId) {
        List<Long> myMembershipIds = membershipService.findMembershipIds(userId);
        if (myMembershipIds.isEmpty()) {
            return 0L;
        }

        List<ChatResponse.ChatroomMembershipDto> myChatroomMemberships = chatroomQueryService.findChatroomIdsByMembershipIds(myMembershipIds);

        Long unreadMessagesCountByMe = chatMessageQueryService.countUnreadMessagesByChatInfo(myChatroomMemberships);
        return unreadMessagesCountByMe;
    }

    public ChatResponse.PostSummary getPostSummaryInChatroom(Long postId, Long chatroomId, Long userId) {
        postQueryService.validatePost(postId);
        PostResponse.DetailCore postDetailCore = postQueryService.getPostDetailCore(postId);

        Long sellerMembershipId = postQueryService.findSellerIdByPostId(postId);

        Long groupId = membershipService.findGroupIdByMembershipId(sellerMembershipId);
        GroupResponse.GroupProfile groupProfile = groupService.findGroupProfile(groupId);

        List<Long> membershipIds = membershipService.findMembershipIds(userId);

        chatroomQueryService.validateChatroom(chatroomId);
        ChatResponse.ChatroomMembershipDto participation = chatroomQueryService.findParticipation(chatroomId, membershipIds);

        ChatResponse.PartnerProfile partnerProfile = chatroomQueryService.findPartnerProfile(chatroomId, participation.membershipId());

        return new ChatResponse.PostSummary(
                partnerProfile.partnerId(),
                partnerProfile.nickname(),
                groupId,
                groupProfile.groupName(),
                postId,
                postDetailCore.title(),
                getImagePresignedUrl(postDetailCore.imageUrls().imageInfos().getFirst().imageUrl()),
                postDetailCore.rentalFee(),
                postDetailCore.feeUnit().name(),
                postDetailCore.rentalStatus().name()
        );
    }

    public Long getPostIdByChatroomId(Long chatroomId, Long userId) {
        List<Long> membershipIds = membershipService.findMembershipIds(userId);

        chatroomQueryService.validateChatroom(chatroomId);
        chatroomQueryService.validateParticipating(chatroomId, membershipIds);

        Long postId = chatroomQueryService.findPostIdByChatroomId(chatroomId);
        return postId;
    }

    private List<Long> toUserIds(List<ChatResponse.ChatroomSummaryCore> cores) {
        return cores.stream()
                .mapToLong(ChatResponse.ChatroomSummaryCore::chatPartnerId)
                .map(membershipService::findUserIdByMembershipId)
                .boxed()
                .toList();
    }

    private List<Long> toMembershipIds(List<ChatResponse.ChatroomSummaryCore> cores) {
        return cores.stream()
                .mapToLong(ChatResponse.ChatroomSummaryCore::chatPartnerId)
                .boxed()
                .toList();
    }

    private String getImagePresignedUrl(String key) {
        return imageService.resolveUrl(key);
    }
}
