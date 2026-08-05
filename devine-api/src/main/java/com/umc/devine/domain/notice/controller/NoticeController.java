package com.umc.devine.domain.notice.controller;

import com.umc.devine.domain.notice.dto.NoticeResDTO;
import com.umc.devine.domain.notice.exception.code.NoticeSuccessCode;
import com.umc.devine.domain.notice.service.query.NoticeQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
public class NoticeController implements NoticeControllerDocs {

    private final NoticeQueryService noticeQueryService;

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<NoticeResDTO.NoticeSummaryDTO>> getNotices(
            @ParameterObject @ModelAttribute PageRequest pageRequest
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICE_LIST_FOUND,
                noticeQueryService.getVisibleNotices(pageRequest)
        );
    }

    @Override
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResDTO.NoticeDetailDTO> getNotice(
            @PathVariable Long noticeId
    ) {
        return ApiResponse.onSuccess(
                NoticeSuccessCode.NOTICE_FOUND,
                noticeQueryService.getVisibleNotice(noticeId)
        );
    }
}
