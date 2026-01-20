package com.umc.devine.domain.techstack.controller;

import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Techstack", description = "기술스택 관련 API")
public interface TechstackControllerDocs {

    @Operation(summary = "기본 기술스택 조회 API", description = "등록된 모든 기술스택 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<TechstackResDTO.TechstackListDTO> getTechstacks();
}
