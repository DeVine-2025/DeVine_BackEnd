package com.umc.devine.domain.notice.controller;

import com.umc.devine.domain.notice.dto.NoticeResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notice", description = "공지사항 조회 API (비회원 허용)")
public interface NoticeControllerDocs {

    @Operation(summary = "공지사항 목록 조회 API",
            description = "현재 게시 중인 공지사항을 최신순으로 페이징 조회합니다. 비노출 처리됐거나 게시 기간이 아닌 공지는 포함되지 않습니다. 본문은 상세 조회에서 제공됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<PagedResponse<NoticeResDTO.NoticeSummaryDTO>> getNotices(PageRequest pageRequest);

    @Operation(summary = "공지사항 상세 조회 API",
            description = "게시 중인 공지사항의 본문을 조회합니다. 비노출이거나 게시 기간이 아닌 공지는 404를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없거나 게시 중이 아님")
    })
    ApiResponse<NoticeResDTO.NoticeDetailDTO> getNotice(
            @Parameter(description = "공지 ID") Long noticeId
    );
}
