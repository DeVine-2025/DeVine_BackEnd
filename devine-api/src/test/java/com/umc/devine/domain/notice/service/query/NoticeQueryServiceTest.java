package com.umc.devine.domain.notice.service.query;

import com.umc.devine.domain.notice.dto.NoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.exception.NoticeException;
import com.umc.devine.domain.notice.exception.code.NoticeErrorReason;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private NoticeQueryService noticeQueryService;

    @Autowired
    private NoticeRepository noticeRepository;

    private LocalDateTime base;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        base = LocalDateTime.now();
    }

    private Notice save(String title, LocalDateTime start, LocalDateTime end, boolean exposed) {
        return noticeRepository.save(Notice.builder()
                .title(title)
                .content(title + " 본문")
                .displayStartAt(start)
                .displayEndAt(end)
                .isExposed(exposed)
                .build());
    }

    @Nested
    @DisplayName("getVisibleNotices")
    class GetVisibleNotices {

        @Test
        @DisplayName("게시 중인 공지만 목록에 포함된다")
        void returnsOnlyVisibleNotices() {
            // given
            save("게시 중", base.minusDays(1), base.plusDays(1), true);
            save("상시", null, null, true);
            save("게시 예정", base.plusDays(1), base.plusDays(2), true);
            save("게시 종료", base.minusDays(2), base.minusDays(1), true);
            save("수동 비노출", null, null, false);

            // when
            PagedResponse<NoticeResDTO.NoticeSummaryDTO> result =
                    noticeQueryService.getVisibleNotices(PageRequest.of(1, 10));

            // then
            assertThat(result.getContent()).extracting(NoticeResDTO.NoticeSummaryDTO::title)
                    .containsExactlyInAnyOrder("게시 중", "상시");
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("게시 중인 공지가 없어도 에러가 아니라 빈 목록을 반환한다")
        void returnsEmptyListWhenNothingVisible() {
            // given
            save("수동 비노출", null, null, false);

            // when
            PagedResponse<NoticeResDTO.NoticeSummaryDTO> result =
                    noticeQueryService.getVisibleNotices(PageRequest.of(1, 10));

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getVisibleNotice")
    class GetVisibleNotice {

        @Test
        @DisplayName("게시 중인 공지의 본문을 조회한다")
        void returnsDetailOfVisibleNotice() {
            // given
            Notice notice = save("게시 중", base.minusDays(1), base.plusDays(1), true);

            // when
            NoticeResDTO.NoticeDetailDTO result = noticeQueryService.getVisibleNotice(notice.getId());

            // then
            assertThat(result.noticeId()).isEqualTo(notice.getId());
            assertThat(result.content()).isEqualTo("게시 중 본문");
        }

        @Test
        @DisplayName("게시 예정/종료/비노출 공지는 존재해도 404로 처리된다")
        void throwsNotFoundForInvisibleNotice() {
            // given
            Notice scheduled = save("게시 예정", base.plusDays(1), base.plusDays(2), true);
            Notice ended = save("게시 종료", base.minusDays(2), base.minusDays(1), true);
            Notice hidden = save("수동 비노출", null, null, false);

            // when & then
            for (Long id : new Long[]{scheduled.getId(), ended.getId(), hidden.getId()}) {
                assertThatThrownBy(() -> noticeQueryService.getVisibleNotice(id))
                        .isInstanceOf(NoticeException.class)
                        .satisfies(e -> assertThat(((NoticeException) e).getReason())
                                .isEqualTo(NoticeErrorReason.NOTICE_NOT_FOUND));
            }
        }
    }
}
