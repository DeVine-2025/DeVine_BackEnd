package com.umc.devine.domain.techstack.service.query;

import com.umc.devine.domain.techstack.dto.TechstackResDTO;

public interface TechstackQueryService {
    TechstackResDTO.TechstackListDTO findAllTechstacks();
}
