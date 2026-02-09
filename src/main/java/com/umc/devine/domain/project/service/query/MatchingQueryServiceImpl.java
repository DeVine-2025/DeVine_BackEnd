package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.converter.MatchingConverter;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.validator.MatchingValidator;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryServiceImpl implements MatchingQueryService {

    private final MatchingRepository matchingRepository;
    private final MatchingValidator matchingValidator;

    @Override
    public MatchingResDTO.DevelopersRes getDevelopers(Member pm, MatchingType type, Pageable pageable) {
        matchingValidator.validatePmRole(pm);

        Page<Matching> matchingPage = matchingRepository.findByProjectOwnerAndMatchingType(
                pm, type, MatchingStatus.CANCELLED, pageable
        );

        List<MatchingResDTO.DeveloperMatchingInfo> developers = matchingPage.getContent().stream()
                .map(MatchingConverter::toDeveloperMatchingInfo)
                .toList();

        return MatchingResDTO.DevelopersRes.builder()
                .developers(PagedResponse.of(matchingPage, developers))
                .build();
    }

    @Override
    public MatchingResDTO.ProjectsRes getProjects(Member developer, MatchingType type, Pageable pageable) {
        matchingValidator.validateDeveloperRole(developer);

        Page<Matching> matchingPage = matchingRepository.findByMemberAndMatchingType(
                developer, type, MatchingStatus.CANCELLED, pageable
        );

        List<MatchingResDTO.ProjectMatchingInfo> projects = matchingPage.getContent().stream()
                .map(MatchingConverter::toProjectMatchingInfo)
                .toList();

        return MatchingResDTO.ProjectsRes.builder()
                .projects(PagedResponse.of(matchingPage, projects))
                .build();
    }
}
