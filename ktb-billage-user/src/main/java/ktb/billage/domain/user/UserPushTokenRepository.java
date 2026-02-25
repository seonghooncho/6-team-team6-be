package ktb.billage.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    Optional<UserPushToken> findByUserIdAndPlatformAndDeviceId(Long userId, UserPushToken.PushPlatform platform, String deviceId);
    Optional<UserPushToken> findByFcmToken(String fcmToken);

    List<UserPushToken> findAllByUserId(Long userId);

    void deleteByFcmTokenIn(List<String> fcmTokens);
    
    @Query("""
        select up
        from UserPushToken up
        join User u on u = up.user
        where u.id = :userId
        and up.deviceId = :deviceId
        """)
    Optional<UserPushToken> findByUserIdAndDeviceId(@Param("userId") Long userId,
                                                    @Param("deviceId") String deviceId);
}
