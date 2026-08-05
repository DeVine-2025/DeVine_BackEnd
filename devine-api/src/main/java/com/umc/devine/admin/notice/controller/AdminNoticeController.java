package com.umc.devine.admin.notice.controller;

import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.admin.notice.exception.code.AdminNoticeSuccessCode;
import com.umc.devine.admin.notice.service.command.AdminNoticeCommandService;
import com.umc.devine.admin.notice.service.query.AdminNoticeQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/notices")
public class AdminNoticeController implements AdminNoticeControllerDocs {

    private final AdminNoticeCommandService adminNoticeCommandService;
    private final AdminNoticeQueryService adminNoticeQueryService;

    @Override
    @PostMapping
    public ApiResponse<AdminNoticeResDTO.NoticeDTO> createNotice(
            @Valid @RequestBody AdminNoticeReqDTO.CreateNoticeReq request
    ) {
        return ApiResponse.onSuccess(
                AdminNoticeSuccessCode.NOTICE_CREATED,
                adminNoticeCommandService.createNotice(request)
        );
    }

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<AdminNoticeResDTO.NoticeDTO>> getNotices(
            @ParameterObject @ModelAttribute PageRequest pageRequest
    ) {
        return ApiResponse.onSuccess(
                AdminNoticeSuccessCode.NOTICE_LIST_FOUND,
                adminNoticeQueryService.getNotices(pageRequest)
        );
    }

    @Override
    @GetMapping("/{noticeId}")
    public ApiResponse<AdminNoticeResDTO.NoticeDTO> getNotice(
            @PathVariable Long noticeId
    ) {
        return ApiResponse.onSuccess(
                AdminNoticeSuccessCode.NOTICE_FOUND,
                adminNoticeQueryService.getNotice(noticeId)
        );
    }

    @Override
    @PatchMapping("/{noticeId}")
    public ApiResponse<AdminNoticeResDTO.NoticeDTO> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody AdminNoticeReqDTO.UpdateNoticeReq request
    ) {
        return ApiResponse.onSuccess(
                AdminNoticeSuccessCode.NOTICE_UPDATED,
                adminNoticeCommandService.updateNotice(noticeId, request)
        );
    }

    @Override
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(
            @PathVariable Long noticeId
    ) {
        adminNoticeCommandService.deleteNotice(noticeId);
        return ApiResponse.onSuccess(AdminNoticeSuccessCode.NOTICE_DELETED, null);
    }
}
