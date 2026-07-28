package com.umc.devine.admin.notice.service.command;

import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.enums.NoticeDisplayStatus;
import com.umc.devine.domain.notice.exception.NoticeException;
import com.umc.devine.domain.notice.exception.code.NoticeErrorReason;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminNoticeCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminNoticeCommandService adminNoticeCommandService;

    @Autowired
    private NoticeRepository noticeRepository;

    private LocalDateTime base;

    @BeforeEach
    void setUp() {
        noticeRepository.deleteAll();
        base = LocalDateTime.now();
    }

    private Notice saveNotice(LocalDateTime start, LocalDateTime end, boolean exposed) {
        return noticeRepository.save(Notice.builder()
                .title("원본 제목")
                .content("원본 내용")
                .displayStartAt(start)
                .displayEndAt(end)
                .isExposed(exposed)
                .build());
    }

    @Nested
    @DisplayName("createNotice")
    class CreateNotice {

        @Test
        @DisplayName("게시 기간을 지정해 공지를 등록한다")
        void createsNoticeWithPeriod() {
            // given
            var request = new AdminNoticeReqDTO.CreateNoticeReq(
                    "점검 안내", "7월 30일 점검", base.minusDays(1), base.plusDays(1), true);

            // when
            AdminNoticeResDTO.NoticeDTO result = adminNoticeCommandService.createNotice(request);

            // then
            assertThat(result.noticeId()).isNotNull();
            assertThat(result.title()).isEqualTo("점검 안내");
            assertThat(result.displayStatus()).isEqualTo(NoticeDisplayStatus.DISPLAYING);
            assertThat(noticeRepository.findById(result.noticeId())).isPresent();
        }

        @Test
        @DisplayName("게시 기간을 지정하지 않으면 상시 노출로 등록된다")
        void createsAlwaysVisibleNotice() {
            // given
            var request = new AdminNoticeReqDTO.CreateNoticeReq("상시 공지", "내용", null, null, null);

            // when
            AdminNoticeResDTO.NoticeDTO result = adminNoticeCommandService.createNotice(request);

            // then
            assertThat(result.displayStartAt()).isNull();
            assertThat(result.displayEndAt()).isNull();
            assertThat(result.isExposed()).isTrue();
            assertThat(result.displayStatus()).isEqualTo(NoticeDisplayStatus.DISPLAYING);
        }

        @Test
        @DisplayName("게시 종료가 시작보다 앞서면 INVALID_DISPLAY_PERIOD 예외가 발생한다")
        void throwsWhenPeriodReversed() {
            // given
            var request = new AdminNoticeReqDTO.CreateNoticeReq(
                    "잘못된 기간", "내용", base.plusDays(2), base.plusDays(1), true);

            // when & then
            assertThatThrownBy(() -> adminNoticeCommandService.createNotice(request))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.INVALID_DISPLAY_PERIOD));
        }
    }

    @Nested
    @DisplayName("updateNotice")
    class UpdateNotice {

        @Test
        @DisplayName("null인 필드는 변경하지 않는다")
        void ignoresNullFields() {
            // given
            Notice notice = saveNotice(base.minusDays(1), base.plusDays(1), true);
            var request = new AdminNoticeReqDTO.UpdateNoticeReq(
                    "수정된 제목", null, null, null, false, null);

            // when
            AdminNoticeResDTO.NoticeDTO result = adminNoticeCommandService.updateNotice(notice.getId(), request);

            // then
            assertThat(result.title()).isEqualTo("수정된 제목");
            assertThat(result.content()).isEqualTo("원본 내용");
            assertThat(result.displayStartAt()).isNotNull();
            assertThat(result.isExposed()).isTrue();
        }

        @Test
        @DisplayName("isExposed=false로 수정하면 노출 상태가 HIDDEN이 된다")
        void hidesNotice() {
            // given
            Notice notice = saveNotice(base.minusDays(1), base.plusDays(1), true);
            var request = new AdminNoticeReqDTO.UpdateNoticeReq(null, null, null, null, false, false);

            // when
            AdminNoticeResDTO.NoticeDTO result = adminNoticeCommandService.updateNotice(notice.getId(), request);

            // then
            assertThat(result.isExposed()).isFalse();
            assertThat(result.displayStatus()).isEqualTo(NoticeDisplayStatus.HIDDEN);
        }

        @Test
        @DisplayName("clearDisplayPeriod=true면 게시 기간이 모두 제거된다")
        void clearsDisplayPeriod() {
            // given
            Notice notice = saveNotice(base.minusDays(1), base.plusDays(1), true);
            var request = new AdminNoticeReqDTO.UpdateNoticeReq(null, null, null, null, true, null);

            // when
            AdminNoticeResDTO.NoticeDTO result = adminNoticeCommandService.updateNotice(notice.getId(), request);

            // then
            assertThat(result.displayStartAt()).isNull();
            assertThat(result.displayEndAt()).isNull();
            assertThat(result.displayStatus()).isEqualTo(NoticeDisplayStatus.DISPLAYING);
        }

        @Test
        @DisplayName("한쪽 일시만 수정해 기간이 역전되면 INVALID_DISPLAY_PERIOD 예외가 발생한다")
        void throwsWhenUpdateResultsInReversedPeriod() {
            // given - 기존 종료일시(base+1일)보다 뒤인 시작일시로만 수정
            Notice notice = saveNotice(base.minusDays(1), base.plusDays(1), true);
            var request = new AdminNoticeReqDTO.UpdateNoticeReq(
                    null, null, base.plusDays(5), null, false, null);

            // when & then
            assertThatThrownBy(() -> adminNoticeCommandService.updateNotice(notice.getId(), request))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.INVALID_DISPLAY_PERIOD));
        }

        @Test
        @DisplayName("제목이나 내용을 빈 문자열로 바꾸려 하면 BLANK_UPDATE_FIELD 예외가 발생한다")
        void throwsWhenBlankTitleOrContent() {
            // given
            Notice notice = saveNotice(null, null, true);
            var blankTitle = new AdminNoticeReqDTO.UpdateNoticeReq("  ", null, null, null, false, null);
            var blankContent = new AdminNoticeReqDTO.UpdateNoticeReq(null, "", null, null, false, null);

            // when & then
            assertThatThrownBy(() -> adminNoticeCommandService.updateNotice(notice.getId(), blankTitle))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.BLANK_UPDATE_FIELD));
            assertThatThrownBy(() -> adminNoticeCommandService.updateNotice(notice.getId(), blankContent))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.BLANK_UPDATE_FIELD));
        }

        @Test
        @DisplayName("존재하지 않는 공지를 수정하면 NOTICE_NOT_FOUND 예외가 발생한다")
        void throwsWhenNotFound() {
            // given
            var request = new AdminNoticeReqDTO.UpdateNoticeReq("제목", null, null, null, false, null);

            // when & then
            assertThatThrownBy(() -> adminNoticeCommandService.updateNotice(999_999L, request))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.NOTICE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteNotice")
    class DeleteNotice {

        @Test
        @DisplayName("공지를 삭제하면 레코드가 사라진다")
        void deletesNotice() {
            // given
            Notice notice = saveNotice(null, null, true);

            // when
            adminNoticeCommandService.deleteNotice(notice.getId());

            // then
            assertThat(noticeRepository.findById(notice.getId())).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 공지를 삭제하면 NOTICE_NOT_FOUND 예외가 발생한다")
        void throwsWhenNotFound() {
            // when & then
            assertThatThrownBy(() -> adminNoticeCommandService.deleteNotice(999_999L))
                    .isInstanceOf(NoticeException.class)
                    .satisfies(e -> assertThat(((NoticeException) e).getReason())
                            .isEqualTo(NoticeErrorReason.NOTICE_NOT_FOUND));
        }
    }
}
