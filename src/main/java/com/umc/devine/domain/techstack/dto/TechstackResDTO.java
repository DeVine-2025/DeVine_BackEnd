package com.umc.devine.domain.techstack.dto;

import com.umc.devine.domain.techstack.enums.TechstackSource;
import lombok.Builder;

import java.util.List;

public class TechstackResDTO {

    @Builder
    public record TechstackItemDTO(
            Long techstackId,
            String name,
            String genre
    ) {}

    @Builder
    public record TechstackCategoryDTO(
            Long techstackId,
            String name,
            List<TechstackItemDTO> list
    ) {}

    @Builder
    public record TechstackListDTO(
            List<TechstackCategoryDTO> techstacks
    ) {}

    @Builder
    public record DevTechstackItemDTO(
            Long techstackId,
            String name,
            String genre,
            TechstackSource source
    ) {}

    @Builder
    public record DevTechstackListDTO(
            List<DevTechstackItemDTO> techstacks
    ) {}
}
