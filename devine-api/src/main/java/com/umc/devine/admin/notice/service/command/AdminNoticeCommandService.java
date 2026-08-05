package com.umc.devine.admin.notice.service.command;

import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;

public interface AdminNoticeCommandService {

    AdminNoticeResDTO.NoticeDTO createNotice(AdminNoticeReqDTO.CreateNoticeReq request);

    AdminNoticeResDTO.NoticeDTO updateNotice(Long noticeId, AdminNoticeReqDTO.UpdateNoticeReq request);

    void deleteNotice(Long noticeId);
}
