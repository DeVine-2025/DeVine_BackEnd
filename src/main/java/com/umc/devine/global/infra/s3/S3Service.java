package com.umc.devine.global.infra.s3;

import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public void validateExtension(String fileName) {
        String ext = extractExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ImageException(ImageErrorCode.UNSUPPORTED_FILE_EXTENSION);
        }
    }

    public PresignedPutObjectRequest generatePresignedPutUrl(String key, String contentType) {
        log.debug("[S3Service] Presigned URL 생성 요청 - key: {}, contentType: {}", key, contentType);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(PRESIGN_DURATION)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            log.debug("[S3Service] Presigned URL 생성 완료 - key: {}", key);

            return presigned;
        } catch (Exception e) {
            log.error("[S3Service] Presigned URL 생성 실패 - key: {}", key, e);
            throw new ImageException(ImageErrorCode.S3_PRESIGN_FAILED);
        }
    }

    public void deleteObject(String s3Key) {
        log.debug("[S3Service] S3 오브젝트 삭제 요청 - key: {}", s3Key);

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.debug("[S3Service] S3 오브젝트 삭제 완료 - key: {}", s3Key);
        } catch (Exception e) {
            log.error("[S3Service] S3 오브젝트 삭제 실패 - key: {}", s3Key, e);
            throw new ImageException(ImageErrorCode.S3_DELETE_FAILED);
        }
    }

    public String buildProfileKey(Long memberId, String fileName) {
        String ext = extractExtension(fileName);
        String key = "profiles/" + memberId + "/" + UUID.randomUUID() + "." + ext;
        log.debug("[S3Service] Profile key 생성 - {}", key);
        return key;
    }

    public String buildProjectKey(String fileName) {
        String ext = extractExtension(fileName);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String key = "projects/" + datePath + "/" + UUID.randomUUID() + "." + ext;
        log.debug("[S3Service] Project key 생성 - {}", key);
        return key;
    }

    public String buildEditorKey(String fileName) {
        String ext = extractExtension(fileName);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String key = "editor/" + datePath + "/" + UUID.randomUUID() + "." + ext;
        log.debug("[S3Service] Editor key 생성 - {}", key);
        return key;
    }

    public String buildImageUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    public String guessContentType(String fileName) {
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        log.debug("[S3Service] Content-Type 결정 - fileName: {}, contentType: {}", fileName, contentType);
        return contentType;
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }
}
