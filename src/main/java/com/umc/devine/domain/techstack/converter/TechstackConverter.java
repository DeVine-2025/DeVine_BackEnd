package com.umc.devine.domain.techstack.converter;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechstackSource;

import java.util.List;
import java.util.stream.Collectors;

public class TechstackConverter {

    public static TechstackResDTO.TechstackItemDTO toTechstackItemDTO(Techstack techstack) {
        return TechstackResDTO.TechstackItemDTO.builder()
                .techstackId(techstack.getId())
                .name(techstack.getName().name())
                .genre(techstack.getGenre() != null ? techstack.getGenre().name() : null)
                .build();
    }

    public static TechstackResDTO.TechstackCategoryDTO toTechstackCategoryDTO(
            Techstack category,
            List<Techstack> children
    ) {
        List<TechstackResDTO.TechstackItemDTO> itemDTOs = children.stream()
                .map(TechstackConverter::toTechstackItemDTO)
                .collect(Collectors.toList());

        return TechstackResDTO.TechstackCategoryDTO.builder()
                .techstackId(category.getId())
                .name(category.getName().name())
                .list(itemDTOs)
                .build();
    }

    public static TechstackResDTO.TechstackListDTO toTechstackListDTO(
            List<TechstackResDTO.TechstackCategoryDTO> categories
    ) {
        return TechstackResDTO.TechstackListDTO.builder()
                .techstacks(categories)
                .build();
    }

    public static TechstackResDTO.DevTechstackItemDTO toDevTechstackItemDTO(DevTechstack devTechstack) {
        Techstack techstack = devTechstack.getTechstack();
        return TechstackResDTO.DevTechstackItemDTO.builder()
                .techstackId(techstack.getId())
                .name(techstack.getName().name())
                .genre(techstack.getGenre() != null ? techstack.getGenre().name() : null)
                .source(devTechstack.getSource())
                .build();
    }

    public static TechstackResDTO.DevTechstackListDTO toDevTechstackListDTO(List<DevTechstack> devTechstacks) {
        List<TechstackResDTO.DevTechstackItemDTO> itemDTOs = devTechstacks.stream()
                .map(TechstackConverter::toDevTechstackItemDTO)
                .collect(Collectors.toList());

        return TechstackResDTO.DevTechstackListDTO.builder()
                .techstacks(itemDTOs)
                .build();
    }
}
