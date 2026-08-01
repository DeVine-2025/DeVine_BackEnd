package com.umc.devine.admin.notice.service.query;

import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;

public interface AdminNoticeQueryService {

    PagedResponse<AdminNoticeResDTO.NoticeDTO> getNotices(PageRequest pageRequest);

    AdminNoticeResDTO.NoticeDTO getNotice(Long noticeId);
}
