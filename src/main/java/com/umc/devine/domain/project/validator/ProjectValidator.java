package com.umc.devine.domain.project.validator;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.exception.code.ProjectErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ProjectValidator {

    // 모집 마감일은 오늘 이후여야 함
    public void validateRecruitmentDeadline(LocalDate recruitmentDeadline) {
        LocalDate today = LocalDate.now();

        if (recruitmentDeadline.isBefore(today)) {
            throw new GeneralException(ProjectErrorCode.INVALID_RECRUITMENT_DEADLINE);
        }
    }

    // 프로젝트를 수정/삭제하려는 사람이 프로젝트 생성자인지 확인
    public void validateOwner(Project project, Long memberId) {
        if (!project.getMember().getId().equals(memberId)) {
            throw new GeneralException(ProjectErrorCode.FORBIDDEN_PROJECT_ACCESS);
        }
    }

    // 같은 포지션이 중복으로 등록되면 안 됨
    public void validateDuplicatePositions(List<ProjectReqDTO.RecruitmentDTO> recruitments) {
        Set<ProjectPart> positions = new HashSet<>();

        for (ProjectReqDTO.RecruitmentDTO recruitment : recruitments) {
            if (!positions.add(recruitment.position())) {
                throw new GeneralException(ProjectErrorCode.DUPLICATE_PROJECT_PART);
            }
        }
    }

    // 진행 방식에 따른 장소 검증 (ONLINE: "온라인" 입력 권장, OFFLINE/HYBRID: 구체적 장소 필요)
    public void validateLocation(ProjectMode mode, String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new GeneralException(ProjectErrorCode.INVALID_LOCATION);
        }

        // OFFLINE이나 HYBRID인데 "온라인"만 입력한 경우 경고 로그
        if ((mode == ProjectMode.OFFLINE || mode == ProjectMode.HYBRID)
                && location.trim().equals("온라인")) {
            log.warn("OFFLINE/HYBRID 모드인데 장소가 '온라인'으로 입력됨");
        }
    }
}