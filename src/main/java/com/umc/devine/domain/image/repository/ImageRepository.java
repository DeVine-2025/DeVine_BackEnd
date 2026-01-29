package com.umc.devine.domain.image.repository;

import com.umc.devine.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("SELECT i FROM Image i " +
            "WHERE i.imageType = com.umc.devine.domain.image.enums.ImageType.PROJECT " +
            "AND i.createdAt < :threshold " +
            "AND i.uploaded = true " +
            "AND i.imageUrl NOT IN (SELECT pi.image FROM ProjectImage pi)")
    List<Image> findOrphanProjectImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT i FROM Image i " +
            "WHERE i.uploaded = false " +
            "AND i.createdAt < :threshold")
    List<Image> findUnconfirmedImages(@Param("threshold") LocalDateTime threshold);
}
