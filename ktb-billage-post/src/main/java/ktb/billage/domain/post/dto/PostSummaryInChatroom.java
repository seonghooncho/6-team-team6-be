package ktb.billage.domain.post.dto;

import java.math.BigDecimal;

public record PostSummaryInChatroom(
        String title,
        String firstImageUrl,
        BigDecimal rentalFee,
        String feeUnit,
        String rentalStatus,
        Boolean isDeleted
) {
}
