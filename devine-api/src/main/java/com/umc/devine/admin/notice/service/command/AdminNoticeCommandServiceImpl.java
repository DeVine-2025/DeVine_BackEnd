package com.umc.devine.admin.notice.service.command;

import com.umc.devine.admin.notice.converter.AdminNoticeConverter;
import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.exception.NoticeException;
import com.umc.devine.domain.notice.exception.code.NoticeErrorReason;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeCommandServiceImpl implements AdminNoticeCommandService {

    private final NoticeRepository noticeRepository;

    @Override
    public AdminNoticeResDTO.NoticeDTO createNotice(AdminNoticeReqDTO.CreateNoticeReq request) {
        // 제목/내용 필수값은 @Valid(@NotBlank)가 이미 검증해 400을 반환한다.
        validateDisplayPeriod(request.displayStartAt(), request.displayEndAt());

        Notice notice = noticeRepository.save(Notice.builder()
                .title(request.title())
                .content(request.content())
                .displayStartAt(request.displayStartAt())
                .displayEndAt(request.displayEndAt())
                .isExposed(request.isExposed() == null || request.isExposed())
                .build());

        return AdminNoticeConverter.toNoticeDTO(notice, LocalDateTime.now());
    }

    @Override
    public AdminNoticeResDTO.NoticeDTO updateNotice(Long noticeId, AdminNoticeReqDTO.UpdateNoticeReq request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorReason.NOTICE_NOT_FOUND));

        // @NotBlank는 null을 통과시키므로, 부분 수정에서 빈 문자열로 지우려는 시도는 여기서 막는다.
        if (isBlank(request.title()) || isBlank(request.content())) {
            throw new NoticeException(NoticeErrorReason.BLANK_UPDATE_FIELD);
        }

        notice.update(request.title(), request.content(),
                request.displayStartAt(), request.displayEndAt(),
                request.clearDisplayPeriod(), request.isExposed());

        // 일부 필드만 바뀌어도 기간이 역전될 수 있으므로 변경이 적용된 최종 상태로 검증한다(위반 시 롤백).
        validateDisplayPeriod(notice.getDisplayStartAt(), notice.getDisplayEndAt());

        return AdminNoticeConverter.toNoticeDTO(notice, LocalDateTime.now());
    }

    @Override
    public void deleteNotice(Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoticeException(NoticeErrorReason.NOTICE_NOT_FOUND);
        }
        noticeRepository.deleteById(noticeId);
    }

    /** 두 일시가 모두 설정된 경우에만 검증한다. DB의 notice_display_period_check 제약과 이중 방어. */
    private void validateDisplayPeriod(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new NoticeException(NoticeErrorReason.INVALID_DISPLAY_PERIOD);
        }
    }

    /** null은 "변경하지 않음"이므로 통과시키고, 빈 문자열/공백만 거른다. */
    private boolean isBlank(String value) {
        return value != null && value.isBlank();
    }
}
