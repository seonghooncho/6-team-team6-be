package ktb.billage.application.post.listener;

import ktb.billage.application.post.event.PostCreateEvent;
import ktb.billage.application.post.event.PostDeleteEvent;
import ktb.billage.application.post.event.PostUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostAiSyncEventListener {
    private final AiSyncPort aiSyncPort;

    @Async("aiSyncAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostCreateEvent event) {
        callSafely("create", event.payload().postId(), () -> aiSyncPort.syncCreated(event.payload()));
    }

    @Async("aiSyncAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostUpdateEvent event) {
        callSafely("update", event.payload().postId(), () -> aiSyncPort.syncUpdated(event.payload()));
    }

    @Async("aiSyncAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostDeleteEvent event) {
        callSafely("delete", event.postId(), () -> aiSyncPort.syncDeleted(event.postId()));
    }

    private void callSafely(String action, Long postId, Runnable call) {
        try {
            call.run();
        } catch (Exception e) {
            log.error("Failed to sync post to AI server. action={}, postId={}, message={}", action, postId, e.getMessage(), e);
        }
    }
}
