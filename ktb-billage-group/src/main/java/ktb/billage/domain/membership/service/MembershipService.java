package ktb.billage.domain.membership.service;

import ktb.billage.common.exception.GroupException;
import ktb.billage.domain.membership.Membership;
import ktb.billage.domain.membership.MembershipRepository;
import ktb.billage.domain.membership.dto.MembershipProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ktb.billage.common.exception.ExceptionCode.ALREADY_GROUP_MEMBER;
import static ktb.billage.common.exception.ExceptionCode.GROUP_CAPACITY_EXCEEDED;
import static ktb.billage.common.exception.ExceptionCode.USER_GROUP_LIMIT_EXCEEDED;
import static ktb.billage.common.exception.ExceptionCode.NOT_GROUP_MEMBER;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private static final long MAX_GROUP_MEMBER_COUNT = 3000;
    private static final long MAX_GROUPS_PER_USER = 30;

    private final MembershipRepository membershipRepository;

    public Long join(Long groupId, Long userId, String nickname) {
        validateNotMember(groupId, userId);

        Membership membership = membershipRepository.save(new Membership(groupId, userId, nickname));
        return membership.getId();
    }

    public Long findMembershipId(Long groupId, Long userId) {
        return findMembership(groupId, userId).getId();
    }

    public void validateMembership(Long groupId, Long userId) {
        if (!membershipRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)) {
            throw new GroupException(NOT_GROUP_MEMBER);
        }
    }

    public void validateNotMember(Long groupId, Long userId) {
        if (membershipRepository.existsByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)) {
            throw new GroupException(ALREADY_GROUP_MEMBER);
        }
    }

    public Long findUserIdByMembershipId(Long membershipId) {
        return findMembershipIncludingWithdraw(membershipId).getUserId();
    }

    public Long findGroupIdByMembershipId(Long membershipId) {
        return findMembershipIncludingWithdraw(membershipId).getGroupId();
    }

    public void validateMembershipOwner(Long userId, Long membershipId) {
        if (!findMembershipIncludingWithdraw(membershipId).isOwnedBy(userId)) {
            throw new GroupException(NOT_GROUP_MEMBER);
        }
    }

    public void validateNotLeave(Long membershipId) {
        findMembership(membershipId);
    }

    public List<Long> findMembershipIds(Long userId) {
         return membershipRepository.findAllByUserIdAndDeletedAtIsNull(userId).stream()
                 .map(Membership::getId)
                 .toList();
    }

    public void validateGroupCapacity(Long groupId) {
        long count = membershipRepository.countByGroupIdAndDeletedAtIsNull(groupId);
        if (count >= MAX_GROUP_MEMBER_COUNT) {
            throw new GroupException(GROUP_CAPACITY_EXCEEDED);
        }
    }

    public void validateUserGroupLimit(Long userId) {
        long count = membershipRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (count >= MAX_GROUPS_PER_USER) {
            throw new GroupException(USER_GROUP_LIMIT_EXCEEDED);
        }
    }

    public boolean isLastMemberWithLock(Long groupId) {
        long count = membershipRepository.findIdsByGroupIdForUpdate(groupId).size();
        return count == 1;
    }

    public void leave(Long membershipId) {
        Membership membership = membershipRepository.findByIdAndDeletedAtIsNull(membershipId)
                .orElseThrow(() -> new GroupException(NOT_GROUP_MEMBER));

        membership.delete(Instant.now());
    }

    public Map<Long, MembershipProfile> findMembershipProfiles(List<Long> membershipIds) {
        return membershipRepository.findAllByIdIn(membershipIds).stream()
                .map(membership -> new MembershipProfile(
                        membership.getId(),
                        membership.getGroupId(),
                        membership.getUserId(),
                        membership.getNickname()
                ))
                .collect(Collectors.toMap(
                        MembershipProfile::membershipId,
                        Function.identity(),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
    }

    public MembershipProfile findMembershipProfile(Long membershipId) {
        Membership membership = findMembership(membershipId);
        return new MembershipProfile(
                membership.getId(),
                membership.getGroupId(),
                membership.getUserId(),
                membership.getNickname()
        );
    }

    public MembershipProfile findMembershipProfile(Long groupId, Long userId) {
        Membership membership = findMembership(groupId, userId);
        return findMembershipProfile(membership.getId());
    }

    public String changeNickname(Long groupId, Long userId, String newNickname) {
        Membership membership = findMembership(groupId, userId);
        membership.changeNickname(newNickname);
        return newNickname;
    }

    private Membership findMembership(Long membershipId) {
        return membershipRepository.findByIdAndDeletedAtIsNull(membershipId)
                .orElseThrow(() -> new GroupException(NOT_GROUP_MEMBER));
    }

    private Membership findMembershipIncludingWithdraw(Long membershipId) {
        return membershipRepository.findById(membershipId)
                .orElseThrow(() -> new GroupException(NOT_GROUP_MEMBER));
    }

    private Membership findMembership(Long groupId, Long userId) {
        return membershipRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)
                .orElseThrow(() -> new GroupException(NOT_GROUP_MEMBER));
    }
}
