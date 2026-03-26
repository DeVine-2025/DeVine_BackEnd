package com.umc.devine.domain.techstack.service.query;

import com.umc.devine.domain.techstack.converter.TechstackConverter;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechstackQueryServiceImpl implements TechstackQueryService {

    private final TechstackRepository techstackRepository;

    @Override
    public TechstackResDTO.TechstackListDTO findAllTechstacks() {
        // 한 번의 쿼리로 전체 조회 (parentStack Fetch Join)
        List<Techstack> allTechstacks = techstackRepository.findAllWithParent();

        // 카테고리 (parent가 null인 것)
        List<Techstack> categories = allTechstacks.stream()
                .filter(t -> t.getParentStack() == null)
                .toList();

        // 하위 기술스택을 parent ID 기준으로 그룹핑
        Map<Long, List<Techstack>> childrenByParentId = allTechstacks.stream()
                .filter(t -> t.getParentStack() != null)
                .collect(Collectors.groupingBy(t -> t.getParentStack().getId()));

        // 카테고리별로 DTO 변환
        List<TechstackResDTO.TechstackCategoryDTO> categoryDTOs = categories.stream()
                .map(category -> {
                    List<Techstack> children = childrenByParentId.getOrDefault(category.getId(), List.of());
                    return TechstackConverter.toTechstackCategoryDTO(category, children);
                })
                .toList();

        return TechstackConverter.toTechstackListDTO(categoryDTOs);
    }
}
