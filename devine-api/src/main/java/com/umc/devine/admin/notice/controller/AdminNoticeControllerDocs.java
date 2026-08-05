package com.umc.devine.admin.notice.controller;

import com.umc.devine.admin.notice.dto.AdminNoticeReqDTO;
import com.umc.devine.admin.notice.dto.AdminNoticeResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PageRequest;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * TODO: 관리자 인증/인가 기능이 추가되면 관리자 권한 검증을 추가.
 */
@Tag(name = "Admin Notice", description = "관리자 공지사항 관리 API (인증/인가 양식 확정 전까지 임시로 인증 없이 사용 가능)")
public interface AdminNoticeControllerDocs {

    @Operation(summary = "공지사항 등록 API",
            description = "제목/내용/게시 기간/노출 여부를 지정해 공지사항을 등록합니다. 게시 기간을 지정하지 않으면 상시 노출됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 공지 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "제목/내용 미입력, 게시 기간 역전")
    })
    ApiResponse<AdminNoticeResDTO.NoticeDTO> createNotice(
            @Valid @RequestBody AdminNoticeReqDTO.CreateNoticeReq request
    );

    @Operation(summary = "공지사항 목록 조회 API",
            description = "비노출/게시 예정/게시 종료 공지를 모두 포함해 최신순으로 페이징 조회합니다. displayStatus로 현재 노출 상태를 확인할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<PagedResponse<AdminNoticeResDTO.NoticeDTO>> getNotices(PageRequest pageRequest);

    @Operation(summary = "공지사항 상세 조회 API", description = "공지 ID로 단건 상세 정보를 조회합니다. 노출 여부와 무관하게 조회됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    ApiResponse<AdminNoticeResDTO.NoticeDTO> getNotice(
            @Parameter(description = "공지 ID") Long noticeId
    );

    @Operation(summary = "공지사항 수정 API",
            description = "제목/내용/게시 기간/노출 여부를 수정합니다. null인 필드는 변경하지 않으며, 게시 기간을 제거하려면 clearDisplayPeriod=true를 보냅니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "게시 기간 역전, 제목/내용을 빈 문자열로 변경 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    ApiResponse<AdminNoticeResDTO.NoticeDTO> updateNotice(
            @Parameter(description = "공지 ID") Long noticeId,
            @Valid @RequestBody AdminNoticeReqDTO.UpdateNoticeReq request
    );

    @Operation(summary = "공지사항 삭제 API",
            description = "공지사항을 영구 삭제합니다(복구 불가). 일시적으로 감추려면 삭제 대신 isExposed=false로 수정하세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음")
    })
    ApiResponse<Void> deleteNotice(
            @Parameter(description = "공지 ID") Long noticeId
    );
}
