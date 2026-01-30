package com.umc.devine.domain.image.scheduler;

import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final S3Service s3Service;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanImages() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        List<Image> orphans = new ArrayList<>();
        orphans.addAll(imageRepository.findUnconfirmedImages(threshold));
        orphans.addAll(imageRepository.findOrphanProjectImages(threshold));

        if (orphans.isEmpty()) {
            log.info("[ImageCleanup] 정리할 고아 이미지 없음");
            return;
        }

        log.info("[ImageCleanup] 고아 이미지 정리 시작 - {}건 (미확인 업로드 + 미사용 프로젝트 이미지)", orphans.size());

        int deleted = 0;
        for (Image image : orphans) {
            try {
                s3Service.deleteObject(image.getS3Key());
                imageRepository.delete(image);
                deleted++;
            } catch (Exception e) {
                log.warn("[ImageCleanup] 이미지 삭제 실패 - imageId: {}, s3Key: {}",
                        image.getId(), image.getS3Key(), e);
            }
        }

        log.info("[ImageCleanup] 고아 이미지 정리 완료 - 총 {}건 중 {}건 삭제", orphans.size(), deleted);
    }
}
