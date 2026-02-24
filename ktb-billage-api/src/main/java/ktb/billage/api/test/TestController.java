package ktb.billage.api.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ktb.billage.domain.membership.service.MembershipService;
import ktb.billage.web.common.annotation.AuthenticatedId;
import ktb.billage.websocket.application.port.ChatPushNotifier;
import ktb.billage.websocket.dto.ChatSendAckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Profile("dev")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "테스트 API")
public class TestController {
    private final MembershipService membershipService;
    private final ChatPushNotifier chatPushNotifier;

    @Operation(
            summary = "내 계정 웹 푸시 테스트 전송",
            description = "인증된 사용자에게 테스트 웹 푸시를 전송합니다. dev 프로필에서만 활성화됩니다. 최소 하나의 그룹에 속해있는 사용자 아이디로 진행해야합니다.",
            security = {@SecurityRequirement(name = "Bearer Auth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전송 트리거 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/push/me")
    public ResponseEntity<Void> sendPushToMe(@AuthenticatedId Long userId) {
        ChatSendAckResponse ack = new ChatSendAckResponse(
                9999L,
                membershipService.findMembershipIds(userId).getFirst(),
                "test",
                "목 데이터 웹 푸시 테스트",
                Instant.now()
        );

        chatPushNotifier.sendPush(userId, ack);
        return ResponseEntity.noContent().build();
    }
}
