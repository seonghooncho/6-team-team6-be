package ktb.billage.domain.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByGroupIdAndUserIdAndDeletedAtIsNull(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndDeletedAtIsNull(Long groupId, Long userId);

    Optional<Membership> findByIdAndDeletedAtIsNull(Long membershipId);
    List<Membership> findAllByIdIn(List<Long> membershipIds);

    List<Membership> findAllByUserIdAndDeletedAtIsNull(Long userId);

    long countByGroupIdAndDeletedAtIsNull(Long groupId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m.id from Membership m where m.groupId = :groupId and m.deletedAt is null")
    List<Long> findIdsByGroupIdForUpdate(@Param("groupId") Long groupId);
}
