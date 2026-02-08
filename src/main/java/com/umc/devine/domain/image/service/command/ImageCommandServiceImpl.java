package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.converter.ImageConverter;
import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.auth.ClerkPrincipal;
import com.umc.devine.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageCommandServiceImpl implements ImageCommandService {

    private final ImageRepository imageRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;

    @Override
    public ImageResDTO.PresignedUrlRes createPresignedUrl(ClerkPrincipal principal, ImageReqDTO.PresignedUrlReq request) {
        String clerkId = principal.getClerkId();
        Member member = memberRepository.findByClerkId(clerkId).orElse(null);

        log.debug("[ImageService] Presigned URL 생성 요청 - clerkId: {}, imageType: {}, fileName: {}",
                clerkId, request.imageType(), request.fileName());

        s3Service.validateExtension(request.fileName());

        String identifier = (member != null) ? member.getId().toString() : clerkId;
        String s3Key = buildS3Key(request, identifier);
        String imageUrl = s3Service.buildImageUrl(s3Key);
        String contentType = s3Service.guessContentType(request.fileName());

        Image image = ImageConverter.toImage(request.imageType(), imageUrl, s3Key, clerkId, member);
        Image savedImage = imageRepository.save(image);

        PresignedPutObjectRequest presigned = s3Service.generatePresignedPutUrl(s3Key, contentType);

        log.debug("[ImageService] Presigned URL 생성 완료 - imageId: {}, imageType: {}",
                savedImage.getId(), request.imageType());

        return ImageConverter.toPresignedUrlRes(savedImage, presigned.url().toString());
    }

    @Override
    public void confirmUpload(ClerkPrincipal principal, Long imageId) {
        log.debug("[ImageService] 업로드 확인 요청 - imageId: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));

        validateImageOwner(image, principal.getClerkId());

        image.confirmUpload();

        log.debug("[ImageService] 업로드 확인 완료 - imageId: {}", imageId);
    }

    @Override
    public void deleteImage(ClerkPrincipal principal, Long imageId) {
        log.debug("[ImageService] 이미지 삭제 요청 - imageId: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));

        validateImageOwner(image, principal.getClerkId());

        s3Service.deleteObject(image.getS3Key());
        imageRepository.delete(image);

        log.debug("[ImageService] 이미지 삭제 완료 - imageId: {}", imageId);
    }

    private void validateImageOwner(Image image, String clerkId) {
        // clerkId 기반 검증 (신규 이미지)
        if (clerkId.equals(image.getClerkId())) {
            return;
        }
        // uploader 기반 검증 (기존 이미지 하위 호환)
        if (image.getUploader() != null && clerkId.equals(image.getUploader().getClerkId())) {
            return;
        }
        throw new ImageException(ImageErrorCode.IMAGE_ACCESS_DENIED);
    }

    private String buildS3Key(ImageReqDTO.PresignedUrlReq request, String identifier) {
        return switch (request.imageType()) {
            case PROFILE -> s3Service.buildProfileKey(identifier, request.fileName());
            case PROJECT -> s3Service.buildProjectKey(request.fileName());
            case EDITOR -> s3Service.buildEditorKey(request.fileName());
        };
    }
}
