package ktb.billage.application.group;

import ktb.billage.domain.group.dto.GroupResponse;
import ktb.billage.domain.chat.service.ChatroomCommandService;
import ktb.billage.domain.group.service.GroupService;
import ktb.billage.domain.membership.dto.MembershipProfile;
import ktb.billage.domain.membership.dto.MembershipResponse;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.domain.post.service.PostCommandService;
import ktb.billage.domain.user.User;
import ktb.billage.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupFacade {
    private final MembershipService membershipService;
    private final GroupService groupService;
    private final UserService userService;
    private final PostCommandService postCommandService;
    private final ChatroomCommandService chatroomCommandService;

    @Transactional
    public Long createGroup(Long userId, String groupName, String groupCoverImageUrl) {
        User user = userService.findById(userId);
        membershipService.validateUserGroupLimit(userId);

        Long groupId = groupService.create(groupName, groupCoverImageUrl);

        membershipService.join(groupId, userId, user.getLoginId());

        return groupId;
    }

    @Transactional
    public String createInvitation(Long groupId, Long userId) {
        groupService.validateGroup(groupId);
        membershipService.validateMembership(groupId, userId);

        return groupService.findOrCreateInvitationToken(groupId);
    }

    @Transactional
    public GroupResponse.GroupProfile checkInvitation(String invitationToken, Long userId) {
        Long groupId = groupService.findGroupIdByInvitationToken(invitationToken);

        membershipService.validateNotMember(groupId, userId);
        membershipService.validateUserGroupLimit(userId);
        membershipService.validateGroupCapacity(groupId);

        return groupService.findGroupProfile(groupId);
    }

    @Transactional
    public Long joinGroup(String invitationToken, Long userId, String nickname) {
        Long groupId = groupService.findGroupIdByInvitationToken(invitationToken);

        membershipService.validateNotMember(groupId, userId);
        membershipService.validateUserGroupLimit(userId);
        membershipService.validateGroupCapacity(groupId);

        return membershipService.join(groupId, userId, nickname);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        groupService.validateGroup(groupId);

        Long membershipId = membershipService.findMembershipId(groupId, userId);

        groupService.lockGroup(groupId);
        boolean isLastMember = membershipService.isLastMemberWithLock(groupId);

        membershipService.leave(membershipId);

        postCommandService.softDeleteBySellerId(membershipId);
        chatroomCommandService.freezeByMembershipId(membershipId);

        if (isLastMember) {
            groupService.softDeleteByGroupId(groupId);
            chatroomCommandService.softDeleteByGroupId(groupId);
        }
    }

    @Transactional(readOnly = true)
    public GroupResponse.GroupSummaries getMyGroups(Long userId) {
        return groupService.findGroupSummariesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public GroupResponse.GroupProfile getGroupProfile(Long groupId, Long userId) {
        membershipService.validateMembership(groupId, userId);

        return groupService.findGroupProfile(groupId);
    }

    @Transactional(readOnly = true)
    public MembershipResponse.Profile getMyMembershipProfile(Long groupId, Long userId) {
        groupService.validateGroup(groupId);
        MembershipProfile profile = membershipService.findMembershipProfile(groupId, userId);

        return new MembershipResponse.Profile(
                profile.membershipId(),
                profile.nickname()
        );
    }

    @Transactional
    public String changeNickname(Long groupId, Long userId, String newNickname) {
        return membershipService.changeNickname(groupId, userId, newNickname);
    }
}
