package ktb.billage.application.post.listener;

import ktb.billage.application.post.event.PostUpsertPayload;

public interface AiSyncPort {
    void syncCreated(PostUpsertPayload payload);

    void syncUpdated(PostUpsertPayload payload);

    void syncDeleted(Long postId);
}
