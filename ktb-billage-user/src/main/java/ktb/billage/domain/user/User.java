package ktb.billage.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ktb.billage.contract.user.PasswordEncoder;
import ktb.billage.common.entity.BaseEntity;
import ktb.billage.common.exception.AuthException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static ktb.billage.common.exception.ExceptionCode.AUTHENTICATION_FAILED;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    private static final String DEFAULT_AVATAR_URL = "images/default-avatar.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(name = "avatar_url", nullable = false)
    private String avatarUrl;

    @Column(name = "web_push_enabled", nullable = false)
    private Boolean webPushEnabled;

    public User(String loginId, String encodedPassword) {
        this.loginId = loginId;
        this.password = encodedPassword;
        this.avatarUrl = DEFAULT_AVATAR_URL;
        this.webPushEnabled = false;
    }

    public void verifyPassword(PasswordEncoder passwordEncoder, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, this.password)) {
            throw new AuthException(AUTHENTICATION_FAILED);
        }
    }

    public void changeWebPushSetting(boolean enabled) {
        this.webPushEnabled = enabled;
    }
}
