package com.umc.devine.domain.image.repository;

import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    Optional<Image> findByImageUrl(String imageUrl);

    /** Hard Delete 배치에서 회원 행을 지우기 전에 호출한다. 이미지 자산은 그대로 두고 업로더 참조만 끊는다. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Image i SET i.uploader = null WHERE i.uploader = :member")
    int bulkNullifyUploader(@Param("member") Member member);

    @Query("SELECT i FROM Image i " +
            "WHERE i.imageType = com.umc.devine.domain.image.enums.ImageType.PROJECT " +
            "AND i.createdAt < :threshold " +
            "AND i.uploaded = true " +
            "AND NOT EXISTS (SELECT 1 FROM ProjectImage pi JOIN pi.project p " +
            "WHERE pi.image = i AND p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED)")
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
            "AND NOT EXISTS (SELECT 1 FROM Project p WHERE p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND p.content LIKE CONCAT('%', i.imageUrl, '%'))")
    List<Image> findOrphanEditorImages(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT i FROM Image i " +
            "WHERE i.uploaded = false " +
            "AND i.createdAt < :threshold")
    List<Image> findUnconfirmedImages(@Param("threshold") LocalDateTime threshold);
}
