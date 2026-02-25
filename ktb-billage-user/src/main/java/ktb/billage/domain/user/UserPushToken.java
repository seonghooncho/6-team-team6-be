package ktb.billage.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ktb.billage.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(
        name = "user_push_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_push_token_user_platform_device", columnNames = {"user_id", "platform", "device_id"}),
                @UniqueConstraint(name = "uk_user_push_token_fcm_token", columnNames = {"fcm_token"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public UserPushToken(User user, PushPlatform platform, String deviceId, String fcmToken, Instant lastSeenAt) {
        this.user = user;
        this.platform = platform;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
        this.lastSeenAt = lastSeenAt;
    }

    public void updateToken(String fcmToken, Instant lastSeenAt) {
        this.fcmToken = fcmToken;
        this.lastSeenAt = lastSeenAt;
    }

    public void touch(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void rebind(User user, PushPlatform platform, String deviceId, String fcmToken, Instant lastSeenAt) {
        this.user = user;
        this.platform = platform;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
        this.lastSeenAt = lastSeenAt;
    }

    public enum PushPlatform {
        WEB,
        IOS,
        AOS
    }
}
