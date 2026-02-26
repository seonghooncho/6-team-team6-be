package ktb.billage.application.post.event;

import java.math.BigDecimal;

public record PostUpsertPayload(
        Long membershipId,
        Long groupId,
        Long postId,
        String imageUrl,
        String title,
        BigDecimal price,
        String feeUnit
) {
}
