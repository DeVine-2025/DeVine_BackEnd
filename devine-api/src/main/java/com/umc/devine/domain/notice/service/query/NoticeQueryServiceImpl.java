package com.umc.devine.domain.notice.service.query;

import com.umc.devine.domain.notice.converter.NoticeConverter;
import com.umc.devine.domain.notice.dto.NoticeResDTO;
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
public class NoticeQueryServiceImpl implements NoticeQueryService {

    private final NoticeRepository noticeRepository;

    @Override
    public PagedResponse<NoticeResDTO.NoticeSummaryDTO> getVisibleNotices(PageRequest pageRequest) {
        Page<Notice> page = noticeRepository.findVisible(LocalDateTime.now(), pageRequest.toPageable());
        List<NoticeResDTO.NoticeSummaryDTO> content = page.getContent().stream()
                .map(NoticeConverter::toSummaryDTO)
                .toList();
        return PagedResponse.of(page, content);
    }

    @Override
    public NoticeResDTO.NoticeDetailDTO getVisibleNotice(Long noticeId) {
        // 게시 중이 아닌 공지는 존재 여부를 노출하지 않도록 404로 처리한다.
        Notice notice = noticeRepository.findVisibleById(noticeId, LocalDateTime.now())
                .orElseThrow(() -> new NoticeException(NoticeErrorReason.NOTICE_NOT_FOUND));
        return NoticeConverter.toDetailDTO(notice);
    }
}
