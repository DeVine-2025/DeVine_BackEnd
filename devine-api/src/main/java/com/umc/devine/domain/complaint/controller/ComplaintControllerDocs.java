package com.umc.devine.domain.complaint.controller;

import com.umc.devine.domain.complaint.dto.ComplaintReqDTO;
import com.umc.devine.domain.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Complaint", description = "신고 관련 API")
public interface ComplaintControllerDocs {

    @Operation(summary = "신고 접수 API",
            description = """
                    프로젝트·회원·채팅 메시지를 신고합니다. 로그인한 회원만 접수할 수 있습니다.

                    - `targetType = PROJECT`: `targetId`에 프로젝트 ID
                    - `targetType = DEVELOPER`: `targetNickname`에 대상 회원 닉네임
                    - `targetType = CHAT`: `targetId`에 채팅 메시지 ID (상대가 보낸 메시지만 신고할 수 있습니다)

                    피신고자는 서버가 대상에서 유도합니다. 사유가 `ETC`면 상세 내용이 필수입니다.
                    프로젝트는 접수 즉시 가려지고 PM에게 알림이 갑니다. 채팅 메시지는 신고자 화면에서만 가려집니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created, 신고가 접수됐어요."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "사유가 기타일 때는 상세 내용을 입력해 주세요. / 본인 항목은 신고할 수 없어요."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이미 삭제되어 신고할 수 없어요."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 신고한 항목이에요. / 이미 신고한 회원이에요. / 오늘은 더 신고할 수 없어요.")
    })
    ApiResponse<ComplaintResDTO.CreateRes> createComplaint(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody ComplaintReqDTO.CreateReq request
    );
}
