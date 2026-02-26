package ktb.billage.application.post;

import ktb.billage.application.post.event.PostCreateEvent;
import ktb.billage.application.post.event.PostDeleteEvent;
import ktb.billage.application.post.event.PostUpdateEvent;
import ktb.billage.application.post.event.PostUpsertPayload;
import ktb.billage.common.image.ImageService;
import ktb.billage.domain.group.dto.GroupResponse;
import ktb.billage.domain.membership.dto.MembershipProfile;
import ktb.billage.domain.post.RentalStatus;
import ktb.billage.domain.post.dto.PostRequest;
import ktb.billage.domain.post.dto.PostResponse;
import ktb.billage.domain.post.service.PostCommandService;
import ktb.billage.domain.post.service.PostQueryService;
import ktb.billage.domain.chat.service.ChatroomQueryService;
import ktb.billage.domain.group.service.GroupService;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.domain.user.dto.UserResponse;
import ktb.billage.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFacade {
    private final PostCommandService postCommandService;
    private final PostQueryService postQueryService;
    private final ImageService imageService;
    private final GroupService groupService;
    private final MembershipService membershipService;
    private final ChatroomQueryService chatroomQueryService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostResponse.Id create(Long groupId, Long userId, PostRequest.Create request) {
        GroupResponse.GroupProfile groupProfile = groupService.findGroupProfile(groupId);
        Long membershipId = membershipService.findMembershipId(groupId, userId);

        PostResponse.Id response = postCommandService.create(
                membershipId,
                request.title(),
                request.content(),
                request.imageUrls(),
                request.rentalFee(),
                request.feeUnit()
        );

        var payload = new PostUpsertPayload(
                membershipId,
                groupProfile.groupId(),
                response.postId(),
                request.imageUrls().getFirst(),
                request.title(),
                request.rentalFee(),
                request.feeUnit().name()
        );
        eventPublisher.publishEvent(new PostCreateEvent(payload));
        return response;
    }

    @Transactional
    public PostResponse.Id update(Long groupId, Long postId, Long userId, PostRequest.Update request) {
        GroupResponse.GroupProfile groupProfile = groupService.findGroupProfile(groupId);
        Long membershipId = membershipService.findMembershipId(groupId, userId);
        PostResponse.Id response = postCommandService.update(
                postId,
                membershipId,
                request.title(),
                request.content(),
                request.imageUrls(),
                request.rentalFee(),
                request.feeUnit()
        );

        var payload = new PostUpsertPayload(
                membershipId,
                groupProfile.groupId(),
                response.postId(),
                request.imageUrls().getFirst().imageUrl(),
                request.title(),
                request.rentalFee(),
                request.feeUnit().name()
        );
        eventPublisher.publishEvent(new PostUpdateEvent(payload));
        return response;
    }

    @Transactional
    public PostResponse.ChangedStatus changeRentalStatus(Long groupId, Long postId, Long userId, RentalStatus rentalStatus) {
        groupService.validateGroup(groupId);
        Long membershipId = membershipService.findMembershipId(groupId, userId);
        return postCommandService.changeRentalStatus(postId, membershipId, rentalStatus);
    }

    @Transactional
    public void delete(Long groupId, Long postId, Long userId) {
        groupService.validateGroup(groupId);
        Long membershipId = membershipService.findMembershipId(groupId, userId);
        postCommandService.delete(postId, membershipId);

        eventPublisher.publishEvent(new PostDeleteEvent(postId));
    }

    public PostResponse.Summaries getPostsByCursor(Long groupId, Long userId, String cursor) {
        groupService.validateGroup(groupId);
        membershipService.validateMembership(groupId, userId);
        PostResponse.Summaries summaries = postQueryService.getPostsByCursor(groupId, cursor);
        var resolvedSummaries = summaries.summaries().stream()
                .map(summary -> new PostResponse.Summary(
                        summary.postId(),
                        summary.postTitle(),
                        summary.postImageId(),
                        getImagePresignedUrl(summary.postFirstImageUrl()),
                        summary.rentalFee(),
                        summary.feeUnit(),
                        summary.rentalStatus(),
                        summary.updatedAt()
                ))
                .toList();
        return new PostResponse.Summaries(resolvedSummaries, summaries.nextCursor(), summaries.hasNextPage());
    }

    @Transactional(readOnly = true)
    public PostResponse.Summaries getPostsByKeywordAndCursor(Long groupId, Long userId, String keyword, String cursor) {
        groupService.validateGroup(groupId);
        membershipService.validateMembership(groupId, userId);
        PostResponse.Summaries summaries = postQueryService.getPostsByKeywordAndCursor(groupId, keyword, cursor);
        var resolvedSummaries = summaries.summaries().stream()
                .map(summary -> new PostResponse.Summary(
                        summary.postId(),
                        summary.postTitle(),
                        summary.postImageId(),
                        getImagePresignedUrl(summary.postFirstImageUrl()),
                        summary.rentalFee(),
                        summary.feeUnit(),
                        summary.rentalStatus(),
                        summary.updatedAt()
                ))
                .toList();
        return new PostResponse.Summaries(resolvedSummaries, summaries.nextCursor(), summaries.hasNextPage());
    }

    public PostResponse.Detail getPostDetail(Long groupId, Long postId, Long userId) {
        groupService.validateGroup(groupId);
        Long membershipId = membershipService.findMembershipId(groupId, userId);
        PostResponse.DetailCore core = postQueryService.getPostDetailCore(postId);
        boolean isSeller = core.sellerId().equals(membershipId);

        MembershipProfile sellerMembershipProfile = membershipService.findMembershipProfile(core.sellerId());

        Long sellerUserId = membershipService.findUserIdByMembershipId(core.sellerId());
        UserResponse.UserProfile sellerUserProfile = userService.findUserProfile(sellerUserId);

        Long chatroomId;
        Long activeChatroomCount;

        if (isSeller) {
            chatroomId = null;
            activeChatroomCount = chatroomQueryService.countChatroomsByPostId(postId);
        } else {
            chatroomId = chatroomQueryService.findChatroomIdByPostIdAndBuyerId(postId, membershipId);
            activeChatroomCount = null;
        }

        PostResponse.ImageUrls resolvedImageUrls = new PostResponse.ImageUrls(
                core.imageUrls().imageInfos().stream()
                        .map(info -> new PostResponse.ImageInfo(
                                info.postImageId(),
                                getImagePresignedUrl(info.imageUrl())
                        ))
                        .toList()
        );

        return new PostResponse.Detail(
                core.title(),
                core.content(),
                resolvedImageUrls,
                core.sellerId(),
                sellerMembershipProfile.nickname(),
                sellerUserProfile.avatarImageUrl(),
                core.rentalFee(),
                core.feeUnit(),
                core.rentalStatus(),
                core.updatedAt(),
                isSeller,
                chatroomId,
                activeChatroomCount
        );
    }

    public PostResponse.MySummaries getMyPostsByCursor(Long userId, String cursor) {
        List<Long> membershipIds = membershipService.findMembershipIds(userId);

        return postQueryService.getMyPostsByCursor(membershipIds, cursor);
    }

    private String getImagePresignedUrl(String imageKey) {
        return imageService.resolveUrl(imageKey);
    }
}
