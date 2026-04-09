package com.umc.devine.domain.techstack.controller;

import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.exception.code.TechstackSuccessCode;
import com.umc.devine.domain.techstack.service.query.TechstackQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/techstacks")
public class TechstackController implements TechstackControllerDocs {

    private final TechstackQueryService techstackQueryService;

    @Override
    @GetMapping
    public ApiResponse<TechstackResDTO.TechstackListDTO> getTechstacks() {
        TechstackSuccessCode code = TechstackSuccessCode.FOUND;

        return ApiResponse.onSuccess(code, techstackQueryService.findAllTechstacks());
    }
}
