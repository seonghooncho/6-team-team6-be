package ktb.billage.domain.chat;

import ktb.billage.domain.chat.dto.PartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ktb.billage.domain.chat.dto.ChatResponse;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
    long countByPostIdAndLastMessageIdIsNotNullAndDeletedAtIsNull(Long postId);

    Optional<Chatroom> findFirstByPostIdAndBuyerId(Long postId, Long buyerId);

    Optional<Chatroom> findByIdAndLastMessageIdIsNotNull(Long memebershipId);

    @Query("""
            select case when count(c) > 0 then true else false end
            from Chatroom c
            join Post p on c.postId = p.id
            where c.id = :chatroomId
              and c.deletedAt is null
              and (c.buyerId = :membershipId or p.sellerId = :membershipId)
    """)
    boolean existsByIdAndBuyerIdOrSellerId(@Param("chatroomId") Long chatroomId,
                                           @Param("membershipId") Long membershipId);

    @Query("""
        select case when count(c) > 0 then true else false end
        from Chatroom c
        join Post p on c.postId = p.id
        where c.id = :chatroomId
          and c.deletedAt is null
          and (c.buyerId in :membershipIds or p.sellerId in :membershipIds)
    """)
    boolean existsByIdAndBuyerIdsOrSellerIds(@Param("chatroomId") Long chatroomId,
                                             @Param("membershipIds") List<Long> membershipIds);

    boolean existsByPostIdAndBuyerId(Long postId, Long buyerId);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomSummaryCore(
                c.id,
                c.buyerId,
                c.postId,
                m.id,
                m.createdAt,
                m.content,
                c.sellerLastReadMessageId,
                c.buyerLastReadMessageId
            )
            from Chatroom c
            join Post p on c.postId = p.id
            join ChatMessage m on m.id = c.lastMessageId
            where c.postId = :postId
              and c.deletedAt is null
              and c.lastMessageId is not null
            order by m.createdAt desc, c.id desc
            """)
    List<ChatResponse.ChatroomSummaryCore> findTop21SummaryCoresByMyPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomSummaryCore(
                c.id,
                c.buyerId,
                c.postId,
                m.id,
                m.createdAt,
                m.content,
                c.sellerLastReadMessageId,
                c.buyerLastReadMessageId
            )
            from Chatroom c
            join Post p on c.postId = p.id
            join ChatMessage m on m.id = c.lastMessageId
            where c.postId = :postId
              and c.deletedAt is null
              and c.lastMessageId is not null
              and (m.createdAt < :time or (m.createdAt = :time and c.id < :id))
            order by m.createdAt desc, c.id desc
            """)
    List<ChatResponse.ChatroomSummaryCore> findNextSummaryCorePageByMyPostId(@Param("postId") Long postId,
                                                                             @Param("time") Instant time,
                                                                             @Param("id") Long id,
                                                                             Pageable pageable);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomSummaryCore(
                c.id,
                case when c.buyerId in :membershipIds then p.sellerId else c.buyerId end,
                c.postId,
                m.id,
                m.createdAt,
                m.content,
                c.sellerLastReadMessageId,
                c.buyerLastReadMessageId
            )
            from Chatroom c
            join ChatMessage m on m.id = c.lastMessageId
            join Post p on c.postId = p.id
            where c.deletedAt is null
              and c.lastMessageId is not null
              and (c.buyerId in :membershipIds or p.sellerId in :membershipIds)
            order by m.createdAt desc, c.id desc
            """)
    List<ChatResponse.ChatroomSummaryCore> findTop21SummaryCoresByMembershipIds(@Param("membershipIds") List<Long> membershipIds, Pageable pageable);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomSummaryCore(
                c.id,
                case when c.buyerId in :membershipIds then p.sellerId else c.buyerId end,
                c.postId,
                m.id,
                m.createdAt,
                m.content,
                c.sellerLastReadMessageId,
                c.buyerLastReadMessageId
            )
            from Chatroom c
            join ChatMessage m on m.id = c.lastMessageId
            join Post p on c.postId = p.id
            where  c.deletedAt is null
              and c.lastMessageId is not null
              and (c.buyerId in :membershipIds or p.sellerId in :membershipIds)
              and (m.createdAt < :time or (m.createdAt = :time and c.id < :id))
            order by m.createdAt desc, c.id desc
            """)
    List<ChatResponse.ChatroomSummaryCore> findNextSummaryCorePageByMembershipIds(@Param("membershipIds") List<Long> membershipIds,
                                                                                  @Param("time")Instant time,
                                                                                  @Param("id")Long id,
                                                                                  Pageable pageable);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomMembershipDto(
                c.id,
                case when c.buyerId in :membershipIds then c.buyerId else p.sellerId end,
                case when c.buyerId in :membershipIds then false else true end
            )
            from Chatroom c
            join Post p on c.postId = p.id
            where c.deletedAt is null
                and c.lastMessageId is not null
                and (c.buyerId in :membershipIds
                    or p.sellerId in :membershipIds)
    """)
    List<ChatResponse.ChatroomMembershipDto> findAllByParticipatingIds(@Param("membershipIds") List<Long> membershipIds);

    @Query("""
            select new ktb.billage.domain.chat.dto.ChatResponse$ChatroomMembershipDto(
                c.id,
                case when c.buyerId in :membershipIds then c.buyerId else p.sellerId end,
                case when c.buyerId in :membershipIds then false else true end
            )
            from Chatroom c
            join Post p on c.postId = p.id
            where c.id = :chatroomId
              and c.deletedAt is null
              and (c.buyerId in :membershipIds or p.sellerId in :membershipIds)
    """)
    Optional<ChatResponse.ChatroomMembershipDto> findParticipationByChatroomIdAndMembershipIds(
            @Param("chatroomId") Long chatroomId,
            @Param("membershipIds") List<Long> membershipIds
    );

    @Query("""
                select new ktb.billage.domain.chat.dto.PartnerProfile(
                    m.id,
                    m.nickname,
                    case when m.deletedAt is not null then true else false end
                   )
            from Chatroom c
            join Post p on c.postId = p.id
            join Membership m on m.id = (case when c.buyerId = :myId then p.sellerId else c.buyerId end)
            where c.id = :chatroomId
                and c.deletedAt is null
            """)
    PartnerProfile findPartnerProfile(@Param("chatroomId") Long chatroomId, @Param("myId") Long myId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Chatroom c
           set c.roomStatus = ktb.billage.domain.chat.RoomStatus.FROZEN
         where c.deletedAt is null
           and (
                c.buyerId = :membershipId
                or exists (
                    select 1
                    from Post p
                    where p.id = c.postId
                      and p.sellerId = :membershipId
                )
           )
    """)
    int freezeByMembershipId(@Param("membershipId") Long membershipId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Chatroom c
           set c.deletedAt = :deletedAt
         where c.deletedAt is null
           and c.buyerId in (
                select m.id
                from Membership m
                where m.groupId = :groupId
           )
    """)
    int softDeleteByGroupId(@Param("groupId") Long groupId,
                            @Param("deletedAt") java.time.Instant deletedAt);
}
