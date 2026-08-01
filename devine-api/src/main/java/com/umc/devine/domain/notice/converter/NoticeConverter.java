package com.umc.devine.domain.notice.converter;

import com.umc.devine.domain.notice.dto.NoticeResDTO;
import com.umc.devine.domain.notice.entity.Notice;

public class NoticeConverter {

    public static NoticeResDTO.NoticeSummaryDTO toSummaryDTO(Notice notice) {
        return new NoticeResDTO.NoticeSummaryDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getCreatedAt()
        );
    }

    public static NoticeResDTO.NoticeDetailDTO toDetailDTO(Notice notice) {
        return new NoticeResDTO.NoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }
}
