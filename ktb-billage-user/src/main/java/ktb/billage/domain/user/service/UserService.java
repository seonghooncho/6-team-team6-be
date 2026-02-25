package ktb.billage.domain.user.service;

import ktb.billage.contract.user.NicknameGenerator;
import ktb.billage.contract.user.PasswordEncoder;
import ktb.billage.domain.user.User;
import ktb.billage.domain.user.UserPushToken;
import ktb.billage.domain.user.UserPushTokenRepository;
import ktb.billage.domain.user.UserRepository;
import ktb.billage.domain.user.dto.UserResponse;
import ktb.billage.common.exception.AuthException;
import ktb.billage.common.exception.UserException;
import ktb.billage.common.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ktb.billage.common.exception.ExceptionCode.AUTHENTICATION_FAILED;
import static ktb.billage.common.exception.ExceptionCode.DUPLICATE_LOGIN_ID;
import static ktb.billage.common.exception.ExceptionCode.PUSH_TOKEN_NOT_FOUND;
import static ktb.billage.common.exception.ExceptionCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    private final UserRepository userRepository;
    private final UserPushTokenRepository userPushTokenRepository;

    public User findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new AuthException(AUTHENTICATION_FAILED));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserException(USER_NOT_FOUND));
    }

    public UserResponse.UserProfile findUserProfile(Long userId) {
        User user = findById(userId);
        String avatarUrl = imageService.resolveUrl(user.getAvatarUrl());
        return new UserResponse.UserProfile(userId, avatarUrl);
    }

    @Transactional
    public UserResponse.Id join(String loginId, String password) {
        validateDuplicateLoginId(loginId);

        String encodedPassword = passwordEncoder.encode(password);

        User user = userRepository.save(new User(loginId, encodedPassword));
        return new UserResponse.Id(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse.MyProfile getMyProfile(Long userId) {
        User user = findById(userId);

        String avatarUrl = imageService.resolveUrl(user.getAvatarUrl());
        return new UserResponse.MyProfile(user.getLoginId(), avatarUrl);
    }

    @Transactional(readOnly = true)
    public List<UserResponse.UserProfile> findUserProfiles(List<Long> userIds) {
        return userRepository.findAllByIdInAndDeletedAtIsNull(userIds).stream()
                .map(user -> new UserResponse.UserProfile(
                        user.getId(),
                        imageService.resolveUrl(user.getAvatarUrl())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, UserResponse.UserProfile> findUserProfilesMap(List<Long> userIds) {
        return findUserProfiles(userIds).stream()
                .collect(Collectors.toMap(
                        UserResponse.UserProfile::userId,
                        Function.identity(),
                        (existing, ignored) -> existing
                ));
    }

    @Transactional
    public void changeWebPushSetting(Long userId, boolean enabled) {
        User user = findById(userId);
        user.changeWebPushSetting(enabled);
    }

    @Transactional(readOnly = true)
    public UserResponse.WebPushEnabled getWebPushSetting(Long userId) {
        Boolean enabled = findById(userId).getWebPushEnabled();
        return new UserResponse.WebPushEnabled(enabled);
    }

    @Transactional
    public void upsertPushToken(Long userId, UserPushToken.PushPlatform platform, String deviceId, String token) {
        User user = findById(userId);
        Instant now = Instant.now();

        var currentDeviceTokenOpt = userPushTokenRepository.findByUserIdAndPlatformAndDeviceId(userId, platform, deviceId);
        var sameTokenOwnerOpt = userPushTokenRepository.findByFcmToken(token);

        if (currentDeviceTokenOpt.isPresent()) {
            UserPushToken currentDeviceToken = currentDeviceTokenOpt.get();
            if (sameTokenOwnerOpt.isPresent() && !sameTokenOwnerOpt.get().getId().equals(currentDeviceToken.getId())) {
                userPushTokenRepository.delete(sameTokenOwnerOpt.get());
            }
            currentDeviceToken.updateToken(token, now);
            return;
        }

        if (sameTokenOwnerOpt.isPresent()) {
            UserPushToken tokenOwner = sameTokenOwnerOpt.get();
            tokenOwner.rebind(user, platform, deviceId, token, now);
            return;
        }

        userPushTokenRepository.save(new UserPushToken(user, platform, deviceId, token, now));
    }
    
    @Transactional
    public void deletePushToken(Long userId, String deviceId) {
        UserPushToken pushToken = userPushTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new UserException(PUSH_TOKEN_NOT_FOUND));
        userPushTokenRepository.delete(pushToken);
    }

    private void validateDuplicateLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new UserException(DUPLICATE_LOGIN_ID);
        }
    }
}
