package com.umc.devine.admin.notice.converter;

import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;

import java.time.LocalDateTime;

public class AdminNoticeConverter {

    public static AdminNoticeResDTO.NoticeDTO toNoticeDTO(Notice notice, LocalDateTime now) {
        return new AdminNoticeResDTO.NoticeDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getDisplayStartAt(),
                notice.getDisplayEndAt(),
                notice.getIsExposed(),
                notice.displayStatusAt(now),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
