package com.umc.devine.domain.notice.entity;

import com.umc.devine.domain.notice.enums.NoticeDisplayStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 게시 시작 일시. null이면 시작 제한이 없다. */
    @Column(name = "display_start_at")
    private LocalDateTime displayStartAt;

    /** 게시 종료 일시. null이면 종료 제한이 없다. */
    @Column(name = "display_end_at")
    private LocalDateTime displayEndAt;

    /** 게시 기간과 독립된 수동 노출 스위치. false면 기간과 무관하게 노출되지 않는다. */
    @Column(name = "is_exposed", nullable = false)
    private Boolean isExposed;

    /**
     * 해당 시각에 일반 유저에게 노출되는지 여부. 게시 기간이 null인 방향으로는 제한이 없고, 경계 시각은 포함한다.
     * <p>
     * 이 판정은 {@code NoticeRepository.findVisible}의 WHERE 절과 논리적으로 동일해야 한다.
     * 목록은 페이징 때문에 DB에서 필터링하고, 단건/표시용 판정은 여기서 하기 때문이다.
     */
    public boolean isVisibleAt(LocalDateTime now) {
        if (!isExposed) return false;
        if (displayStartAt != null && now.isBefore(displayStartAt)) return false;
        return displayEndAt == null || !now.isAfter(displayEndAt);
    }

    /** 관리자 화면 표시용 파생 상태. 저장하지 않고 조회 시점에 계산한다. */
    public NoticeDisplayStatus displayStatusAt(LocalDateTime now) {
        if (!isExposed) return NoticeDisplayStatus.HIDDEN;
        if (displayStartAt != null && now.isBefore(displayStartAt)) return NoticeDisplayStatus.SCHEDULED;
        if (displayEndAt != null && now.isAfter(displayEndAt)) return NoticeDisplayStatus.ENDED;
        return NoticeDisplayStatus.DISPLAYING;
    }

    /**
     * 부분 수정. null인 필드는 변경하지 않는다.
     * 게시 기간을 없애는 것과 "변경하지 않음"을 구분할 수 없으므로, 기간 제거는 {@code clearDisplayPeriod}로 명시한다.
     */
    public void update(String title, String content,
                       LocalDateTime displayStartAt, LocalDateTime displayEndAt,
                       boolean clearDisplayPeriod, Boolean isExposed) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (clearDisplayPeriod) {
            this.displayStartAt = null;
            this.displayEndAt = null;
        } else {
            if (displayStartAt != null) this.displayStartAt = displayStartAt;
            if (displayEndAt != null) this.displayEndAt = displayEndAt;
        }
        if (isExposed != null) this.isExposed = isExposed;
    }
}
