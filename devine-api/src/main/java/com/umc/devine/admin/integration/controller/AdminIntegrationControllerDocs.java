package com.umc.devine.admin.integration.controller;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Integration", description = "관리자 외부 연동 상태 조회 API (인증/인가 양식 확정 전까지 임시로 인증 없이 사용 가능)")
public interface AdminIntegrationControllerDocs {

    @Operation(summary = "외부 연동 상태 조회 API",
            description = """
                    스케줄러가 주기적으로 갱신해 둔 최신 상태를 반환합니다. 외부 호출을 하지 않아 즉시 응답합니다.
                    화면 진입 시 사용하세요.

                    상태값: NORMAL(정상) / DELAYED(지연) / DOWN(장애) / UNKNOWN(확인 불가).
                    UNKNOWN은 타임아웃이나 설정값 누락으로 점검 자체를 수행하지 못한 경우입니다.

                    아직 한 번도 점검하지 않았다면 integrations는 빈 배열이고 checkedAt은 null입니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<AdminIntegrationResDTO.HealthSummaryDTO> getIntegrationHealth();

    @Operation(summary = "외부 연동 상태 재점검 API",
            description = """
                    등록된 모든 외부 연동을 즉시 병렬 점검하고 결과를 저장한 뒤 반환합니다.
                    관리자 화면의 새로고침 버튼에 연결하세요.

                    실제 외부 API를 호출하므로 프로브 타임아웃(기본 3초)만큼 응답이 지연될 수 있습니다.
                    남용 방지를 위해 분당 6회로 제한됩니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 재점검 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "재점검 요청 횟수 초과")
    })
    ApiResponse<AdminIntegrationResDTO.HealthSummaryDTO> refreshIntegrationHealth();
}
