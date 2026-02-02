package com.umc.devine.domain.image.repository;

import com.umc.devine.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByImageUrl(String imageUrl);

    @Query("SELECT i FROM Image i " +
            "WHERE i.imageType = com.umc.devine.domain.image.enums.ImageType.PROJECT " +
            "AND i.createdAt < :threshold " +
            "AND i.uploaded = true " +
            "AND i.imageUrl NOT IN (SELECT pi.image FROM ProjectImage pi)")
    List<Image> findOrphanProjectImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT i FROM Image i " +
            "WHERE i.imageType = com.umc.devine.domain.image.enums.ImageType.PROFILE " +
            "AND i.createdAt < :threshold " +
            "AND i.uploaded = true " +
            "AND i.imageUrl NOT IN (SELECT m.image FROM Member m WHERE m.image IS NOT NULL)")
    List<Image> findOrphanProfileImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT i FROM Image i " +
            "WHERE i.imageType = com.umc.devine.domain.image.enums.ImageType.EDITOR " +
            "AND i.createdAt < :threshold " +
            "AND i.uploaded = true " +
            "AND NOT EXISTS (SELECT 1 FROM Project p WHERE p.content LIKE CONCAT('%', i.imageUrl, '%'))")
    List<Image> findOrphanEditorImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT i FROM Image i " +
            "WHERE i.uploaded = false " +
            "AND i.createdAt < :threshold")
    List<Image> findUnconfirmedImages(@Param("threshold") LocalDateTime threshold);
}
