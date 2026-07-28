package com.umc.devine.domain.notice.repository;

import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.enums.NoticeDisplayStatus;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeRepositoryTest extends CoreIntegrationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 28, 12, 0, 0);

    @Autowired
    private NoticeRepository noticeRepository;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
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
    @DisplayName("findVisible")
    class FindVisible {

        @Test
        @DisplayName("게시 기간이 양쪽 모두 null이면 상시 노출된다")
        void includesNoticeWithoutPeriod() {
            // given
            save("상시 공지", null, null, true);

            // when
            Page<Notice> result = noticeRepository.findVisible(NOW, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).extracting(Notice::getTitle).containsExactly("상시 공지");
        }

        @Test
        @DisplayName("게시 기간이 한쪽만 설정되면 그 방향으로만 제한된다")
        void includesNoticeWithHalfOpenPeriod() {
            // given
            save("시작만 지정 - 이미 시작됨", NOW.minusDays(1), null, true);
            save("시작만 지정 - 아직 미시작", NOW.plusDays(1), null, true);
            save("종료만 지정 - 아직 유효", null, NOW.plusDays(1), true);
            save("종료만 지정 - 이미 종료", null, NOW.minusDays(1), true);

            // when
            Page<Notice> result = noticeRepository.findVisible(NOW, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).extracting(Notice::getTitle)
                    .containsExactlyInAnyOrder("시작만 지정 - 이미 시작됨", "종료만 지정 - 아직 유효");
        }

        @Test
        @DisplayName("게시 기간의 시작/종료 경계 시각은 노출에 포함된다")
        void includesBoundaryInstants() {
            // given
            save("시작 정각", NOW, NOW.plusDays(1), true);
            save("종료 정각", NOW.minusDays(1), NOW, true);

            // when
            Page<Notice> result = noticeRepository.findVisible(NOW, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).extracting(Notice::getTitle)
                    .containsExactlyInAnyOrder("시작 정각", "종료 정각");
        }

        @Test
        @DisplayName("게시 기간 전/후의 공지는 제외된다")
        void excludesOutOfPeriodNotices() {
            // given
            save("게시 예정", NOW.plusDays(1), NOW.plusDays(2), true);
            save("게시 종료", NOW.minusDays(2), NOW.minusDays(1), true);
            save("게시 중", NOW.minusDays(1), NOW.plusDays(1), true);

            // when
            Page<Notice> result = noticeRepository.findVisible(NOW, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).extracting(Notice::getTitle).containsExactly("게시 중");
        }

        @Test
        @DisplayName("게시 기간 중이라도 isExposed=false면 제외된다")
        void excludesHiddenNotice() {
            // given
            save("수동 비노출", NOW.minusDays(1), NOW.plusDays(1), false);

            // when
            Page<Notice> result = noticeRepository.findVisible(NOW, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("쿼리 필터 결과가 엔티티의 isVisibleAt 판정과 일치한다")
        void matchesEntityVisibilityJudgement() {
            // given - 노출/비노출 경계를 모두 포함하는 조합
            save("상시", null, null, true);
            save("시작 정각", NOW, NOW.plusDays(1), true);
            save("종료 정각", NOW.minusDays(1), NOW, true);
            save("게시 중", NOW.minusDays(1), NOW.plusDays(1), true);
            save("게시 예정", NOW.plusDays(1), NOW.plusDays(2), true);
            save("게시 종료", NOW.minusDays(2), NOW.minusDays(1), true);
            save("수동 비노출", null, null, false);

            // when
            List<String> byQuery = noticeRepository.findVisible(NOW, PageRequest.of(0, 100))
                    .getContent().stream().map(Notice::getTitle).toList();
            List<String> byEntity = noticeRepository.findAll().stream()
                    .filter(n -> n.isVisibleAt(NOW))
                    .map(Notice::getTitle)
                    .toList();

            // then
            assertThat(byQuery).containsExactlyInAnyOrderElementsOf(byEntity);
            assertThat(byQuery).hasSize(4);
        }

        @Test
        @DisplayName("최신순으로 정렬되고 페이징된다")
        void sortsByCreatedAtDescAndPages() {
            // given
            save("첫 번째", null, null, true);
            save("두 번째", null, null, true);
            save("세 번째", null, null, true);

            // when
            Page<Notice> firstPage = noticeRepository.findVisible(NOW, PageRequest.of(0, 2));

            // then
            assertThat(firstPage.getTotalElements()).isEqualTo(3);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(firstPage.getContent()).hasSize(2);
            // createdAt이 동일 마이크로초로 겹칠 수 있어 제목 순서가 아니라 정렬 자체를 검증한다
            assertThat(firstPage.getContent())
                    .isSortedAccordingTo(Comparator.comparing(Notice::getCreatedAt).reversed());
        }
    }

    @Nested
    @DisplayName("findVisibleById")
    class FindVisibleById {

        @Test
        @DisplayName("게시 중인 공지는 조회된다")
        void findsVisibleNotice() {
            // given
            Notice notice = save("게시 중", NOW.minusDays(1), NOW.plusDays(1), true);

            // when
            Optional<Notice> result = noticeRepository.findVisibleById(notice.getId(), NOW);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("게시 중");
        }

        @Test
        @DisplayName("게시 예정/종료/비노출 공지는 조회되지 않는다")
        void doesNotFindInvisibleNotice() {
            // given
            Notice scheduled = save("게시 예정", NOW.plusDays(1), NOW.plusDays(2), true);
            Notice ended = save("게시 종료", NOW.minusDays(2), NOW.minusDays(1), true);
            Notice hidden = save("수동 비노출", null, null, false);

            // when & then
            assertThat(noticeRepository.findVisibleById(scheduled.getId(), NOW)).isEmpty();
            assertThat(noticeRepository.findVisibleById(ended.getId(), NOW)).isEmpty();
            assertThat(noticeRepository.findVisibleById(hidden.getId(), NOW)).isEmpty();
        }
    }

    @Nested
    @DisplayName("displayStatusAt")
    class DisplayStatusAt {

        @Test
        @DisplayName("노출 여부와 게시 기간에 따라 파생 상태를 계산한다")
        void calculatesDerivedStatus() {
            // given & when & then
            assertThat(save("비노출", NOW.minusDays(1), NOW.plusDays(1), false).displayStatusAt(NOW))
                    .isEqualTo(NoticeDisplayStatus.HIDDEN);
            assertThat(save("예정", NOW.plusDays(1), NOW.plusDays(2), true).displayStatusAt(NOW))
                    .isEqualTo(NoticeDisplayStatus.SCHEDULED);
            assertThat(save("게시중", NOW.minusDays(1), NOW.plusDays(1), true).displayStatusAt(NOW))
                    .isEqualTo(NoticeDisplayStatus.DISPLAYING);
            assertThat(save("상시", null, null, true).displayStatusAt(NOW))
                    .isEqualTo(NoticeDisplayStatus.DISPLAYING);
            assertThat(save("종료", NOW.minusDays(2), NOW.minusDays(1), true).displayStatusAt(NOW))
                    .isEqualTo(NoticeDisplayStatus.ENDED);
        }
    }
}
