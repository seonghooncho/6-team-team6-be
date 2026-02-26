package ktb.billage.application.ai;

import ktb.billage.common.exception.InternalException;
import ktb.billage.domain.post.dto.PostRequest;
import ktb.billage.domain.post.dto.PostResponse;
import ktb.billage.domain.post.service.AiPostDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static ktb.billage.common.exception.ExceptionCode.IMAGE_HANDLING_FAILED;

@Service
@RequiredArgsConstructor
public class AiFacade {
    private final AiPostDraftService aiPostDraftService;

    public PostResponse.PostDraft makePostDraftByAi(List<MultipartFile> images) {
        return aiPostDraftService.makePostDraft(toImagesDto(images));
    }

    private List<PostRequest.ImageComponent> toImagesDto(List<MultipartFile> images) {
        return images.stream()
                .map(image -> {
                    try {
                        return new PostRequest.ImageComponent(
                                image.getBytes(),
                                image.getContentType(),
                                image.getSize()
                        );
                    } catch (IOException e) {
                        throw new InternalException(IMAGE_HANDLING_FAILED);
                    }
                })
                .toList();
    }
}
