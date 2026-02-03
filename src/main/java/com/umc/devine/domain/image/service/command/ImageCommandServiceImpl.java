package com.umc.devine.domain.image.service.command;

import com.umc.devine.domain.image.converter.ImageConverter;
import com.umc.devine.domain.image.dto.ImageReqDTO;
import com.umc.devine.domain.image.dto.ImageResDTO;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorCode;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.infra.s3.S3Service;
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
    public ImageResDTO.PresignedUrlRes createPresignedUrl(Long memberId, ImageReqDTO.PresignedUrlReq request) {
        log.debug("[ImageService] Presigned URL 생성 요청 - memberId: {}, imageType: {}, fileName: {}",
                memberId, request.imageType(), request.fileName());

        s3Service.validateExtension(request.fileName());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        String s3Key = buildS3Key(request, memberId);
        String imageUrl = s3Service.buildImageUrl(s3Key);
        String contentType = s3Service.guessContentType(request.fileName());

        Image image = ImageConverter.toImage(request.imageType(), imageUrl, s3Key, member);
        Image savedImage = imageRepository.save(image);

        PresignedPutObjectRequest presigned = s3Service.generatePresignedPutUrl(s3Key, contentType);

        log.debug("[ImageService] Presigned URL 생성 완료 - imageId: {}, imageType: {}",
                savedImage.getId(), request.imageType());

        return ImageConverter.toPresignedUrlRes(savedImage, presigned.url().toString());
    }

    @Override
    public void confirmUpload(Long memberId, Long imageId) {
        log.debug("[ImageService] 업로드 확인 요청 - imageId: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));

        validateImageOwner(image, memberId);

        image.confirmUpload();

        log.debug("[ImageService] 업로드 확인 완료 - imageId: {}", imageId);
    }

    @Override
    public void deleteImage(Long memberId, Long imageId) {
        log.debug("[ImageService] 이미지 삭제 요청 - imageId: {}", imageId);

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));

        validateImageOwner(image, memberId);

        s3Service.deleteObject(image.getS3Key());
        imageRepository.delete(image);

        log.debug("[ImageService] 이미지 삭제 완료 - imageId: {}", imageId);
    }

    private void validateImageOwner(Image image, Long memberId) {
        if (!image.getUploader().getId().equals(memberId)) {
            throw new ImageException(ImageErrorCode.IMAGE_ACCESS_DENIED);
        }
    }

    private String buildS3Key(ImageReqDTO.PresignedUrlReq request, Long memberId) {
        return switch (request.imageType()) {
            case PROFILE -> s3Service.buildProfileKey(memberId, request.fileName());
            case PROJECT -> s3Service.buildProjectKey(request.fileName());
            case EDITOR -> s3Service.buildEditorKey(request.fileName());
        };
    }
}
