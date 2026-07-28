package com.umc.devine.admin.notice.service.query;

import com.umc.devine.admin.notice.converter.AdminNoticeConverter;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;
import com.umc.devine.domain.notice.exception.NoticeException;
import com.umc.devine.domain.notice.exception.code.NoticeErrorReason;
import com.umc.devine.domain.notice.repository.NoticeRepository;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNoticeQueryServiceImpl implements AdminNoticeQueryService {

    private final NoticeRepository noticeRepository;

    @Override
    public PagedResponse<AdminNoticeResDTO.NoticeDTO> getNotices(PageRequest pageRequest) {
        // 응답 안에서 displayStatus 판정 기준 시각이 흔들리지 않도록 now를 한 번만 구한다.
        LocalDateTime now = LocalDateTime.now();
        Page<Notice> page = noticeRepository.findAllByOrderByCreatedAtDesc(pageRequest.toPageable());
        List<AdminNoticeResDTO.NoticeDTO> content = page.getContent().stream()
                .map(notice -> AdminNoticeConverter.toNoticeDTO(notice, now))
                .toList();
        return PagedResponse.of(page, content);
    }

    @Override
    public AdminNoticeResDTO.NoticeDTO getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorReason.NOTICE_NOT_FOUND));
        return AdminNoticeConverter.toNoticeDTO(notice, LocalDateTime.now());
    }
}
