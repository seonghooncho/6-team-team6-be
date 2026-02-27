package ktb.billage.domain.chat.dto;

public record PartnerProfile(
        Long partnerId,
        String nickname,
        Boolean isLeftGroup
) {
}
