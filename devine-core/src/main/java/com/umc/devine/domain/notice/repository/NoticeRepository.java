package com.umc.devine.domain.notice.repository;

import com.umc.devine.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 관리자 목록. 비노출/게시예정/게시종료를 모두 포함한다. */
    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 일반 유저에게 노출되는 공지 목록.
     * WHERE 조건은 {@link Notice#isVisibleAt(LocalDateTime)}와 논리적으로 동일해야 한다.
     */
    @Query("""
            SELECT n FROM Notice n
            WHERE n.isExposed = true
              AND (n.displayStartAt IS NULL OR n.displayStartAt <= :now)
              AND (n.displayEndAt IS NULL OR n.displayEndAt >= :now)
            ORDER BY n.createdAt DESC
            """)
    Page<Notice> findVisible(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT n FROM Notice n
            WHERE n.id = :noticeId
              AND n.isExposed = true
              AND (n.displayStartAt IS NULL OR n.displayStartAt <= :now)
              AND (n.displayEndAt IS NULL OR n.displayEndAt >= :now)
            """)
    Optional<Notice> findVisibleById(@Param("noticeId") Long noticeId, @Param("now") LocalDateTime now);
}
