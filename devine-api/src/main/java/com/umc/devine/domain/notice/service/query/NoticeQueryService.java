package com.umc.devine.domain.notice.service.query;

import com.umc.devine.domain.notice.dto.NoticeResDTO;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;

public interface NoticeQueryService {

    PagedResponse<NoticeResDTO.NoticeSummaryDTO> getVisibleNotices(PageRequest pageRequest);

    NoticeResDTO.NoticeDetailDTO getVisibleNotice(Long noticeId);
}
